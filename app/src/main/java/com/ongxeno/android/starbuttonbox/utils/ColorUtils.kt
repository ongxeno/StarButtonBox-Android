package com.ongxeno.android.starbuttonbox.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils as AndroidColorUtils // Use alias to avoid name clash

object ColorUtils {

    // --- Standard Button Colors ---
    // You can customize these or add more
    val DefaultButtonBackground = Color.DarkGray // Example default
    val StandardColors = listOf(
        "#424242", // Grey 800
        "#616161", // Grey 700 (from theme)
        "#0D47A1", // Blue 900
        "#1B5E20", // Green 900
        "#E65100", // Orange 900
        "#B71C1C", // Red 900
        "#FF8C00", // DarkOrange (from theme)
        "#212121"  // Almost Black
    )

    // --- Contrasting Text Color Logic ---
    /**
     * Calculates whether a light (White) or dark (Black) text color provides
     * better contrast against the given background color.
     *
     * @param backgroundColor The background color.
     * @param contrastThreshold The luminance threshold to switch between light/dark text.
     * Values closer to 0 are darker, closer to 1 are lighter.
     * 0.5 is a common midpoint.
     * @return Color.White or Color.Black.
     */
    fun getContrastingTextColor(backgroundColor: Color, contrastThreshold: Float = 0.4f): Color {
        // Calculate luminance (perceived brightness)
        val luminance = backgroundColor.luminance()

        // Compare luminance against the threshold
        return if (luminance > contrastThreshold) {
            Color.Black // Use dark text on light background
        } else {
            Color.White // Use light text on dark background
        }
    }

    /**
     * Safely parses a hex color string (e.g., "#RRGGBB" or "#AARRGGBB").
     * Returns null if parsing fails.
     */
    fun parseHexColor(hexString: String?): Color? {
        if (hexString.isNullOrBlank()) return null
        return try {
            // Ensure '#' prefix is handled correctly
            val cleanHex = if (hexString.startsWith("#")) hexString else "#$hexString"
            Color(android.graphics.Color.parseColor(cleanHex))
        } catch (e: IllegalArgumentException) {
            println("Error parsing hex color '$hexString': ${e.message}")
            null
        }
    }

    /**
     * Converts a Compose Color to a hex string (e.g., "#RRGGBB").
     * Always returns 6 hex digits (RGB), ignoring alpha.
     */
    fun Color.toHexString(): String {
        // Convert Compose Color to Android Color Int first
        val androidColorInt = this.hashCode() // Note: This might not be the most reliable way long-term
        // Format as 6-digit hex (excluding alpha)
        return String.format("#%06X", (0xFFFFFF and androidColorInt))
    }

    /**
     * Adjusts the saturation of a Compose Color by a percentage factor.
     *
     * @param percentage The factor to multiply the saturation by (e.g., 1.1f for +10%, 0.8f for -20%).
     * @return A new Color object with the adjusted saturation.
     */
    fun Color.adjustSaturation(percentage: Float): Color {
        // Convert Compose Color to Android ColorInt
        val colorInt = this.toArgb()
        // Allocate HSL array (Hue, Saturation, Luminance)
        val hsl = FloatArray(3)
        // Convert ColorInt to HSL using AndroidX ColorUtils
        AndroidColorUtils.colorToHSL(colorInt, hsl)

        // Adjust saturation: hsl[1] is saturation
        // Multiply by percentage and clamp between 0.0 and 1.0
        hsl[1] = (hsl[1] * percentage).coerceIn(0.0f, 1.0f)

        // Convert the modified HSL back to ColorInt
        val adjustedColorInt = AndroidColorUtils.HSLToColor(hsl)
        // Convert ColorInt back to Compose Color
        return Color(adjustedColorInt)
    }

    /**
     * Adjusts the luminance (lightness) of a Compose Color by a percentage factor.
     *
     * @param percentage The factor to multiply the luminance by (e.g., 1.1f for +10%, 0.8f for -20%).
     * @return A new Color object with the adjusted luminance.
     */
    fun Color.adjustLuminance(percentage: Float): Color {
        // Convert Compose Color to Android ColorInt
        val colorInt = this.toArgb()
        // Allocate HSL array
        val hsl = FloatArray(3)
        // Convert ColorInt to HSL
        AndroidColorUtils.colorToHSL(colorInt, hsl)

        // Adjust luminance: hsl[2] is luminance
        // Multiply by percentage and clamp between 0.0 and 1.0
        hsl[2] = (hsl[2] * percentage).coerceIn(0.0f, 1.0f)

        // Convert the modified HSL back to ColorInt
        val adjustedColorInt = AndroidColorUtils.HSLToColor(hsl)
        // Convert ColorInt back to Compose Color
        return Color(adjustedColorInt)
    }
}