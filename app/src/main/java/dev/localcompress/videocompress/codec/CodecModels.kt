package dev.localcompress.videocompress.codec

data class CodecMimeCapability(
    val mimeType: String,
    val isVideo: Boolean,
    val bitrateRange: IntRange? = null,
    val widthRange: IntRange? = null,
    val heightRange: IntRange? = null,
    val widthAlignment: Int? = null,
    val heightAlignment: Int? = null,
    val frameRateRange: ClosedFloatingPointRange<Double>? = null,
    val maxSupportedInstances: Int? = null,
    val profileLevels: List<String> = emptyList(),
    val colorFormats: List<String> = emptyList(),
)

data class CodecEntry(
    val name: String,
    val isEncoder: Boolean,
    val isHardwareAccelerated: Boolean,
    val isSoftwareOnly: Boolean,
    val isVendor: Boolean,
    val canonicalName: String?,
    val capabilities: List<CodecMimeCapability>,
)
