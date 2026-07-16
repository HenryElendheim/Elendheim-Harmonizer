package com.elendheim.harmonizer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Process
import com.elendheim.harmonizer.ScaleType
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.pow
import kotlin.math.sin

/** Frequency multiplier for a shift of [semitones] (12 to the octave). */
fun semitonesToRatio(semitones: Int): Float = 2.0.pow(semitones / 12.0).toFloat()

/**
 * The live loop: read the microphone, stack one or two harmony voices on top,
 * play the blend straight back out. Everything runs on one dedicated
 * high-priority audio thread so the round trip stays as tight as the device
 * allows.
 *
 * Two voices let you hold a high harmony and a low one at once (say a fifth up
 * and an octave down). Each has its own interval, in semitones, and its own
 * level. The clean voice is always kept in the mix. Headphones are strongly
 * recommended, otherwise the speaker feeds back into the mic.
 */
class HarmonizerEngine(
    private val onLevel: (Float) -> Unit,
    private val onNote: (Double?) -> Unit,
    private val onRecordingSaved: (File) -> Unit = {},
) {
    @Volatile
    private var running = false
    private var worker: Thread? = null
    private var pendingRecordFile: File? = null

    // Two harmony voices plus an autotune corrector for the lead.
    private val voiceAShifter = PitchShifter(semitonesToRatio(7))
    private val voiceBShifter = PitchShifter(semitonesToRatio(-12))
    private val autoShifter = PitchShifter(1f)

    @Volatile
    var voiceAEnabled: Boolean = true

    @Volatile
    var voiceALevel: Float = 0.85f

    var voiceASemitones: Int = 7
        set(value) {
            field = value
            voiceAShifter.ratio = semitonesToRatio(value)
        }

    @Volatile
    var voiceBEnabled: Boolean = false

    @Volatile
    var voiceBLevel: Float = 0.7f

    var voiceBSemitones: Int = -12
        set(value) {
            field = value
            voiceBShifter.ratio = semitonesToRatio(value)
        }

    // Autotune.
    @Volatile
    var autotuneEnabled: Boolean = false

    @Volatile
    var autotuneKey: Int = 0

    @Volatile
    var autotuneScale: ScaleType = ScaleType.MAJOR

    @Volatile
    var retuneSpeed: Float = 0.6f

    @Volatile
    var humanizer: Float = 0.3f

    val isRunning: Boolean get() = running

    /**
     * Begin monitoring. If [recordTo] is given, the harmonized output is written
     * to that .wav file for the length of the session.
     */
    fun start(recordTo: File? = null) {
        if (running) return
        pendingRecordFile = recordTo
        running = true
        worker = thread(name = "harmonizer-audio", priority = Thread.MAX_PRIORITY) {
            runLoop()
        }
    }

    fun stop() {
        running = false
        worker?.join(500)
        worker = null
        onLevel(0f)
        onNote(null)
    }

    private fun runLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        val sampleRate = pickSampleRate()

        val inMin = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        val outMin = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        if (inMin <= 0 || outMin <= 0) {
            running = false
            return
        }

        val block = 1024
        val recordBufferBytes = maxOf(inMin, block * 4 * 2)
        val trackBufferBytes = maxOf(outMin, block * 4 * 2)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
            recordBufferBytes
        )
        val player = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(trackBufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()

        if (recorder.state != AudioRecord.STATE_INITIALIZED ||
            player.state != AudioTrack.STATE_INITIALIZED
        ) {
            recorder.release()
            player.release()
            running = false
            return
        }

        val detector = PitchDetector(sampleRate)
        voiceAShifter.reset()
        voiceBShifter.reset()
        autoShifter.reset()

        val recordFile = pendingRecordFile
        val writer = recordFile?.let {
            runCatching { WavFileWriter(it, sampleRate) }.getOrNull()
        }
        val minFrames = sampleRate / 2 // ignore recordings shorter than ~0.5s

        val input = FloatArray(block)
        val output = FloatArray(block)

        var levelSmoothed = 0f
        var framesSinceNote = 0

        // Autotune glide state.
        var currentAutoRatio = 1f
        var desiredAutoRatio = 1f
        var driftPhase = 0f

        try {
            recorder.startRecording()
            player.play()

            while (running) {
                val read = recorder.read(input, 0, block, AudioRecord.READ_BLOCKING)
                if (read <= 0) continue

                val levelA = if (voiceAEnabled) voiceALevel else 0f
                val levelB = if (voiceBEnabled) voiceBLevel else 0f
                val autoOn = autotuneEnabled

                var peak = 0f
                for (i in 0 until read) {
                    val dry = input[i]

                    // The lead is the (optionally) autotuned voice; harmonies
                    // stack on top of it so they stay in tune with the lead.
                    val lead = if (autoOn) autoShifter.process(dry) else dry

                    // Always run the harmony shifters so their buffers stay warm;
                    // a muted voice just contributes zero.
                    val wetA = voiceAShifter.process(lead) * levelA
                    val wetB = voiceBShifter.process(lead) * levelB

                    var mixed = lead + wetA + wetB
                    if (mixed > 1f) mixed = 1f
                    if (mixed < -1f) mixed = -1f
                    output[i] = mixed

                    val a = if (dry < 0) -dry else dry
                    if (a > peak) peak = a
                }

                player.write(output, 0, read, AudioTrack.WRITE_BLOCKING)
                writer?.let { runCatching { it.write(output, read) } }

                levelSmoothed += (peak - levelSmoothed) * 0.3f
                onLevel(levelSmoothed)

                // Track pitch: often enough for autotune when it's on, and just
                // enough to name the note on screen when it isn't.
                framesSinceNote += read
                val interval = if (autoOn) 2048 else sampleRate / 12
                if (framesSinceNote >= interval) {
                    framesSinceNote = 0
                    val f0 = detector.detect(input, read)
                    onNote(f0)
                    if (autoOn && f0 != null && f0 > 0.0) {
                        desiredAutoRatio = autoRatioFor(f0)
                    }
                }

                // Glide toward the target correction, or ease back to no shift.
                if (autoOn) {
                    val speed = retuneSpeed.coerceIn(0f, 1f)
                    val alpha = 0.03f + speed * speed * 0.9f
                    currentAutoRatio += (desiredAutoRatio - currentAutoRatio) * alpha
                    // Humanizer adds a gentle, slow drift so it never locks dead-on.
                    driftPhase += 0.35f
                    val drift = 1f + sin(driftPhase) * humanizer.coerceIn(0f, 1f) * 0.012f
                    autoShifter.ratio = currentAutoRatio * drift
                } else if (currentAutoRatio != 1f) {
                    currentAutoRatio = 1f
                    autoShifter.ratio = 1f
                }
            }
        } catch (_: Exception) {
            // If the audio path is yanked out from under us, just stop cleanly.
        } finally {
            runCatching { recorder.stop() }
            runCatching { player.stop() }
            recorder.release()
            player.release()

            if (writer != null && recordFile != null) {
                val frames = writer.frameCount
                runCatching { writer.close() }
                if (frames >= minFrames) {
                    onRecordingSaved(recordFile)
                } else {
                    // Too short to be worth keeping.
                    runCatching { recordFile.delete() }
                }
            }

            pendingRecordFile = null
            running = false
        }
    }

    /**
     * The pitch-correction ratio for a detected frequency: snap to the nearest
     * in-key note, pulled fully or partway depending on the humanizer.
     */
    private fun autoRatioFor(freq: Double): Float {
        val target = Autotune.nearestFrequency(freq, autotuneKey, autotuneScale)
        val full = target / freq
        val strength = (1.0 - humanizer.coerceIn(0f, 1f) * 0.6).coerceIn(0.2, 1.0)
        return full.pow(strength).toFloat()
    }

    /** Prefer 48 kHz (native on most phones); fall back to 44.1 kHz. */
    private fun pickSampleRate(): Int {
        for (rate in intArrayOf(48000, 44100)) {
            val min = AudioRecord.getMinBufferSize(
                rate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            )
            if (min > 0) return rate
        }
        return 44100
    }
}
