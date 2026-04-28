package com.example.foodtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AppBackground = Color(0xFF0D0D0D)
val AppSurface = Color(0xFF1A1A2E)
val AppSurfaceAlt = Color(0xFF23233C)
val AppPrimary = Color(0xFF00D4AA)
val AppSecondary = Color(0xFF4ECDC4)
val AppTertiary = Color(0xFFFF6B6B)
val AppTextPrimary = Color(0xFFFFFFFF)
val AppTextSecondary = Color(0xFFA0A0B0)

private val DarkColors = darkColorScheme(
    primary = AppPrimary,
    onPrimary = Color(0xFF002A21),
    primaryContainer = Color(0xFF004638),
    onPrimaryContainer = Color(0xFF87FFE2),
    secondary = AppSecondary,
    onSecondary = Color(0xFF073737),
    tertiary = AppTertiary,
    onTertiary = Color(0xFF3D1010),
    background = AppBackground,
    onBackground = AppTextPrimary,
    surface = AppSurface,
    onSurface = AppTextPrimary,
    surfaceVariant = AppSurfaceAlt,
    onSurfaceVariant = AppTextSecondary,
    error = Color(0xFFFF5252),
    onError = Color.White
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
