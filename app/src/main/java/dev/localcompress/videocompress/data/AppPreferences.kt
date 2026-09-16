package dev.localcompress.videocompress.data

import android.content.Context
import android.net.Uri

/** Tiny SharedPreferences wrapper: persists the SAF output-folder Uri and last-used settings. */
class AppPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("video_compress_prefs", Context.MODE_PRIVATE)

    var outputFolderUri: Uri?
        get() = prefs.getString(KEY_OUTPUT_FOLDER, null)?.let(Uri::parse)
        set(value) = prefs.edit().putString(KEY_OUTPUT_FOLDER, value?.toString()).apply()

    var videoMimeType: String?
        get() = prefs.getString(KEY_MIME, null)
        set(value) = prefs.edit().putString(KEY_MIME, value).apply()

    var encoderName: String?
        get() = prefs.getString(KEY_ENCODER, null)
        set(value) = prefs.edit().putString(KEY_ENCODER, value).apply()

    var resolutionName: String?
        get() = prefs.getString(KEY_RESOLUTION, null)
        set(value) = prefs.edit().putString(KEY_RESOLUTION, value).apply()

    var qualityName: String?
        get() = prefs.getString(KEY_QUALITY, null)
        set(value) = prefs.edit().putString(KEY_QUALITY, value).apply()

    var customBitrateMbps: Float
        get() = prefs.getFloat(KEY_CUSTOM_BITRATE, 6f)
        set(value) = prefs.edit().putFloat(KEY_CUSTOM_BITRATE, value).apply()

    var removeAudio: Boolean
        get() = prefs.getBoolean(KEY_REMOVE_AUDIO, false)
        set(value) = prefs.edit().putBoolean(KEY_REMOVE_AUDIO, value).apply()

    fun loadSettings(default: CompressionSettings): CompressionSettings = CompressionSettings(
        videoMimeType = videoMimeType ?: default.videoMimeType,
        encoderName = encoderName,
        resolution = resolutionName?.let { name -> runCatching { ResolutionPreset.valueOf(name) }.getOrNull() } ?: default.resolution,
        quality = qualityName?.let { name -> runCatching { QualityPreset.valueOf(name) }.getOrNull() } ?: default.quality,
        customBitrateMbps = customBitrateMbps,
        removeAudio = removeAudio,
    )

    fun saveSettings(settings: CompressionSettings) {
        videoMimeType = settings.videoMimeType
        encoderName = settings.encoderName
        resolutionName = settings.resolution.name
        qualityName = settings.quality.name
        customBitrateMbps = settings.customBitrateMbps
        removeAudio = settings.removeAudio
    }

    companion object {
        private const val KEY_OUTPUT_FOLDER = "output_folder_uri"
        private const val KEY_MIME = "video_mime"
        private const val KEY_ENCODER = "encoder_name"
        private const val KEY_RESOLUTION = "resolution"
        private const val KEY_QUALITY = "quality"
        private const val KEY_CUSTOM_BITRATE = "custom_bitrate"
        private const val KEY_REMOVE_AUDIO = "remove_audio"
    }
}
