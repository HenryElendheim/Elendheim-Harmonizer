package com.elendheim.harmonizer.audio

import java.io.File
import java.io.RandomAccessFile

/**
 * Streams mono 16-bit PCM into a standard .wav file. The 44-byte header is
 * written up front with placeholder sizes and patched with the real totals when
 * the recording is closed, so nothing needs to be held in memory.
 */
class WavFileWriter(
    private val file: File,
    private val sampleRate: Int,
) {
    private val raf = RandomAccessFile(file, "rw")
    private var dataBytes = 0
    private var scratch = ByteArray(0)

    init {
        raf.setLength(0)
        writeHeader(0)
    }

    /** Total audio frames written so far. */
    val frameCount: Int get() = dataBytes / 2

    /** Append [count] float samples (−1..1), converting to 16-bit little-endian. */
    fun write(samples: FloatArray, count: Int) {
        if (scratch.size < count * 2) scratch = ByteArray(count * 2)
        var b = 0
        for (i in 0 until count) {
            val clamped = when {
                samples[i] > 1f -> 1f
                samples[i] < -1f -> -1f
                else -> samples[i]
            }
            val v = (clamped * 32767f).toInt()
            scratch[b++] = (v and 0xFF).toByte()
            scratch[b++] = ((v shr 8) and 0xFF).toByte()
        }
        raf.write(scratch, 0, count * 2)
        dataBytes += count * 2
    }

    /** Patch the header with final sizes and close the file. */
    fun close() {
        try {
            raf.seek(0)
            writeHeader(dataBytes)
        } finally {
            raf.close()
        }
    }

    private fun writeHeader(dataLen: Int) {
        val channels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val header = ByteArray(44)

        fun putString(offset: Int, s: String) {
            for (i in s.indices) header[offset + i] = s[i].code.toByte()
        }
        fun putInt(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = ((value shr 8) and 0xFF).toByte()
            header[offset + 2] = ((value shr 16) and 0xFF).toByte()
            header[offset + 3] = ((value shr 24) and 0xFF).toByte()
        }
        fun putShort(offset: Int, value: Int) {
            header[offset] = (value and 0xFF).toByte()
            header[offset + 1] = ((value shr 8) and 0xFF).toByte()
        }

        putString(0, "RIFF")
        putInt(4, 36 + dataLen)
        putString(8, "WAVE")
        putString(12, "fmt ")
        putInt(16, 16)                 // PCM header size
        putShort(20, 1)                // PCM format
        putShort(22, channels)
        putInt(24, sampleRate)
        putInt(28, byteRate)
        putShort(32, blockAlign)
        putShort(34, bitsPerSample)
        putString(36, "data")
        putInt(40, dataLen)

        raf.write(header)
    }
}
