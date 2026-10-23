package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF10B981),      // Emerald
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF3B82F6),    // Blue
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFF59E0B),     // Amber
    background = Color(0xFF0F172A),   // Slate 900
    surface = Color(0xFF1E293B),      // Slate 800
    onBackground = Color(0xFFF1F5F9), // Slate 100
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155), // Slate 700
    onSurfaceVariant = Color(0xFFCBD5E1) // Slate 300
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF059669),      // Deep Emerald
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF2563EB),    // Electric Blue
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFD97706),     // Warm Amber
    background = Color(0xFFF8FAFC),   // Slate 50
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0F172A), // Slate 900
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0), // Slate 200
    onSurfaceVariant = Color(0xFF475569) // Slate 600
)

@Composable
fun AttendanceWageTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
