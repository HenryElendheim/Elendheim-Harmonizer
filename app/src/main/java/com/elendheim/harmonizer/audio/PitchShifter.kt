package com.elendheim.harmonizer.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor

/**
 * A low-latency pitch shifter with a live-adjustable ratio.
 *
 * This is a time-domain granular shifter: incoming audio is written into a small
 * circular buffer, and two read taps sweep through it at a rate that raises (or
 * lowers) the pitch by [ratio]. The taps are offset by half a grain and windowed
 * with overlapping raised-cosine (Hann) envelopes whose sum is constant, so the
 * output stays at a steady level and the seam where a tap wraps is masked while
 * that tap is silent.
 *
 * It runs sample-by-sample with no FFT, which keeps latency low enough to sing
 * through live. On steady tones there's a gentle chorus-like character; on a real
 * voice, natural vibrato and breath smear it into something musical. [ratio] can
 * be changed at any time (e.g. when the interval knob moves); it is read fresh on
 * every sample. For a perfect fifth use [RATIO_FIFTH].
 */
class PitchShifter(
    initialRatio: Float,
    grainSize: Int = DEFAULT_GRAIN
) {
    /** Frequency multiplier. Safe to set from another thread while running. */
    @Volatile
    var ratio: Float = initialRatio

    private val grain = grainSize
    private val size = grainSize * 2
    private val buffer = FloatArray(size)
    private var writeIndex = 0
    private var phase = 0f

    /** Reset internal state so a fresh session starts clean. */
    fun reset() {
        buffer.fill(0f)
        writeIndex = 0
        phase = 0f
    }

    /** Shift a single sample. Call once per input frame, in order. */
    fun process(sample: Float): Float {
        buffer[writeIndex] = sample

        // Phase moves the read taps relative to the write head. A negative
        // increment (ratio > 1) shortens the delay over time, raising pitch.
        val increment = (1f - ratio) / grain

        val p1 = phase
        var p2 = phase + 0.5f
        p2 -= floor(p2)

        val read1 = writeIndex - p1 * grain
        val read2 = writeIndex - p2 * grain

        val w1 = 0.5f - 0.5f * cos(TWO_PI * p1)
        val w2 = 0.5f - 0.5f * cos(TWO_PI * p2)

        val out = w1 * interpolate(read1) + w2 * interpolate(read2)

        writeIndex++
        if (writeIndex >= size) writeIndex = 0

        phase += increment
        phase -= floor(phase)

        return out
    }

    /** Linear interpolation into the circular buffer at a fractional position. */
    private fun interpolate(position: Float): Float {
        val base = floor(position).toInt()
        val frac = position - base
        var a = base % size
        if (a < 0) a += size
        var b = a + 1
        if (b >= size) b -= size
        return buffer[a] * (1f - frac) + buffer[b] * frac
    }

    companion object {
        private const val TWO_PI = (2.0 * PI).toFloat()

        /** ~43 ms grain at 48 kHz: a good balance of latency and smoothness. */
        const val DEFAULT_GRAIN = 2048

        /** A perfect fifth is a 3:2 frequency ratio. */
        const val RATIO_FIFTH = 1.5f
    }
}
