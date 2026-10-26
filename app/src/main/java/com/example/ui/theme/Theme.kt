package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val NeonDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF001F26),
    primaryContainer = Color(0xFF003642),
    onPrimaryContainer = Color(0xFFB8F4FF),
    secondary = NeonMagenta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A1230),
    onSecondaryContainer = Color(0xFFFFD9E5),
    tertiary = NeonCoral,
    onTertiary = Color.White,
    background = NeonBackground,
    onBackground = TextPrimaryDark,
    surface = NeonSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NeonSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = NeonCardBorder
)

private val WarmLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBD2),
    onPrimaryContainer = Color(0xFF3D0A00),
    secondary = LightSecondary,
    onSecondary = Color.White,
    tertiary = NeonPurple,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = Color(0xFFDDD8CC)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NeonDarkColorScheme else WarmLightColorScheme

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
