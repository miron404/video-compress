package dev.localcompress.videocompress

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import dev.localcompress.videocompress.data.AppPreferences
import dev.localcompress.videocompress.data.CompressionJob
import dev.localcompress.videocompress.data.CompressionSettings
import dev.localcompress.videocompress.data.JobRepository
import dev.localcompress.videocompress.transcode.CompressionService
import dev.localcompress.videocompress.util.MediaFileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)

    val jobs: StateFlow<List<CompressionJob>> = JobRepository.jobs
    val isProcessing: StateFlow<Boolean> = JobRepository.isProcessing

    private val _settings = MutableStateFlow(prefs.loadSettings(CompressionSettings()))
    val settings: StateFlow<CompressionSettings> = _settings

    private val _outputFolder = MutableStateFlow(prefs.outputFolderUri)
    val outputFolder: StateFlow<Uri?> = _outputFolder

    fun updateSettings(newSettings: CompressionSettings) {
        _settings.value = newSettings
        prefs.saveSettings(newSettings)
    }

    fun setOutputFolder(uri: Uri) {
        val context = getApplication<Application>()
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        prefs.outputFolderUri = uri
        _outputFolder.value = uri
    }

    fun addVideos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val context = getApplication<Application>()
        val newJobs = uris.map { uri ->
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val info = MediaFileUtils.queryFileInfo(context, uri)
            CompressionJob(sourceUri = uri, displayName = info.displayName, sizeBytes = info.sizeBytes)
        }
        JobRepository.addJobs(newJobs)
    }

    fun removeJob(id: String) = JobRepository.removeJob(id)

    fun clearFinished() = JobRepository.clearFinished()

    fun startCompression() {
        val context = getApplication<Application>()
        val intent = Intent(context, CompressionService::class.java).setAction(CompressionService.ACTION_START)
        ContextCompat.startForegroundService(context, intent)
    }

    fun cancelCompression() {
        val context = getApplication<Application>()
        val intent = Intent(context, CompressionService::class.java).setAction(CompressionService.ACTION_CANCEL)
        context.startService(intent)
    }
}
