package com.example.calyx.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Light colors ──────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary          = Color(0xFF1B5E20),
    onPrimary        = Color.White,
    primaryContainer = Color(0xFFA5D6A7),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary        = Color(0xFF388E3C),
    onSecondary      = Color.White,
    background       = Color(0xFFF0F4E8),
    onBackground     = Color(0xFF1A1A1A),
    surface          = Color.White,
    onSurface        = Color(0xFF1A1A1A),
    surfaceVariant   = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF616161),
    outline          = Color(0xFFBDBDBD),
    error            = Color(0xFFE53935),
    onError          = Color.White
)

// ── Dark colors ───────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary          = Color(0xFF81C784),
    onPrimary        = Color(0xFF003909),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary        = Color(0xFF66BB6A),
    onSecondary      = Color(0xFF003909),
    background       = Color(0xFF121212),
    onBackground     = Color(0xFFE8F5E9),
    surface          = Color(0xFF1E1E1E),
    onSurface        = Color(0xFFE8F5E9),
    surfaceVariant   = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0BEC5),
    outline          = Color(0xFF424242),
    error            = Color(0xFFEF9A9A),
    onError          = Color(0xFF7F0000)
)

@Composable
fun CalyxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content:   @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content     = content
    )
}