package com.elendheim.harmonizer.audio

import com.elendheim.harmonizer.ScaleType
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow

/**
 * Snaps a detected frequency to the nearest note allowed by a key and scale.
 * Autotune uses this to work out how far to nudge the voice.
 */
object Autotune {
    // Semitone offsets from the key root that belong to each scale.
    private val MAJOR = intArrayOf(0, 2, 4, 5, 7, 9, 11)
    private val MINOR = intArrayOf(0, 2, 3, 5, 7, 8, 10)

    private const val A4 = 440.0
    private val LN2 = ln(2.0)

    /** The nearest in-key frequency to [freq]; returns [freq] unchanged if invalid. */
    fun nearestFrequency(freq: Double, keyRoot: Int, scale: ScaleType): Double {
        if (freq <= 0.0) return freq
        val midi = 69.0 + 12.0 * ln(freq / A4) / LN2

        var bestMidi = midi
        var bestDist = Double.MAX_VALUE
        val lo = floor(midi).toInt() - 1
        val hi = ceil(midi).toInt() + 1
        for (m in lo..hi) {
            if (!isAllowed(m, keyRoot, scale)) continue
            val d = abs(m - midi)
            if (d < bestDist) {
                bestDist = d
                bestMidi = m.toDouble()
            }
        }
        return A4 * 2.0.pow((bestMidi - 69.0) / 12.0)
    }

    private fun isAllowed(midi: Int, keyRoot: Int, scale: ScaleType): Boolean {
        if (scale == ScaleType.CHROMATIC) return true
        val degree = (((midi - keyRoot) % 12) + 12) % 12
        val set = if (scale == ScaleType.MAJOR) MAJOR else MINOR
        return set.contains(degree)
    }
}
