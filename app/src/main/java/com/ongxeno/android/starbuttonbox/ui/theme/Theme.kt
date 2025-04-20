package com.ongxeno.android.starbuttonbox.ui.theme // Ensure package matches yours

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Define the updated DarkColorScheme using the new colors from Color.kt
private val AppDarkColorScheme = darkColorScheme(
    primary = OrangeDarkPrimary,
    onPrimary = OnDarkPrimary,
    secondary = GreyDarkSecondary,
    onSecondary = OnDarkSecondary,
    tertiary = GreyDarkTertiary,
    onTertiary = OnDarkTertiary,
    background = DarkBackground,
    onBackground = OnDarkBackground,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    error = DarkOnError, // Use a standard error color
    onError = OnDarkError,
)

@Composable
fun StarButtonBoxTheme(
    content: @Composable () -> Unit
) {
    // Force dark theme for this app
    val colorScheme = AppDarkColorScheme

    // Code for handling status bar color based on theme (can be simplified for dark-only)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assumes Typography.kt exists and is defined
        content = content
    )
}
