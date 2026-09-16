package dev.localcompress.videocompress.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Blue = Color(0xFF3D6FE0)
private val BlueDark = Color(0xFF8AB4F8)

private val LightColors = lightColorScheme(
    primary = Blue,
    secondary = Color(0xFF5E5E6B),
)

private val DarkColors = darkColorScheme(
    primary = BlueDark,
    secondary = Color(0xFFC7C6D2),
)

@Composable
fun VideoCompressTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
