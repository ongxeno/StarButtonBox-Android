package com.ongxeno.android.starbuttonbox.ui.theme

import androidx.compose.ui.graphics.Color

// Primary orange accent color for the dark theme
val OrangeDarkPrimary = Color(0xFFFF8C00) // A vibrant dark orange

// Secondary and Tertiary colors for supporting elements (can be shades of gray or muted colors)
val GreyDarkSecondary = Color(0xFF616161) // A medium-dark gray
val GreyDarkTertiary = Color(0xFF424242)  // A darker gray, or another accent

// Base colors for the dark theme
val DarkBackground = Color(0xFF121212)      // Standard very dark background
val DarkSurface = Color(0xFF1E1E1E)         // Slightly lighter surface for cards, dialogs
val DarkOnError = Color(0xFFCF6679)         // Standard Material dark error color

// "On" colors for text and icons, ensuring contrast
val OnDarkPrimary = Color.Black             // Text/icons on OrangeDarkPrimary
val OnDarkSecondary = Color.White           // Text/icons on GreyDarkSecondary
val OnDarkTertiary = Color.White            // Text/icons on GreyDarkTertiary
val OnDarkBackground = Color(0xFFE1E1E1)    // Light grey text/icons on DarkBackground
val OnDarkSurface = Color(0xFFE1E1E1)       // Light grey text/icons on DarkSurface
val OnDarkError = Color.Black               // Text/icons on DarkOnError

// Specific color for action buttons, now aligned with the orange accent
val ActionButtonColor = OrangeDarkPrimary   // Changed from dark red to the primary orange accent

// You can define other semantic colors if needed, e.g.:
// val WarningColor = Color(0xFFFFA000) // Amber for warnings
// val SuccessColor = Color(0xFF388E3C) // Green for success indicators
