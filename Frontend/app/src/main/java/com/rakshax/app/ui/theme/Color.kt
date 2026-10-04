package com.rakshax.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// ==========================================
// 1. SOLID PRODUCTION COLOR DEFINITIONS
// ==========================================

// Solid Dark Theme (Obsidian & Deep Black)
val SolidDarkBackground = Color(0xFF09090B)        // Deep solid black canvas
val SolidDarkSurface = Color(0xFF141417)           // Primary card surface
val SolidDarkSurfaceElevated = Color(0xFF1F1F24)   // Secondary card / pill / input
val SolidDarkSurfaceBorder = Color(0xFF2E2E36)     // High-definition hairline border
val SolidDarkTextPrimary = Color(0xFFFFFFFF)       // Crisp white heading & labels
val SolidDarkTextSecondary = Color(0xFFA1A1AA)     // High-readability zinc secondary
val SolidDarkTextMuted = Color(0xFF71717A)         // Neutral muted text
val SolidDarkDivider = Color(0xFF27272A)           // Crisp horizontal divider

// Solid Light Theme (Pure White & Slate)
val SolidLightBackground = Color(0xFFFFFFFF)       // Pure crisp white canvas
val SolidLightSurface = Color(0xFFF4F4F6)          // Soft crisp light card
val SolidLightSurfaceElevated = Color(0xFFE5E5EB)  // Secondary card / pill / input
val SolidLightSurfaceBorder = Color(0xFFE2E2E8)    // Subtle light border
val SolidLightTextPrimary = Color(0xFF09090B)      // Solid rich black heading & labels
val SolidLightTextSecondary = Color(0xFF52525B)    // Dark slate secondary text
val SolidLightTextMuted = Color(0xFF71717A)        // Muted gray text
val SolidLightDivider = Color(0xFFE4E4E7)          // Crisp light divider

// ==========================================
// 2. CRITICAL FUNCTIONAL ACTION COLORS
// (High contrast on both Black & White)
// ==========================================

// Red: Critical SOS / Emergency / Alert / Delete
val EmergencyRed = Color(0xFFE11D48)         // Vibrant crimson
val EmergencyRedDark = Color(0xFFBE123C)     // Deep crimson for gradients
val EmergencyRedGlow = Color(0x33E11D48)

// Green: Safe Status / ESP32 Connected / 112 Help / Active
val SafeGreen = Color(0xFF10B981)            // Emerald green
val SafeGreenDark = Color(0xFF047857)

// Amber: Warning / Elevated Risk / Hotspot Caution
val WarningAmber = Color(0xFFF59E0B)         // Alert Amber
val WarningAmberDark = Color(0xFFB45309)

// Blue: Bluetooth / Telemetry / GPS / Neutral Actions
val AccentBlue = Color(0xFF2563EB)           // Solid Royal Blue
val InfoCyan = Color(0xFF06B6D4)

// ==========================================
// 3. DYNAMIC THEME DATA CLASS
// ==========================================

data class RakshaXThemeColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val divider: Color,
    val emergencyRed: Color = EmergencyRed,
    val emergencyRedDark: Color = EmergencyRedDark,
    val safeGreen: Color = SafeGreen,
    val warningAmber: Color = WarningAmber,
    val accentBlue: Color = AccentBlue,
    val isDark: Boolean
)

val DarkRakshaXColors = RakshaXThemeColors(
    background = SolidDarkBackground,
    surface = SolidDarkSurface,
    surfaceElevated = SolidDarkSurfaceElevated,
    surfaceBorder = SolidDarkSurfaceBorder,
    textPrimary = SolidDarkTextPrimary,
    textSecondary = SolidDarkTextSecondary,
    textMuted = SolidDarkTextMuted,
    divider = SolidDarkDivider,
    isDark = true
)

val LightRakshaXColors = RakshaXThemeColors(
    background = SolidLightBackground,
    surface = SolidLightSurface,
    surfaceElevated = SolidLightSurfaceElevated,
    surfaceBorder = SolidLightSurfaceBorder,
    textPrimary = SolidLightTextPrimary,
    textSecondary = SolidLightTextSecondary,
    textMuted = SolidLightTextMuted,
    divider = SolidLightDivider,
    isDark = false
)

val LocalRakshaXColors = staticCompositionLocalOf { DarkRakshaXColors }

// ==========================================
// 4. COMPOSABLE ACCESSORS
// (Seamless backward-compatibility with entire app)
// ==========================================

val BackgroundDark: Color
    @Composable get() = LocalRakshaXColors.current.background

val SurfaceDark: Color
    @Composable get() = LocalRakshaXColors.current.surface

val SurfaceElevated: Color
    @Composable get() = LocalRakshaXColors.current.surfaceElevated

val SurfaceBorder: Color
    @Composable get() = LocalRakshaXColors.current.surfaceBorder

val TextPrimary: Color
    @Composable get() = LocalRakshaXColors.current.textPrimary

val TextSecondary: Color
    @Composable get() = LocalRakshaXColors.current.textSecondary

val TextMuted: Color
    @Composable get() = LocalRakshaXColors.current.textMuted

val DividerColor: Color
    @Composable get() = LocalRakshaXColors.current.divider
