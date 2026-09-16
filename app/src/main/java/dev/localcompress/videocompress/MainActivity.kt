package dev.localcompress.videocompress

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import dev.localcompress.videocompress.data.CompressionJob
import dev.localcompress.videocompress.data.OutputWriter
import dev.localcompress.videocompress.data.ResolutionPreset
import dev.localcompress.videocompress.data.videoMimeLabel
import dev.localcompress.videocompress.ui.screens.DiagnosticsScreen
import dev.localcompress.videocompress.ui.screens.HomeScreen
import dev.localcompress.videocompress.ui.screens.SettingsSheetContent
import dev.localcompress.videocompress.ui.theme.VideoCompressTheme

private enum class Screen { HOME, DIAGNOSTICS }

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.addVideos(extractIncomingUris(intent))

        setContent {
            VideoCompressTheme {
                AppRoot(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.addVideos(extractIncomingUris(intent))
    }

    private fun extractIncomingUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        return when (intent.action) {
            Intent.ACTION_VIEW -> listOfNotNull(intent.data)
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                listOfNotNull(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION") intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                }
                uris.orEmpty()
            }
            else -> emptyList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(viewModel: MainViewModel) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.HOME) }
    var showSettings by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val jobs by viewModel.jobs.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val outputFolder by viewModel.outputFolder.collectAsState()

    val outputFolderLabel = remember(outputFolder) {
        outputFolder?.let { runCatching { OutputWriter.folderDisplayName(context, it) }.getOrNull() ?: it.toString() }
    }

    val addVideosLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        viewModel.addVideos(uris)
    }
    val pickFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { viewModel.setOutputFolder(it) }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (granted != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    when (screen) {
        Screen.DIAGNOSTICS -> DiagnosticsScreen(onBack = { screen = Screen.HOME })
        Screen.HOME -> {
            HomeScreen(
                jobs = jobs,
                isProcessing = isProcessing,
                outputFolderLabel = outputFolderLabel,
                settingsSummary = settingsSummary(settings),
                onAddVideos = { addVideosLauncher.launch(arrayOf("video/*")) },
                onOpenDiagnostics = { screen = Screen.DIAGNOSTICS },
                onOpenSettings = { showSettings = true },
                onPickOutputFolder = { pickFolderLauncher.launch(null) },
                onCompressAll = { viewModel.startCompression() },
                onCancel = { viewModel.cancelCompression() },
                onRemove = { viewModel.removeJob(it) },
                onShare = { shareJob(context, it) },
                onOpen = { openJob(context, it) },
                onClearFinished = { viewModel.clearFinished() },
            )

            if (showSettings) {
                ModalBottomSheet(onDismissRequest = { showSettings = false }, sheetState = sheetState) {
                    SettingsSheetContent(
                        settings = settings,
                        outputFolderLabel = outputFolderLabel,
                        onSettingsChange = { viewModel.updateSettings(it) },
                        onPickOutputFolder = { pickFolderLauncher.launch(null) },
                    )
                }
            }
        }
    }
}

private fun settingsSummary(settings: dev.localcompress.videocompress.data.CompressionSettings): String = buildString {
    append(videoMimeLabel(settings.videoMimeType))
    append(" · ")
    append(if (settings.resolution == ResolutionPreset.ORIGINAL) "оригинал" else settings.resolution.label)
    append(" · ")
    append(settings.quality.label)
    if (settings.removeAudio) append(" · без звука")
}

private fun shareJob(context: android.content.Context, job: CompressionJob) {
    val uri = job.outputUri ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_share))) }
        .onFailure { Toast.makeText(context, "Не удалось открыть диалог «Поделиться»", Toast.LENGTH_SHORT).show() }
}

private fun openJob(context: android.content.Context, job: CompressionJob) {
    val uri = job.outputUri ?: return
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/mp4")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(intent) }
        .onFailure { Toast.makeText(context, "Нет приложения для просмотра видео", Toast.LENGTH_SHORT).show() }
}
