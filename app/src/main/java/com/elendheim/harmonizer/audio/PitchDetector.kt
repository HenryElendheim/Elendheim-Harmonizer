package com.elendheim.harmonizer.audio

import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A small autocorrelation pitch tracker, used only to name the note on screen.
 * It is deliberately cheap and forgiving: if it isn't confident, it reports
 * nothing rather than flickering a wrong guess. The harmony itself does not
 * depend on this; the fifth is a fixed ratio and needs no pitch tracking.
 */
class PitchDetector(private val sampleRate: Int) {

    private val minFreq = 70.0    // roughly the low end of a singing voice
    private val maxFreq = 1000.0

    /** Returns the detected frequency in Hz, or null when the signal is unclear. */
    fun detect(frame: FloatArray, count: Int): Double? {
        var rms = 0.0
        for (i in 0 until count) rms += (frame[i] * frame[i]).toDouble()
        rms = sqrt(rms / count)
        if (rms < 0.01) return null // too quiet to be a note

        val minLag = (sampleRate / maxFreq).toInt().coerceAtLeast(2)
        val maxLag = (sampleRate / minFreq).toInt().coerceAtMost(count - 1)
        if (maxLag <= minLag) return null

        var bestLag = -1
        var bestValue = 0.0
        var normAtBest = 0.0
        for (lag in minLag..maxLag) {
            var sum = 0.0
            var norm = 0.0
            for (i in 0 until count - lag) {
                sum += (frame[i] * frame[i + lag]).toDouble()
                norm += (frame[i + lag] * frame[i + lag]).toDouble()
            }
            val value = if (norm > 0) sum / sqrt(norm) else 0.0
            if (value > bestValue) {
                bestValue = value
                bestLag = lag
                normAtBest = norm
            }
        }

        if (bestLag < 0 || normAtBest <= 0.0) return null

        // Confidence gate: the peak must be a strong match, not incidental.
        val confidence = bestValue / sqrt((count - bestLag).toDouble())
        if (confidence < 0.5) return null

        // Parabolic interpolation around the peak for sub-sample precision.
        val refined = parabolicPeak(frame, count, bestLag)
        return sampleRate / refined
    }

    private fun parabolicPeak(frame: FloatArray, count: Int, lag: Int): Double {
        fun corr(l: Int): Double {
            if (l < 1 || l >= count) return 0.0
            var s = 0.0
            for (i in 0 until count - l) s += (frame[i] * frame[i + l]).toDouble()
            return s
        }
        val y0 = corr(lag - 1)
        val y1 = corr(lag)
        val y2 = corr(lag + 1)
        val denom = (y0 - 2 * y1 + y2)
        if (denom == 0.0) return lag.toDouble()
        val shift = 0.5 * (y0 - y2) / denom
        return lag + shift
    }

    companion object {
        private val NAMES = arrayOf(
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
        )

        /** Human-readable note name (e.g. "A4") for a frequency, or "--" if invalid. */
        fun noteName(frequency: Double?): String {
            if (frequency == null || frequency <= 0.0) return "--"
            val midi = (69 + 12 * ln(frequency / 440.0) / ln(2.0)).roundToInt()
            if (midi < 0 || midi > 127) return "--"
            val name = NAMES[midi % 12]
            val octave = midi / 12 - 1
            return "$name$octave"
        }
    }
}
