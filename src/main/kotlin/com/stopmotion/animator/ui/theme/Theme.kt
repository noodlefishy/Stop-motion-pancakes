package com.stopmotion.animator.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val StudioDark = Color(0xFF141619)
val StudioSurface = Color(0xFF1E2126)
val StudioCard = Color(0xFF262A30)
val StudioBorder = Color(0xFF333842)
val StudioPrimary = Color(0xFFFF5E36) // Energetic stop-motion orange/amber
val StudioPrimaryHover = Color(0xFFFF7A59)
val StudioSecondary = Color(0xFF38BDF8) // Cyan accent
val StudioAccent = Color(0xFF10B981) // Emerald / green for capture & success
val StudioWarning = Color(0xFFF59E0B)
val StudioTextPrimary = Color(0xFFF1F5F9)
val StudioTextSecondary = Color(0xFF94A3B8)
val StudioTextMuted = Color(0xFF64748B)

val OnionSkinPastColor = Color(0xFFFF3366) // Neon red/pink for past frames
val OnionSkinFutureColor = Color(0xFF00E676) // Neon green for future frames

val StudioColorScheme: ColorScheme = darkColorScheme(
    primary = StudioPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4D1C10),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = StudioSecondary,
    onSecondary = Color.Black,
    surface = StudioSurface,
    onSurface = StudioTextPrimary,
    background = StudioDark,
    onBackground = StudioTextPrimary,
    surfaceVariant = StudioCard,
    onSurfaceVariant = StudioTextSecondary,
    outline = StudioBorder,
    error = Color(0xFFEF4444),
    onError = Color.White
)

@Composable
fun StopMotionTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StudioColorScheme,
        content = content
    )
}
