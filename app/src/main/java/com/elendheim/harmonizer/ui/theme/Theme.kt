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
import com.elendheim.harmonizer.ThemeMode

private val DarkColors = darkColorScheme(
    primary = VoiceAccent,
    secondary = FifthAccent,
    background = Background,
    surface = Surface,
    onBackground = OnSurface,
    onSurface = OnSurface,
    error = Danger,
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E8A7A),
    secondary = Color(0xFFB9791F),
    background = Color(0xFFF6F8F9),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF14181D),
    onSurface = Color(0xFF14181D),
    error = Danger,
)

// High contrast pushes the background darker/lighter and the text to the edges
// of the range, so everything reads clearly.
private val DarkHighContrast = darkColorScheme(
    primary = VoiceAccent,
    secondary = FifthAccent,
    background = Color(0xFF000000),
    surface = Color(0xFF0C0F13),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    error = Danger,
)
private val LightHighContrast = lightColorScheme(
    primary = Color(0xFF0E8A7A),
    secondary = Color(0xFFB9791F),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    onSurface = Color(0xFF000000),
    error = Danger,
)

@Composable
fun ElendheimHarmonizerTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colors = when {
        dark && highContrast -> DarkHighContrast
        dark -> DarkColors
        highContrast -> LightHighContrast
        else -> LightColors
    }

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
