package dev.localcompress.videocompress.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.localcompress.videocompress.codec.CodecInfoProvider
import dev.localcompress.videocompress.data.CompressionSettings
import dev.localcompress.videocompress.data.QualityPreset
import dev.localcompress.videocompress.data.ResolutionPreset
import dev.localcompress.videocompress.data.SUPPORTED_VIDEO_MIME_TYPES
import dev.localcompress.videocompress.data.videoMimeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheetContent(
    settings: CompressionSettings,
    outputFolderLabel: String?,
    onSettingsChange: (CompressionSettings) -> Unit,
    onPickOutputFolder: () -> Unit,
) {
    Column(Modifier.padding(20.dp)) {
        Text("Настройки сжатия", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        // Output folder
        Card {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        outputFolderLabel ?: "Не выбрана",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                TextButton(onClick = onPickOutputFolder) { Text("Изменить") }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Видеокодек", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        CodecDropdown(
            selectedMime = settings.videoMimeType,
            selectedEncoder = settings.encoderName,
            onSelected = { mime, encoder -> onSettingsChange(settings.copy(videoMimeType = mime, encoderName = encoder)) },
        )

        Spacer(Modifier.height(20.dp))
        Text("Разрешение", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ResolutionPreset.entries.forEachIndexed { index, preset ->
                SegmentedButton(
                    selected = settings.resolution == preset,
                    onClick = { onSettingsChange(settings.copy(resolution = preset)) },
                    shape = SegmentedButtonDefaults.itemShape(index, ResolutionPreset.entries.size),
                ) { Text(preset.label) }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Качество", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            QualityPreset.entries.forEachIndexed { index, preset ->
                SegmentedButton(
                    selected = settings.quality == preset,
                    onClick = { onSettingsChange(settings.copy(quality = preset)) },
                    shape = SegmentedButtonDefaults.itemShape(index, QualityPreset.entries.size),
                ) { Text(preset.label) }
            }
        }
        if (settings.quality == QualityPreset.CUSTOM) {
            Spacer(Modifier.height(8.dp))
            Text("Битрейт: ${"%.1f".format(settings.customBitrateMbps)} Мбит/с")
            Slider(
                value = settings.customBitrateMbps,
                onValueChange = { onSettingsChange(settings.copy(customBitrateMbps = it)) },
                valueRange = 0.5f..30f,
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Удалить звук", style = MaterialTheme.typography.labelLarge)
            Switch(checked = settings.removeAudio, onCheckedChange = { onSettingsChange(settings.copy(removeAudio = it)) })
        }
        Spacer(Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CodecDropdown(
    selectedMime: String,
    selectedEncoder: String?,
    onSelected: (mime: String, encoderName: String?) -> Unit,
) {
    var mimeExpanded by remember { mutableStateOf(false) }
    val encoders = remember(selectedMime) { CodecInfoProvider.recommendedVideoEncoders(selectedMime) }

    ExposedDropdownMenuBox(expanded = mimeExpanded, onExpandedChange = { mimeExpanded = it }) {
        OutlinedTextField(
            readOnly = true,
            value = videoMimeLabel(selectedMime),
            onValueChange = {},
            label = { Text("Формат") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mimeExpanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = mimeExpanded, onDismissRequest = { mimeExpanded = false }) {
            SUPPORTED_VIDEO_MIME_TYPES.forEach { mime ->
                val hasHw = CodecInfoProvider.recommendedVideoEncoders(mime).any { it.isHardwareAccelerated }
                val hasAny = CodecInfoProvider.hasAnyEncoderFor(mime)
                DropdownMenuItem(
                    text = {
                        Text(videoMimeLabel(mime) + if (!hasAny) " (недоступно)" else if (hasHw) " · HW" else " · SW")
                    },
                    enabled = hasAny,
                    onClick = {
                        mimeExpanded = false
                        onSelected(mime, null)
                    },
                )
            }
        }
    }

    if (encoders.size > 1) {
        Spacer(Modifier.height(8.dp))
        var encoderExpanded by remember { mutableStateOf(false) }
        val currentLabel = encoders.firstOrNull { it.name == selectedEncoder }?.name ?: "Авто (лучший доступный)"
        ExposedDropdownMenuBox(expanded = encoderExpanded, onExpandedChange = { encoderExpanded = it }) {
            OutlinedTextField(
                readOnly = true,
                value = currentLabel,
                onValueChange = {},
                label = { Text("Конкретный кодек (необязательно)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = encoderExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = encoderExpanded, onDismissRequest = { encoderExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Авто (лучший доступный)") },
                    onClick = { encoderExpanded = false; onSelected(selectedMime, null) },
                )
                encoders.forEach { enc ->
                    DropdownMenuItem(
                        text = { Text(enc.name + if (enc.isHardwareAccelerated) " · HW" else " · SW") },
                        onClick = { encoderExpanded = false; onSelected(selectedMime, enc.name) },
                    )
                }
            }
        }
    }
}
