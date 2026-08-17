package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NavCyan,
    onPrimary = NavDarkNavy,
    primaryContainer = NavDeepBlue,
    onPrimaryContainer = Color.White,
    secondary = NavAccentAmber,
    onSecondary = Color.Black,
    secondaryContainer = NavSurfaceBlue,
    onSecondaryContainer = Color.White,
    tertiary = NavEmerald,
    onTertiary = Color.Black,
    background = NavDarkNavy,
    onBackground = TextPrimaryDark,
    surface = NavDeepBlue,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavSurfaceBlue,
    onSurfaceVariant = TextSecondaryDark,
    outline = NavBorderBlue,
    error = NavCoralRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = NavBrightBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = NavAccentAmber,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = NavEmerald,
    onTertiary = Color.White,
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = NavCoralRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme // Navigation apps thrive in high-contrast dark theme by default
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
