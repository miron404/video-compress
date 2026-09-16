package dev.localcompress.videocompress.data

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Process-wide singleton shared between the UI (Activity/ViewModel) and [CompressionService].
 * A bound/messenger service would be more "correct" for a multi-process app, but this app never
 * runs the service in a separate process, so a plain in-memory shared state holder is simpler
 * and avoids a whole layer of IPC boilerplate.
 */
object JobRepository {
    private val _jobs = MutableStateFlow<List<CompressionJob>>(emptyList())
    val jobs: StateFlow<List<CompressionJob>> = _jobs

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    fun setProcessing(value: Boolean) {
        _isProcessing.value = value
    }

    fun addJobs(newJobs: List<CompressionJob>) {
        _jobs.update { current ->
            val existingUris = current.map { it.sourceUri }.toSet()
            current + newJobs.filter { it.sourceUri !in existingUris }
        }
    }

    fun removeJob(id: String) {
        _jobs.update { current -> current.filterNot { it.id == id } }
    }

    fun clearFinished() {
        _jobs.update { current -> current.filterNot { it.status is JobStatus.Done || it.status is JobStatus.Cancelled } }
    }

    fun updateStatus(id: String, status: JobStatus) {
        _jobs.update { current -> current.map { if (it.id == id) it.copy(status = status) else it } }
    }

    fun updateOutput(id: String, outputUri: Uri, outputSizeBytes: Long) {
        _jobs.update { current ->
            current.map {
                if (it.id == id) it.copy(status = JobStatus.Done, outputUri = outputUri, outputSizeBytes = outputSizeBytes) else it
            }
        }
    }

    fun resetQueuedAndErrorToQueued() {
        _jobs.update { current ->
            current.map { if (it.status is JobStatus.Error) it.copy(status = JobStatus.Queued) else it }
        }
    }

    fun nextQueued(): CompressionJob? = _jobs.value.firstOrNull { it.status is JobStatus.Queued }

    fun snapshot(): List<CompressionJob> = _jobs.value
}
