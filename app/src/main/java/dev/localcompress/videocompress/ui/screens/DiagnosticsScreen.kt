package dev.localcompress.videocompress.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.localcompress.videocompress.codec.CodecEntry
import dev.localcompress.videocompress.codec.CodecInfoProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(onBack: () -> Unit) {
    var showAll by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val codecs = remember(showAll) { CodecInfoProvider.listCodecs(showAll) }
    val encoders = remember(codecs) { codecs.filter { it.isEncoder && it.capabilities.any { c -> c.isVideo } } }
    val decoders = remember(codecs) { codecs.filter { !it.isEncoder && it.capabilities.any { c -> c.isVideo } } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Диагностика кодеков") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                },
                actions = {
                    IconButton(onClick = {
                        val report = buildReport(encoders, decoders)
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(ClipData.newPlainText("codec_report", report))
                        Toast.makeText(context, "Отчёт скопирован в буфер обмена", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Default.ContentCopy, contentDescription = "Скопировать отчёт") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp, 8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Показать все кодеки (включая нерекомендуемые)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = showAll, onCheckedChange = { showAll = it })
            }

            LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp, 0.dp, 16.dp, 16.dp)) {
                item { SectionHeader("Энкодеры (для сжатия) — ${encoders.size}") }
                items(encoders, key = { "enc_" + it.name }) { CodecCard(it) }

                item { Spacer(Modifier.height(16.dp)); SectionHeader("Декодеры (для воспроизведения) — ${decoders.size}") }
                items(decoders, key = { "dec_" + it.name }) { CodecCard(it) }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun CodecCard(entry: CodecEntry) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize()
            .clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                AssistChip(onClick = {}, enabled = false, label = { Text(if (entry.isHardwareAccelerated) "HW" else "SW") })
                if (entry.isVendor) AssistChip(onClick = {}, enabled = false, label = { Text("Vendor") })
            }
            Text(
                entry.capabilities.joinToString(", ") { it.mimeType },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                entry.capabilities.filter { it.isVideo }.forEach { cap ->
                    Text(cap.mimeType, style = MaterialTheme.typography.labelLarge)
                    cap.bitrateRange?.let { Text("Битрейт: ${it.first / 1000}–${it.last / 1000} кбит/с", style = MaterialTheme.typography.bodySmall) }
                    if (cap.widthRange != null && cap.heightRange != null) {
                        Text(
                            "Разрешение: ${cap.widthRange.first}x${cap.heightRange.first} – ${cap.widthRange.last}x${cap.heightRange.last}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    cap.frameRateRange?.let { Text("Частота кадров: ${it.start.toInt()}–${it.endInclusive.toInt()} fps", style = MaterialTheme.typography.bodySmall) }
                    cap.maxSupportedInstances?.let { Text("Макс. одновременных сессий: $it", style = MaterialTheme.typography.bodySmall) }
                    if (cap.profileLevels.isNotEmpty()) {
                        Text("Профили/уровни: " + cap.profileLevels.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                    }
                    if (cap.colorFormats.isNotEmpty()) {
                        Text("Форматы цвета: " + cap.colorFormats.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

private fun buildReport(encoders: List<CodecEntry>, decoders: List<CodecEntry>): String = buildString {
    appendLine("=== Видео-энкодеры ===")
    encoders.forEach { e ->
        appendLine("${e.name} [${if (e.isHardwareAccelerated) "HW" else "SW"}${if (e.isVendor) ", vendor" else ""}]")
        e.capabilities.filter { it.isVideo }.forEach { c ->
            appendLine("  ${c.mimeType} bitrate=${c.bitrateRange} size=${c.widthRange}x${c.heightRange} fps=${c.frameRateRange} maxInstances=${c.maxSupportedInstances}")
            if (c.profileLevels.isNotEmpty()) appendLine("    profiles: ${c.profileLevels.joinToString(", ")}")
        }
    }
    appendLine()
    appendLine("=== Видео-декодеры ===")
    decoders.forEach { d ->
        appendLine("${d.name} [${if (d.isHardwareAccelerated) "HW" else "SW"}${if (d.isVendor) ", vendor" else ""}]")
    }
}
