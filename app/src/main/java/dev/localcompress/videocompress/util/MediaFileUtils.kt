package dev.localcompress.videocompress.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

data class MediaFileInfo(val displayName: String, val sizeBytes: Long)

object MediaFileUtils {

    fun queryFileInfo(context: Context, uri: Uri): MediaFileInfo {
        var name: String? = null
        var size = 0L
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIdx >= 0) name = cursor.getString(nameIdx)
                        if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
                    }
                }
        }
        return MediaFileInfo(name ?: uri.lastPathSegment ?: "video", size)
    }

    /** Compressed output is always muxed into an .mp4 container, whatever the source container was. */
    fun compressedFileName(originalDisplayName: String): String {
        val base = originalDisplayName.substringBeforeLast('.', originalDisplayName)
        val safe = base.ifBlank { "video" }
        return "${safe}_compressed.mp4"
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 Б"
        val units = arrayOf("Б", "КБ", "МБ", "ГБ")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.lastIndex) {
            value /= 1024
            unitIndex++
        }
        return if (unitIndex == 0) "${value.toInt()} ${units[unitIndex]}" else "%.1f %s".format(value, units[unitIndex])
    }
}
