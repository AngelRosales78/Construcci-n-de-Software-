package com.example.sp500auth.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF45F99C),
    onPrimary = Color(0xFF00391D),
    primaryContainer = Color(0xFF00DC82),
    onPrimaryContainer = Color(0xFF005B32),
    secondary = Color(0xFFBDF4FF),
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF00E3FD),
    onSecondaryContainer = Color(0xFF00616D),
    tertiary = Color(0xFFCEDEF4),
    onTertiary = Color(0xFF233143),
    background = Color(0xFF111319),
    onBackground = Color(0xFFE1E2EA),
    surface = Color(0xFF111319),
    onSurface = Color(0xFFE1E2EA),
    surfaceVariant = Color(0xFF32353B),
    onSurfaceVariant = Color(0xFFBACBBC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF859587),
    surfaceContainer = Color(0xFF1D2025),
    surfaceContainerHigh = Color(0xFF272A30),
    surfaceContainerHighest = Color(0xFF32353B)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    secondary = Color(0xFF1976D2),
    tertiary = Color(0xFF42A5F5),
    background = Color(0xFFF8F9FF),
    surface = Color(0xFFF8F9FF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF111319),
    onSurface = Color(0xFF111319)
)

@Composable
fun SP500AuthTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
