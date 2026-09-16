package dev.localcompress.videocompress.transcode

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.localcompress.videocompress.MainActivity
import dev.localcompress.videocompress.R
import dev.localcompress.videocompress.data.AppPreferences
import dev.localcompress.videocompress.data.CompressionSettings
import dev.localcompress.videocompress.data.JobRepository
import dev.localcompress.videocompress.data.JobStatus
import dev.localcompress.videocompress.data.OutputWriter
import dev.localcompress.videocompress.util.MediaFileUtils
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class CompressionService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var queueJob: Job? = null
    private lateinit var prefs: AppPreferences
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences(this)
        notificationManager = getSystemService(NotificationManager::class.java)
        ensureChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                queueJob?.cancel()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> startQueueIfNeeded()
        }
        return START_NOT_STICKY
    }

    private fun startQueueIfNeeded() {
        if (JobRepository.isProcessing.value) return
        val total = JobRepository.snapshot().count { it.status is JobStatus.Queued }
        if (total == 0) {
            stopSelf()
            return
        }
        JobRepository.setProcessing(true)
        startForeground(NOTIFICATION_ID, buildProgressNotification(0, total, 0, ""))

        queueJob = scope.launch {
            var completed = 0
            while (true) {
                val job = JobRepository.nextQueued() ?: break
                JobRepository.updateStatus(job.id, JobStatus.Processing(0))
                notify(buildProgressNotification(completed, total, 0, job.displayName))

                val settings = currentSettings()
                val tempFile = File(cacheDir, "compress_${job.id}.mp4")
                try {
                    VideoTranscoder.transcode(applicationContext, job.sourceUri, tempFile, settings).collect { event ->
                        when (event) {
                            is TranscodeEvent.Progress -> {
                                JobRepository.updateStatus(job.id, JobStatus.Processing(event.percent))
                                notify(buildProgressNotification(completed, total, event.percent, job.displayName))
                            }
                            is TranscodeEvent.Success -> {
                                val treeUri = prefs.outputFolderUri
                                if (treeUri == null) {
                                    JobRepository.updateStatus(job.id, JobStatus.Error("Папка сохранения не выбрана"))
                                } else {
                                    val name = MediaFileUtils.compressedFileName(job.displayName)
                                    val outUri = OutputWriter.writeToOutputFolder(applicationContext, treeUri, tempFile, name)
                                    JobRepository.updateOutput(job.id, outUri, tempFile.length())
                                }
                            }
                            is TranscodeEvent.Failure -> {
                                JobRepository.updateStatus(job.id, JobStatus.Error(event.message))
                            }
                        }
                    }
                } catch (t: Throwable) {
                    JobRepository.updateStatus(job.id, JobStatus.Error(t.message ?: "Неизвестная ошибка"))
                } finally {
                    tempFile.delete()
                }
                completed++
            }

            JobRepository.setProcessing(false)
            notify(buildDoneNotification(completed))
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun currentSettings(): CompressionSettings =
        AppPreferences(this).loadSettings(CompressionSettings())

    override fun onDestroy() {
        scope.cancel()
        JobRepository.setProcessing(false)
        super.onDestroy()
    }

    private fun notify(notification: android.app.Notification) {
        if (androidx.core.app.ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < 33
        ) {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun contentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(this, 0, intent, flags)
    }

    private fun buildProgressNotification(completed: Int, total: Int, percent: Int, currentName: String): android.app.Notification {
        val cancelIntent = Intent(this, CompressionService::class.java).setAction(ACTION_CANCEL)
        val cancelPendingIntent = PendingIntent.getService(
            this, 1, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.notif_title_progress, (completed + 1).coerceAtMost(total.coerceAtLeast(1)), total))
            .setContentText(currentName)
            .setProgress(100, percent, percent == 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentIntent())
            .addAction(0, "Отмена", cancelPendingIntent)
            .build()
    }

    private fun buildDoneNotification(count: Int): android.app.Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(getString(R.string.notif_title_done))
            .setContentText(getString(R.string.notif_text_done, count))
            .setAutoCancel(true)
            .setContentIntent(contentIntent())
            .build()

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, getString(R.string.notif_channel_name), NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_desc)
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "dev.localcompress.videocompress.action.START"
        const val ACTION_CANCEL = "dev.localcompress.videocompress.action.CANCEL"
        private const val CHANNEL_ID = "compression_progress"
        private const val NOTIFICATION_ID = 42
    }
}
