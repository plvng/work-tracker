package com.plvng.worktracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B8DEF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E6FF),
    onPrimaryContainer = Color(0xFF1A3A7A),
    secondary = Color(0xFF7BA4FF),
    background = Color(0xFFF3F6FC),
    surface = Color.White,
    onSurface = Color(0xFF1C2333),
    surfaceVariant = Color(0xFFE8EEF9),
    onSurfaceVariant = Color(0xFF5A6478),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FB4FF),
    onPrimary = Color(0xFF0F2347),
    primaryContainer = Color(0xFF2A4575),
    onPrimaryContainer = Color(0xFFD8E6FF),
    secondary = Color(0xFF9EC0FF),
    background = Color(0xFF121722),
    surface = Color(0xFF1A2130),
    onSurface = Color(0xFFE8EDF8),
    surfaceVariant = Color(0xFF2A3347),
    onSurfaceVariant = Color(0xFFB8C2D9),
)

val BubbleShape = RoundedCornerShape(28.dp)
val BubbleShapeLarge = RoundedCornerShape(36.dp)
val BubbleShapeSmall = RoundedCornerShape(20.dp)

@Composable
fun WorkTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
