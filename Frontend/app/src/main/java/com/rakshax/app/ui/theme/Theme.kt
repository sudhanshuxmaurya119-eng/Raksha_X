package com.rakshax.app.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = EmergencyRed,
    onPrimary = SolidDarkTextPrimary,
    primaryContainer = EmergencyRedDark,
    onPrimaryContainer = SolidDarkTextPrimary,
    secondary = SafeGreen,
    onSecondary = SolidDarkTextPrimary,
    background = SolidDarkBackground,
    onBackground = SolidDarkTextPrimary,
    surface = SolidDarkSurface,
    onSurface = SolidDarkTextPrimary,
    surfaceVariant = SolidDarkSurfaceElevated,
    onSurfaceVariant = SolidDarkTextSecondary,
    outline = SolidDarkSurfaceBorder,
    error = EmergencyRed,
    onError = SolidDarkTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = EmergencyRed,
    onPrimary = SolidLightBackground,
    primaryContainer = EmergencyRedDark,
    onPrimaryContainer = SolidLightBackground,
    secondary = SafeGreen,
    onSecondary = SolidLightBackground,
    background = SolidLightBackground,
    onBackground = SolidLightTextPrimary,
    surface = SolidLightSurface,
    onSurface = SolidLightTextPrimary,
    surfaceVariant = SolidLightSurfaceElevated,
    onSurfaceVariant = SolidLightTextSecondary,
    outline = SolidLightSurfaceBorder,
    error = EmergencyRed,
    onError = SolidLightBackground
)

object RakshaXTheme {
    val colors: RakshaXThemeColors
        @Composable
        get() = LocalRakshaXColors.current
}

@Composable
fun RakshaXTheme(
    content: @Composable () -> Unit
) {
    val currentMode by ThemeManager.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()

    val isDark = when (currentMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
    }

    val rakshaColors = if (isDark) DarkRakshaXColors else LightRakshaXColors
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = AndroidColor.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = AndroidColor.TRANSPARENT
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(LocalRakshaXColors provides rakshaColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
