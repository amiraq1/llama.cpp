package com.localai.hub.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = DeepInk,
    onPrimary = WarmSurface,
    secondary = Sea,
    tertiary = Clay,
    background = Sand,
    surface = WarmSurface,
    surfaceVariant = Mist,
)

private val DarkColors = darkColorScheme(
    primary = Sand,
    onPrimary = DeepInk,
    secondary = Clay,
    tertiary = Sea,
    background = DeepInk,
    surface = Color(0xFF102030),
    surfaceVariant = Color(0xFF1C3347),
)

@Composable
fun LocalAIHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}

