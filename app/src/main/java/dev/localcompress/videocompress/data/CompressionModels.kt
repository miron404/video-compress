package dev.localcompress.videocompress.data

import android.net.Uri
import androidx.media3.common.MimeTypes
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

/** The handful of video mime types we let the user pick between in the UI. */
val SUPPORTED_VIDEO_MIME_TYPES = listOf(
    MimeTypes.VIDEO_H264,
    MimeTypes.VIDEO_H265,
    MimeTypes.VIDEO_VP9,
    MimeTypes.VIDEO_AV1,
)

fun videoMimeLabel(mime: String): String = when (mime) {
    MimeTypes.VIDEO_H264 -> "H.264 / AVC"
    MimeTypes.VIDEO_H265 -> "H.265 / HEVC"
    MimeTypes.VIDEO_VP9 -> "VP9"
    MimeTypes.VIDEO_AV1 -> "AV1"
    else -> mime
}

/**
 * Shared settings applied to every job in the current batch. [encoderName] optionally pins a
 * specific MediaCodec encoder instance (as chosen on the diagnostics/codec-picker screen)
 * instead of letting Media3 pick the "best" hardware encoder for [videoMimeType] itself.
 */
data class CompressionSettings(
    val videoMimeType: String = MimeTypes.VIDEO_H265,
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
