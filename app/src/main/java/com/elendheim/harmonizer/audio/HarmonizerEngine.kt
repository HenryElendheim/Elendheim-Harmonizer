package com.elendheim.harmonizer.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Process
import kotlin.concurrent.thread

/**
 * The live loop: read the microphone, stack a perfect fifth on top, play the
 * blend straight back out. Everything runs on one dedicated high-priority audio
 * thread so the round trip stays as tight as the device allows.
 *
 * Headphones are strongly recommended, otherwise the speaker feeds back into the
 * mic. The UI hints at this on first run.
 */
class HarmonizerEngine(
    private val onLevel: (Float) -> Unit,
    private val onNote: (Double?) -> Unit,
) {
    @Volatile
    private var running = false
    private var worker: Thread? = null

    /** Dry/wet blend for the fifth, 0f..1f. The clean voice always stays present. */
    @Volatile
    var fifthLevel: Float = 0.85f

    val isRunning: Boolean get() = running

    fun start() {
        if (running) return
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

        // A short block keeps latency down; we still double it for headroom.
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

        val shifter = PitchShifter(PitchShifter.RATIO_FIFTH)
        val detector = PitchDetector(sampleRate)

        val input = FloatArray(block)
        val output = FloatArray(block)

        var levelSmoothed = 0f
        var framesSinceNote = 0

        try {
            recorder.startRecording()
            player.play()

            while (running) {
                val read = recorder.read(input, 0, block, AudioRecord.READ_BLOCKING)
                if (read <= 0) continue

                var peak = 0f
                for (i in 0 until read) {
                    val dry = input[i]
                    val wet = shifter.process(dry)
                    // Keep the clean voice, add the fifth underneath, guard the ceiling.
                    var mixed = dry + wet * fifthLevel
                    if (mixed > 1f) mixed = 1f
                    if (mixed < -1f) mixed = -1f
                    output[i] = mixed

                    val a = if (dry < 0) -dry else dry
                    if (a > peak) peak = a
                }

                player.write(output, 0, read, AudioTrack.WRITE_BLOCKING)

                // Smooth the level for a calm meter, and name the note now and then.
                levelSmoothed += (peak - levelSmoothed) * 0.3f
                onLevel(levelSmoothed)

                framesSinceNote += read
                if (framesSinceNote >= sampleRate / 12) { // ~12 times a second
                    framesSinceNote = 0
                    onNote(detector.detect(input, read))
                }
            }
        } catch (_: Exception) {
            // If the audio path is yanked out from under us, just stop cleanly.
        } finally {
            runCatching { recorder.stop() }
            runCatching { player.stop() }
            recorder.release()
            player.release()
            running = false
        }
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
