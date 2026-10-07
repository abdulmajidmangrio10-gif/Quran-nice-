package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val EditorColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.Black,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = EmeraldLight,
    secondary = GoldAccent,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF452205),
    onSecondaryContainer = GoldLight,
    tertiary = CyanTrackColor(),
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    error = ErrorRed,
    onError = Color.White
)

private fun CyanTrackColor() = Color(0xFF06B6D4)

@Composable
fun QuranVideoEditorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EditorColorScheme,
        typography = Typography,
        content = content
    )
}
