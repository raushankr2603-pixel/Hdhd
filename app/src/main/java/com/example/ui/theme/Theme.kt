package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AetherColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF001F24),
    primaryContainer = CyberCyanDim,
    onPrimaryContainer = Color(0xFFD0F9FF),
    secondary = QuantumEmerald,
    onSecondary = Color(0xFF002114),
    secondaryContainer = QuantumEmeraldDim,
    onSecondaryContainer = Color(0xFFC6FFE6),
    tertiary = NeuralViolet,
    onTertiary = Color.White,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = CrimsonError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AetherColorScheme,
        typography = Typography,
        content = content
    )
}
