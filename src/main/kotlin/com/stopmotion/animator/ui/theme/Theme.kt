package com.stopmotion.animator.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.stopmotion.animator.model.AppThemePreset

data class StudioPalette(
    val dark: Color,
    val surface: Color,
    val card: Color,
    val border: Color,
    val primary: Color,
    val primaryHover: Color,
    val secondary: Color,
    val accent: Color,
    val warning: Color,
    val error: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val onionSkinPast: Color,
    val onionSkinFuture: Color
)

object StudioPalettes {
    val ObsidianIndigo = StudioPalette(
        dark = Color(0xFF0D0F14),
        surface = Color(0xFF151821),
        card = Color(0xFF1E2330),
        border = Color(0xFF2A3142),
        primary = Color(0xFF6366F1),
        primaryHover = Color(0xFF818CF8),
        secondary = Color(0xFF06B6D4),
        accent = Color(0xFF10B981),
        warning = Color(0xFFF59E0B),
        error = Color(0xFFEF4444),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF64748B),
        onionSkinPast = Color(0xFFF43F5E),
        onionSkinFuture = Color(0xFF10B981)
    )

    val CyberCyan = StudioPalette(
        dark = Color(0xFF07090E),
        surface = Color(0xFF0D121B),
        card = Color(0xFF131D2B),
        border = Color(0xFF1E2F46),
        primary = Color(0xFF00E5FF),
        primaryHover = Color(0xFF38EFFF),
        secondary = Color(0xFFFF007A),
        accent = Color(0xFF00F5A0),
        warning = Color(0xFFFFB800),
        error = Color(0xFFFF2A6D),
        textPrimary = Color(0xFFF1F7FF),
        textSecondary = Color(0xFF7F9BBB),
        textMuted = Color(0xFF48607E),
        onionSkinPast = Color(0xFFFF007A),
        onionSkinFuture = Color(0xFF00F5A0)
    )

    val SunsetAmber = StudioPalette(
        dark = Color(0xFF110E0B),
        surface = Color(0xFF1A1612),
        card = Color(0xFF251F19),
        border = Color(0xFF3A3127),
        primary = Color(0xFFF59E0B),
        primaryHover = Color(0xFFFBBF24),
        secondary = Color(0xFFFB923C),
        accent = Color(0xFF10B981),
        warning = Color(0xFFF97316),
        error = Color(0xFFEF4444),
        textPrimary = Color(0xFFFEF3C7),
        textSecondary = Color(0xFFB4A596),
        textMuted = Color(0xFF786B5E),
        onionSkinPast = Color(0xFFEF4444),
        onionSkinFuture = Color(0xFF10B981)
    )

    val EmeraldStudio = StudioPalette(
        dark = Color(0xFF09100D),
        surface = Color(0xFF0E1A15),
        card = Color(0xFF162720),
        border = Color(0xFF223C32),
        primary = Color(0xFF10B981),
        primaryHover = Color(0xFF34D399),
        secondary = Color(0xFF06B6D4),
        accent = Color(0xFF22C55E),
        warning = Color(0xFFF59E0B),
        error = Color(0xFFEF4444),
        textPrimary = Color(0xFFECFDF5),
        textSecondary = Color(0xFF86A697),
        textMuted = Color(0xFF4E6B5D),
        onionSkinPast = Color(0xFFF43F5E),
        onionSkinFuture = Color(0xFF34D399)
    )

    val NeonRose = StudioPalette(
        dark = Color(0xFF110B14),
        surface = Color(0xFF1A111F),
        card = Color(0xFF25182D),
        border = Color(0xFF3B2747),
        primary = Color(0xFFF43F5E),
        primaryHover = Color(0xFFFB7185),
        secondary = Color(0xFFA855F7),
        accent = Color(0xFF06B6D4),
        warning = Color(0xFFF59E0B),
        error = Color(0xFFFF2A6D),
        textPrimary = Color(0xFFFFF1F2),
        textSecondary = Color(0xFFA693B1),
        textMuted = Color(0xFF6A5774),
        onionSkinPast = Color(0xFFFB7185),
        onionSkinFuture = Color(0xFF38BDF8)
    )

    val MonochromeSlate = StudioPalette(
        dark = Color(0xFF111215),
        surface = Color(0xFF181A1F),
        card = Color(0xFF23262D),
        border = Color(0xFF333842),
        primary = Color(0xFFE2E8F0),
        primaryHover = Color(0xFFFFFFFF),
        secondary = Color(0xFF94A3B8),
        accent = Color(0xFF38BDF8),
        warning = Color(0xFFF59E0B),
        error = Color(0xFFEF4444),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFF94A3B8),
        textMuted = Color(0xFF64748B),
        onionSkinPast = Color(0xFFF43F5E),
        onionSkinFuture = Color(0xFF38BDF8)
    )

    fun get(preset: AppThemePreset): StudioPalette = when (preset) {
        AppThemePreset.OBSIDIAN_INDIGO -> ObsidianIndigo
        AppThemePreset.CYBER_CYAN -> CyberCyan
        AppThemePreset.SUNSET_AMBER -> SunsetAmber
        AppThemePreset.EMERALD_STUDIO -> EmeraldStudio
        AppThemePreset.NEON_ROSE -> NeonRose
        AppThemePreset.MONOCHROME_SLATE -> MonochromeSlate
    }
}

// Global observable theme palette state
var CurrentStudioPalette: StudioPalette by mutableStateOf(StudioPalettes.ObsidianIndigo)

// Direct dynamic theme color accessors for composables & draw scopes
val StudioDark: Color get() = CurrentStudioPalette.dark
val StudioSurface: Color get() = CurrentStudioPalette.surface
val StudioCard: Color get() = CurrentStudioPalette.card
val StudioBorder: Color get() = CurrentStudioPalette.border

val StudioPrimary: Color get() = CurrentStudioPalette.primary
val StudioPrimaryHover: Color get() = CurrentStudioPalette.primaryHover
val StudioSecondary: Color get() = CurrentStudioPalette.secondary
val StudioAccent: Color get() = CurrentStudioPalette.accent
val StudioWarning: Color get() = CurrentStudioPalette.warning
val StudioError: Color get() = CurrentStudioPalette.error

val StudioTextPrimary: Color get() = CurrentStudioPalette.textPrimary
val StudioTextSecondary: Color get() = CurrentStudioPalette.textSecondary
val StudioTextMuted: Color get() = CurrentStudioPalette.textMuted

val OnionSkinPastColor: Color get() = CurrentStudioPalette.onionSkinPast
val OnionSkinFutureColor: Color get() = CurrentStudioPalette.onionSkinFuture

@Composable
fun StopMotionTheme(
    preset: AppThemePreset = AppThemePreset.OBSIDIAN_INDIGO,
    content: @Composable () -> Unit
) {
    CurrentStudioPalette = StudioPalettes.get(preset)
    val palette = CurrentStudioPalette

    val colorScheme = darkColorScheme(
        primary = palette.primary,
        onPrimary = if (preset == AppThemePreset.MONOCHROME_SLATE) Color.Black else Color.White,
        primaryContainer = palette.card,
        onPrimaryContainer = palette.textPrimary,
        secondary = palette.secondary,
        onSecondary = Color.Black,
        surface = palette.surface,
        onSurface = palette.textPrimary,
        background = palette.dark,
        onBackground = palette.textPrimary,
        surfaceVariant = palette.card,
        onSurfaceVariant = palette.textSecondary,
        outline = palette.border,
        error = palette.error,
        onError = Color.White
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
