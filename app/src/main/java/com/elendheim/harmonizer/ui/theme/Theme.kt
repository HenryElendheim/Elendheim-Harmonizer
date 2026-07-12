package com.elendheim.harmonizer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = VoiceAccent,
    secondary = FifthAccent,
    background = Background,
    surface = Surface,
    onBackground = OnSurface,
    onSurface = OnSurface,
    error = Danger,
)

// A light scheme exists for completeness, but the app is designed dark-first
// and defaults to the dark palette regardless of system setting.
private val LightColors = lightColorScheme(
    primary = Color(0xFF0E8A7A),
    secondary = Color(0xFFB9791F),
    background = Color(0xFFF6F8F9),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF14181D),
    onSurface = Color(0xFF14181D),
    error = Danger,
)

@Composable
fun ElendheimHarmonizerTheme(
    // Dark-first by default. Callers can opt into following the system.
    followSystem: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = if (followSystem) isSystemInDarkTheme() else true
    val colors = if (dark) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = colors.background.toArgb()
        window.navigationBarColor = colors.background.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content,
    )
}
