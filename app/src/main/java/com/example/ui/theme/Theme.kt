package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = DiyaAmberPrimary,
    onPrimary = DiyaAmberOnPrimary,
    primaryContainer = DiyaAmberContainer,
    onPrimaryContainer = DiyaAmberOnContainer,
    secondary = DiyaCyanAccent,
    onSecondary = DiyaDarkBackground,
    secondaryContainer = DiyaDarkSurfaceVariant,
    onSecondaryContainer = DiyaTextPrimary,
    tertiary = DiyaGold,
    background = DiyaDarkBackground,
    onBackground = DiyaTextPrimary,
    surface = DiyaDarkSurface,
    onSurface = DiyaTextPrimary,
    surfaceVariant = DiyaDarkSurfaceVariant,
    onSurfaceVariant = DiyaTextSecondary,
    outline = DiyaDarkBorder,
    error = DiyaRoseError
)

private val LightColorScheme = DarkColorScheme // Diya is dark-first personal AI operating system

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent dark AI terminal look
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
