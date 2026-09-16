package dev.localcompress.videocompress.codec

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build

/**
 * Enumerates every codec the device (and OS build) actually exposes via [MediaCodecList], for the
 * "hardware codec availability" diagnostics screen and for populating the codec picker with only
 * encoders that genuinely exist on this phone.
 */
object CodecInfoProvider {

    fun listCodecs(includeAll: Boolean = false): List<CodecEntry> {
        val list = MediaCodecList(if (includeAll) MediaCodecList.ALL_CODECS else MediaCodecList.REGULAR_CODECS)
        return list.codecInfos
            .map(::buildEntry)
            .sortedWith(
                compareBy(
                    { !it.isEncoder },
                    { !it.isHardwareAccelerated },
                    { it.name },
                )
            )
    }

    /** Video encoders on this device that can produce [mimeType], hardware ones first. */
    fun recommendedVideoEncoders(mimeType: String, includeAll: Boolean = false): List<CodecEntry> =
        listCodecs(includeAll).filter { entry ->
            entry.isEncoder && entry.capabilities.any { it.isVideo && it.mimeType.equals(mimeType, ignoreCase = true) }
        }

    fun hasAnyEncoderFor(mimeType: String): Boolean = recommendedVideoEncoders(mimeType, includeAll = true).isNotEmpty()

    private fun buildEntry(info: MediaCodecInfo): CodecEntry {
        val isHardware: Boolean
        val isSoftwareOnly: Boolean
        val isVendor: Boolean
        if (Build.VERSION.SDK_INT >= 29) {
            isHardware = info.isHardwareAccelerated
            isSoftwareOnly = info.isSoftwareOnly
            isVendor = info.isVendor
        } else {
            val lower = info.name.lowercase()
            isSoftwareOnly = lower.startsWith("omx.google.") || lower.startsWith("c2.android.") || lower.startsWith("c2.google.")
            isHardware = !isSoftwareOnly
            isVendor = !lower.startsWith("omx.google.") && !lower.startsWith("c2.android.") && !lower.startsWith("c2.google.") &&
                !lower.startsWith("omx.sec.") // heuristic only, pre-API29 has no reliable signal
        }
        val canonicalName = if (Build.VERSION.SDK_INT >= 29) runCatching { info.canonicalName }.getOrNull() else null

        val caps = info.supportedTypes.mapNotNull { mime ->
            runCatching { buildMimeCapability(info, mime) }.getOrNull()
        }

        return CodecEntry(
            name = info.name,
            isEncoder = info.isEncoder,
            isHardwareAccelerated = isHardware,
            isSoftwareOnly = isSoftwareOnly,
            isVendor = isVendor,
            canonicalName = canonicalName,
            capabilities = caps,
        )
    }

    private fun buildMimeCapability(info: MediaCodecInfo, mime: String): CodecMimeCapability {
        val capabilities = info.getCapabilitiesForType(mime)
        val isVideo = mime.startsWith("video/")
        val videoCaps = if (isVideo) runCatching { capabilities.videoCapabilities }.getOrNull() else null

        val maxInstances = runCatching { capabilities.maxSupportedInstances }.getOrNull()

        val profileLevels = capabilities.profileLevels.orEmpty()
            .map { CodecFieldNames.profileLevelName(mime, it.profile, it.level) }
            .distinct()

        val colorFormats = if (isVideo) {
            capabilities.colorFormats.orEmpty().map { CodecFieldNames.colorFormatName(it) }.distinct()
        } else emptyList()

        return CodecMimeCapability(
            mimeType = mime,
            isVideo = isVideo,
            bitrateRange = videoCaps?.bitrateRange?.let { it.lower..it.upper },
            widthRange = videoCaps?.supportedWidths?.let { it.lower..it.upper },
            heightRange = videoCaps?.supportedHeights?.let { it.lower..it.upper },
            widthAlignment = videoCaps?.widthAlignment,
            heightAlignment = videoCaps?.heightAlignment,
            frameRateRange = videoCaps?.supportedFrameRates?.let { it.lower.toDouble()..it.upper.toDouble() },
            maxSupportedInstances = maxInstances,
            profileLevels = profileLevels,
            colorFormats = colorFormats,
        )
    }
}
