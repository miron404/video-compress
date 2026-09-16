package dev.localcompress.videocompress.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.localcompress.videocompress.R
import dev.localcompress.videocompress.data.CompressionJob
import dev.localcompress.videocompress.data.JobStatus
import dev.localcompress.videocompress.util.MediaFileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    jobs: List<CompressionJob>,
    isProcessing: Boolean,
    outputFolderLabel: String?,
    settingsSummary: String,
    onAddVideos: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenSettings: () -> Unit,
    onPickOutputFolder: () -> Unit,
    onCompressAll: () -> Unit,
    onCancel: () -> Unit,
    onRemove: (String) -> Unit,
    onShare: (CompressionJob) -> Unit,
    onOpen: (CompressionJob) -> Unit,
    onClearFinished: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenDiagnostics) {
                        Icon(Icons.Default.BugReport, contentDescription = stringResource(R.string.diagnostics_title))
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        AssistChip(
                            onClick = onOpenSettings,
                            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                            label = { Text(settingsSummary, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        )
                        if (jobs.any { it.status is JobStatus.Done }) {
                            TextButton(onClick = onClearFinished) { Text("Очистить готовые") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    val queuedCount = jobs.count { it.status is JobStatus.Queued }
                    if (isProcessing) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = onCancel, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Отменить")
                            }
                        }
                    } else {
                        Button(
                            onClick = onCompressAll,
                            enabled = queuedCount > 0 && outputFolderLabel != null,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.compress_all, queuedCount))
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            androidx.compose.material3.FloatingActionButton(onClick = onAddVideos) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_videos))
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (outputFolderLabel == null) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(stringResource(R.string.output_folder_not_set), color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(onClick = onPickOutputFolder) { Text(stringResource(R.string.output_folder_pick)) }
                    }
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.output_folder_label, outputFolderLabel),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(onClick = onPickOutputFolder) { Text(stringResource(R.string.output_folder_pick)) }
                }
            }

            if (jobs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.queue_empty_title), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.queue_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(jobs, key = { it.id }) { job ->
                        JobCard(job, onRemove = { onRemove(job.id) }, onShare = { onShare(job) }, onOpen = { onOpen(job) })
                    }
                }
            }
        }
    }
}

@Composable
private fun JobCard(
    job: CompressionJob,
    onRemove: () -> Unit,
    onShare: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(job.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        MediaFileUtils.formatBytes(job.sizeBytes) + statusSuffix(job),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (job.status is JobStatus.Queued) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_remove))
                    }
                }
            }

            when (val status = job.status) {
                is JobStatus.Processing -> {
                    Spacer(Modifier.height(8.dp))
                    if (status.progress > 0) {
                        LinearProgressIndicator(progress = { status.progress / 100f }, modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
                is JobStatus.Error -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.status_error, status.message),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                is JobStatus.Done -> {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onOpen) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.action_open))
                        }
                        TextButton(onClick = onShare) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.action_share))
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

private fun statusSuffix(job: CompressionJob): String = when (val s = job.status) {
    is JobStatus.Queued -> " · В очереди"
    is JobStatus.Processing -> " · ${s.progress}%"
    is JobStatus.Done -> " → " + MediaFileUtils.formatBytes(job.outputSizeBytes)
    is JobStatus.Error -> " · Ошибка"
    is JobStatus.Cancelled -> " · Отменено"
}
