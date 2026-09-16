package dev.localcompress.videocompress.codec

import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecCapabilities

/**
 * MediaCodecInfo.CodecProfileLevel / CodecCapabilities expose profiles, levels and color formats
 * only as opaque int constants. This reflects over the SDK's own constant fields once and caches
 * value -> human-readable-name lookup tables, so the diagnostics screen can show "Main10 / Level5"
 * instead of "6 / 4096" without us hand-maintaining a table that goes stale every SDK release.
 */
internal object CodecFieldNames {
    private val profileLevelFields: List<Pair<String, Int>> by lazy {
        MediaCodecInfo.CodecProfileLevel::class.java.fields
            .mapNotNull { field ->
                runCatching { field.name to (field.get(null) as? Int ?: return@mapNotNull null) }.getOrNull()
            }
    }

    private val colorFormatMap: Map<Int, String> by lazy {
        CodecCapabilities::class.java.fields
            .filter { it.name.startsWith("COLOR_Format") }
            .mapNotNull { field -> runCatching { (field.get(null) as Int) to field.name }.getOrNull() }
            .toMap()
    }

    /** e.g. "video/avc" -> "AVC", "video/hevc" -> "HEVC", "video/x-vnd.on2.vp9" -> "VP9" */
    private fun mimePrefix(mimeType: String): String? = when (mimeType) {
        "video/avc" -> "AVC"
        "video/hevc" -> "HEVC"
        "video/x-vnd.on2.vp8" -> "VP8"
        "video/x-vnd.on2.vp9" -> "VP9"
        "video/av01" -> "AV1"
        "video/mp4v-es" -> "MPEG4"
        "video/3gpp" -> "H263"
        "video/dolby-vision" -> "DolbyVision"
        "audio/mp4a-latm" -> "AAC"
        else -> null
    }

    fun profileLevelName(mimeType: String, profile: Int, level: Int): String {
        val prefix = mimePrefix(mimeType)
        if (prefix == null) return "profile=$profile level=$level"
        val profileName = profileLevelFields.firstOrNull {
            it.first.startsWith(prefix) && it.first.contains("Profile") && it.second == profile
        }?.first?.removePrefix(prefix)?.removePrefix("Profile") ?: "profile=$profile"
        val levelName = profileLevelFields.firstOrNull {
            it.first.startsWith(prefix) && it.first.contains("Level") && it.second == level
        }?.first?.removePrefix(prefix)?.removeSuffix("Tier")?.replace("Level", " L") ?: "level=$level"
        return "$profileName /$levelName"
    }

    fun colorFormatName(value: Int): String = colorFormatMap[value] ?: "0x${value.toString(16)}"
}
