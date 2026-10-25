package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BizMateColorScheme = darkColorScheme(
    primary = PrimaryViolet,
    onPrimary = TextPrimary,
    primaryContainer = BgGradientPurple,
    onPrimaryContainer = LightLavender,
    secondary = BrightLavender,
    onSecondary = Color(0xFF160826),
    secondaryContainer = GlassCardSolid,
    onSecondaryContainer = LightLavender,
    tertiary = AccentLavender,
    onTertiary = Color.White,
    background = BgMain,
    onBackground = TextPrimary,
    surface = GlassCardSolid,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF2C1945),
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = GlassBorderActive,
    error = StatusError,
    onError = Color.White
)

@Composable
fun BizMateTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BizMateColorScheme,
        typography = AppTypography,
        content = content
    )
}
