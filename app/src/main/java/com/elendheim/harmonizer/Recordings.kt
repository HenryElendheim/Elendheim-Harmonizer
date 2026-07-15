package com.elendheim.harmonizer

import android.content.Context
import java.io.File
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One saved recording on disk, with its date and length worked out. */
data class Recording(
    val file: File,
    val recordedAt: Long,
    val durationSeconds: Int,
) {
    val displayDate: String
        get() = DISPLAY_FORMAT.format(Date(recordedAt))

    val displayDuration: String
        get() {
            val m = durationSeconds / 60
            val s = durationSeconds % 60
            return "%d:%02d".format(m, s)
        }

    /** A friendly filename for exports, e.g. "Elendheim Harmonizer 2026-07-15 14.23.wav". */
    val exportName: String
        get() = "Elendheim Harmonizer ${EXPORT_FORMAT.format(Date(recordedAt))}.wav"

    companion object {
        private val DISPLAY_FORMAT = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
        private val EXPORT_FORMAT = SimpleDateFormat("yyyy-MM-dd HH.mm", Locale.US)
    }
}

/** Manages the folder of saved .wav recordings. */
class RecordingStore(context: Context) {
    private val dir = File(context.filesDir, "recordings")
    private val fileStamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

    /** Create a fresh, timestamped .wav file to record into. */
    fun newFile(): File {
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "harmonizer-${fileStamp.format(Date())}.wav")
    }

    /** All saved recordings, newest first. */
    fun list(): List<Recording> {
        val files = dir.listFiles { f -> f.isFile && f.extension.equals("wav", ignoreCase = true) }
            ?: return emptyList()
        return files
            .map { Recording(it, it.lastModified(), durationSeconds(it)) }
            .sortedByDescending { it.recordedAt }
    }

    fun delete(recording: Recording) {
        runCatching { recording.file.delete() }
    }

    /** Read the WAV header to work out the length in whole seconds. */
    private fun durationSeconds(file: File): Int {
        return runCatching {
            RandomAccessFile(file, "r").use { raf ->
                if (raf.length() < 44) return@use 0
                val header = ByteArray(44)
                raf.readFully(header)
                val byteRate = readInt(header, 28)
                val dataLen = readInt(header, 40)
                if (byteRate <= 0) 0 else (dataLen / byteRate)
            }
        }.getOrDefault(0)
    }

    private fun readInt(b: ByteArray, offset: Int): Int =
        (b[offset].toInt() and 0xFF) or
            ((b[offset + 1].toInt() and 0xFF) shl 8) or
            ((b[offset + 2].toInt() and 0xFF) shl 16) or
            ((b[offset + 3].toInt() and 0xFF) shl 24)
}
