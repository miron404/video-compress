package dev.localcompress.videocompress.transcode

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.TransformationRequest
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import androidx.media3.effect.Presentation
import dev.localcompress.videocompress.data.CompressionSettings
import java.io.File
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

sealed interface TranscodeEvent {
    data class Progress(val percent: Int) : TranscodeEvent
    data class Success(val outputFile: File) : TranscodeEvent
    data class Failure(val message: String) : TranscodeEvent
}

/**
 * Thin wrapper around Media3's [Transformer]. Transformer must be created and driven from a
 * single thread that has a [Looper] (we use the main thread); all callbacks below are therefore
 * posted through [mainHandler] rather than called directly from the collecting coroutine.
 */
object VideoTranscoder {

    fun transcode(
        context: Context,
        sourceUri: Uri,
        outputFile: File,
        settings: CompressionSettings,
    ): Flow<TranscodeEvent> = callbackFlow {
        val appContext = context.applicationContext
        val mainHandler = Handler(Looper.getMainLooper())
        val progressHolder = ProgressHolder()
        var transformer: Transformer? = null
        var progressRunnable: Runnable? = null

        val listener = object : Transformer.Listener {
            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                trySend(TranscodeEvent.Success(outputFile))
                close()
            }

            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                trySend(TranscodeEvent.Failure(exportException.message ?: "Ошибка кодирования видео"))
                close()
            }
        }

        mainHandler.post {
            try {
                val encoderFactory = DefaultEncoderFactory.Builder(appContext)
                    .setRequestedVideoEncoderSettings(
                        VideoEncoderSettings.Builder().setBitrate(settings.targetBitrateBps).build()
                    )
                    .setEncoderSelector(PinnedEncoderSelector(settings.encoderName))
                    .build()

                val transformationRequest = TransformationRequest.Builder()
                    .setVideoMimeType(settings.videoMimeType)
                    .build()

                val videoEffects = buildList {
                    settings.resolution.targetHeight?.let { add(Presentation.createForHeight(it)) }
                }

                val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(sourceUri))
                    .setRemoveAudio(settings.removeAudio)
                    .setEffects(Effects(emptyList(), videoEffects))
                    .build()

                val t = Transformer.Builder(appContext)
                    .setTransformationRequest(transformationRequest)
                    .setEncoderFactory(encoderFactory)
                    .addListener(listener)
                    .build()
                transformer = t
                t.start(editedMediaItem, outputFile.absolutePath)

                val runnable = object : Runnable {
                    override fun run() {
                        val state = t.getProgress(progressHolder)
                        if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                            trySend(TranscodeEvent.Progress(progressHolder.progress))
                        }
                        if (state != Transformer.PROGRESS_STATE_NOT_STARTED) {
                            mainHandler.postDelayed(this, 400)
                        }
                    }
                }
                progressRunnable = runnable
                mainHandler.postDelayed(runnable, 400)
            } catch (t: Throwable) {
                trySend(TranscodeEvent.Failure(t.message ?: "Не удалось запустить сжатие"))
                close()
            }
        }

        awaitClose {
            mainHandler.post {
                progressRunnable?.let { mainHandler.removeCallbacks(it) }
                runCatching { transformer?.cancel() }
            }
        }
    }
}
