package com.stopmotion.animator.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Modern Pro Studio Color Palette (Obsidian & Electric Indigo)
val StudioDark = Color(0xFF0D0F14)       // Deep obsidian canvas
val StudioSurface = Color(0xFF151821)    // Refined deep slate panels
val StudioCard = Color(0xFF1E2330)       // Elevated cards & controls
val StudioBorder = Color(0xFF2A3142)     // Crisp subtle borders

val StudioPrimary = Color(0xFF6366F1)    // Modern Electric Indigo / Violet
val StudioPrimaryHover = Color(0xFF818CF8)
val StudioSecondary = Color(0xFF06B6D4)  // Electric Cyan
val StudioAccent = Color(0xFF10B981)     // Crisp Emerald for capture & success
val StudioWarning = Color(0xFFF59E0B)    // Warm Amber
val StudioError = Color(0xFFEF4444)      // Rose Red

val StudioTextPrimary = Color(0xFFF8FAFC)
val StudioTextSecondary = Color(0xFF94A3B8)
val StudioTextMuted = Color(0xFF64748B)

val OnionSkinPastColor = Color(0xFFF43F5E)   // Neon Rose for past frames
val OnionSkinFutureColor = Color(0xFF10B981) // Neon Mint for future frames

val StudioColorScheme: ColorScheme = darkColorScheme(
    primary = StudioPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E335B),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = StudioSecondary,
    onSecondary = Color.Black,
    surface = StudioSurface,
    onSurface = StudioTextPrimary,
    background = StudioDark,
    onBackground = StudioTextPrimary,
    surfaceVariant = StudioCard,
    onSurfaceVariant = StudioTextSecondary,
    outline = StudioBorder,
    error = StudioError,
    onError = Color.White
)

@Composable
fun StopMotionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StudioColorScheme,
        content = content
    )
}
