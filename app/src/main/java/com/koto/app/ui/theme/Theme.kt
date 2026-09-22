package com.koto.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor

private val Colors = lightColorScheme(
    primary = KotoColors.Accent,
    onPrimary = KotoColors.BarSurface,
    primaryContainer = KotoColors.SoftGrey,
    onPrimaryContainer = KotoColors.Navy,
    secondary = KotoColors.Gold,
    onSecondary = KotoColors.Background,
    secondaryContainer = KotoColors.SoftGrey,
    onSecondaryContainer = KotoColors.Navy,
    background = KotoColors.Background,
    onBackground = KotoColors.Ink,
    surface = KotoColors.BarSurface,
    onSurface = KotoColors.Ink,
    onSurfaceVariant = KotoColors.QuietInk,
    surfaceVariant = KotoColors.SoftGrey,
    surfaceContainerHighest = KotoColors.SoftGrey,
    surfaceContainerHigh = KotoColors.SoftGrey,
    surfaceContainer = KotoColors.Background,
    surfaceContainerLow = KotoColors.Background,
    surfaceContainerLowest = KotoColors.Background,
    surfaceTint = KotoColors.Navy,
    outline = KotoColors.QuietInk,
    outlineVariant = KotoColors.Hairline,
)

@Composable
fun KotoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = KotoTypography) {
        CompositionLocalProvider(LocalContentColor provides KotoColors.Navy, content = content)
    }
}
