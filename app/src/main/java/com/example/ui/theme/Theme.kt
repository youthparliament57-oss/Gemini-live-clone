package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GeminiLiveColorScheme = darkColorScheme(
    primary = GeminiBlue,
    secondary = GeminiPurple,
    tertiary = GeminiCyan,
    background = GeminiDarkBg,
    surface = GeminiSurfaceDark,
    surfaceVariant = GeminiSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = GeminiOnSurface,
    onSurface = GeminiOnSurface,
    onSurfaceVariant = GeminiOnSurfaceVariant
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = GeminiLiveColorScheme,
        typography = Typography,
        content = content
    )
}

