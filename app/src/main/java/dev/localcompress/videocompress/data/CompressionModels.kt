package dev.localcompress.videocompress.data

import android.net.Uri
import java.util.UUID

enum class ResolutionPreset(val targetHeight: Int?, val label: String) {
    ORIGINAL(null, "Оригинал"),
    P1080(1080, "1080p"),
    P720(720, "720p"),
    P480(480, "480p"),
}

enum class QualityPreset(val bitrateMbps: Float?, val label: String) {
    LOW(2.5f, "Низкое"),
    MEDIUM(6f, "Среднее"),
    HIGH(12f, "Высокое"),
    CUSTOM(null, "Свой битрейт"),
}

// Mirrors androidx.media3.common.MimeTypes' video constants as plain literals, so this data
// layer doesn't have to opt in to Media3's @UnstableApi surface just to name a codec.
const val MIME_VIDEO_H264 = "video/avc"
const val MIME_VIDEO_H265 = "video/hevc"
const val MIME_VIDEO_VP9 = "video/x-vnd.on2.vp9"
const val MIME_VIDEO_AV1 = "video/av01"

/** The handful of video mime types we let the user pick between in the UI. */
val SUPPORTED_VIDEO_MIME_TYPES = listOf(MIME_VIDEO_H264, MIME_VIDEO_H265, MIME_VIDEO_VP9, MIME_VIDEO_AV1)

fun videoMimeLabel(mime: String): String = when (mime) {
    MIME_VIDEO_H264 -> "H.264 / AVC"
    MIME_VIDEO_H265 -> "H.265 / HEVC"
    MIME_VIDEO_VP9 -> "VP9"
    MIME_VIDEO_AV1 -> "AV1"
    else -> mime
}

/**
 * Shared settings applied to every job in the current batch. [encoderName] optionally pins a
 * specific MediaCodec encoder instance (as chosen on the diagnostics/codec-picker screen)
 * instead of letting Media3 pick the "best" hardware encoder for [videoMimeType] itself.
 */
data class CompressionSettings(
    val videoMimeType: String = MIME_VIDEO_H265,
    val encoderName: String? = null,
    val resolution: ResolutionPreset = ResolutionPreset.ORIGINAL,
    val quality: QualityPreset = QualityPreset.MEDIUM,
    val customBitrateMbps: Float = 6f,
    val removeAudio: Boolean = false,
) {
    val targetBitrateBps: Int
        get() = (((quality.bitrateMbps ?: customBitrateMbps)) * 1_000_000).toInt().coerceAtLeast(250_000)
}

sealed interface JobStatus {
    data object Queued : JobStatus
    data class Processing(val progress: Int) : JobStatus
    data object Done : JobStatus
    data class Error(val message: String) : JobStatus
    data object Cancelled : JobStatus
}

data class CompressionJob(
    val id: String = UUID.randomUUID().toString(),
    val sourceUri: Uri,
    val displayName: String,
    val sizeBytes: Long = 0L,
    val status: JobStatus = JobStatus.Queued,
    val outputUri: Uri? = null,
    val outputSizeBytes: Long = 0L,
)
