package com.book.ng.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class JellyThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

private val JellyTypography = Typography()

private fun jellyColorScheme(c: JellyColors, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = c.primary,
        onPrimary = Color(0xFF1A1A1A),
        secondary = c.secondary,
        onSecondary = Color(0xFF1A1A1A),
        tertiary = c.tertiary,
        onTertiary = Color(0xFF1A1A1A),
        primaryContainer = c.primaryContainer,
        secondaryContainer = c.secondaryContainer,
        tertiaryContainer = c.tertiaryContainer,
        background = c.background,
        surface = c.surface,
    )
} else {
    lightColorScheme(
        primary = c.primary,
        secondary = c.secondary,
        tertiary = c.tertiary,
        primaryContainer = c.primaryContainer,
        secondaryContainer = c.secondaryContainer,
        tertiaryContainer = c.tertiaryContainer,
        background = c.background,
        surface = c.surface,
    )
}

@Composable
fun NGBookTheme(
    themeMode: JellyThemeMode = JellyThemeMode.SYSTEM,
    palette: JellyPalette = JellyPalette.MATCHA,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        JellyThemeMode.SYSTEM -> isSystemInDarkTheme()
        JellyThemeMode.LIGHT -> false
        JellyThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = jellyColorScheme(jellyColors(palette, isDark), isDark),
        typography = JellyTypography,
        content = content,
    )
}
