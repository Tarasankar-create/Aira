package com.example.aira.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AiraDarkColorScheme = darkColorScheme(
    primary = AiraCyan,
    onPrimary = BackgroundDark,
    primaryContainer = SurfaceVariantDark,
    onPrimaryContainer = AiraCyan,
    secondary = AiraPurple,
    onSecondary = TextPrimary,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun AiraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AiraDarkColorScheme,
        typography = Typography,
        content = content
    )
}
