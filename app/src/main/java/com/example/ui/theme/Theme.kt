package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreenDark,
    secondary = SecondaryGoldDark,
    tertiary = SecondaryGoldDark,
    background = BackgroundSoftDark,
    surface = BackgroundSoftDark,
    surfaceVariant = CardBackgroundDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    error = ErrorColor
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreenLight,
    secondary = SecondaryGoldLight,
    tertiary = SecondaryGoldLight,
    background = BackgroundSoftLight,
    surface = BackgroundSoftLight,
    surfaceVariant = CardBackgroundLight,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    error = ErrorColor
)

object ThemeManager {
    fun shouldPlayDarkTheme(themeMode: String, isSystemInDark: Boolean): Boolean {
        return when (themeMode) {
            "LIGHT" -> false
            "DARK" -> true
            else -> isSystemInDark
        }
    }
}

@Composable
fun MyApplicationTheme(
    themeMode: String = "LIGHT",
    content: @Composable () -> Unit
) {
    val darkTheme = ThemeManager.shouldPlayDarkTheme(themeMode, isSystemInDarkTheme())
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
