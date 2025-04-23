/*
 * File: StarButtonBox/app/src/main/java/com/ongxeno/android/starbuttonbox/ui/dialog/ColorPickerDialog.kt
 * Custom Color Picker Dialog Composable.
 * Updated color wheel to show saturation gradient based on current lightness.
 */
package com.ongxeno.android.starbuttonbox.ui.dialog

import androidx.compose.foundation.Canvas // Import Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.* // Import graphics classes
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged // Import onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration // Import LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ongxeno.android.starbuttonbox.utils.ColorUtils.toHexString // Import hex converter
import kotlin.math.*

// Helper data class to hold HSL values (alpha is fixed to 1.0)
private data class HslColor(
    val hue: Float = 0f,       // 0-360
    val saturation: Float = 1f, // 0-1
    val lightness: Float = 0.5f // 0-1
) {
    fun toComposeColor(): Color {
        val androidHsl = floatArrayOf(hue, saturation, lightness)
        val colorInt = androidx.core.graphics.ColorUtils.HSLToColor(androidHsl)
        return Color(colorInt).copy(alpha = 1f)
    }

    companion object {
        fun fromComposeColor(color: Color): HslColor {
            val colorInt = color.toArgb()
            val hsl = FloatArray(3)
            androidx.core.graphics.ColorUtils.colorToHSL(colorInt, hsl)
            return HslColor(
                hue = hsl[0],
                saturation = hsl[1],
                lightness = hsl[2]
            )
        }
        // Helper to get the gray color for a specific lightness
        fun lightnessToGray(lightness: Float): Color {
            val androidHsl = floatArrayOf(0f, 0f, lightness.coerceIn(0f, 1f)) // Hue and Saturation are 0 for gray
            val colorInt = androidx.core.graphics.ColorUtils.HSLToColor(androidHsl)
            return Color(colorInt).copy(alpha = 1f)
        }
    }
}


@Composable
fun CustomColorPickerDialog(
    initialColor: Color = Color.Red,
    onDismissRequest: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var currentHslColor by remember { mutableStateOf(HslColor.fromComposeColor(initialColor)) }
    val finalSelectedColor = remember(currentHslColor) { currentHslColor.toComposeColor() }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val maxDialogWidth = remember(screenWidth) { (screenWidth * 0.9f).coerceAtMost(400.dp) }


    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(max = maxDialogWidth)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Select Color", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 16.dp))

                // --- Color Wheel ---
                ColorWheel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .padding(bottom = 16.dp),
                    hslColor = currentHslColor, // Pass the full HSL state
                    onColorChanged = { newHue, newSaturation ->
                        // Only update Hue and Saturation from the wheel interaction
                        currentHslColor = currentHslColor.copy(hue = newHue, saturation = newSaturation)
                    }
                )

                // --- Luminance Slider ---
                Text(
                    text = "Luminance (${String.format("%.2f", currentHslColor.lightness)})",
                    style = MaterialTheme.typography.labelMedium
                )
                Slider(
                    value = currentHslColor.lightness,
                    onValueChange = { newLightness ->
                        currentHslColor = currentHslColor.copy(lightness = newLightness)
                    },
                    valueRange = 0f..1f
                )
                Spacer(modifier = Modifier.height(16.dp)) // Increased spacing

                // --- Action Buttons ---
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onColorSelected(finalSelectedColor) }) {
                        Text("Select")
                    }
                }
            }
        }
    }
}

// --- Composable for the Color Wheel ---
@Composable
private fun ColorWheel(
    modifier: Modifier = Modifier,
    hslColor: HslColor, // Receives the full HSL state including lightness
    onColorChanged: (hue: Float, saturation: Float) -> Unit
) {
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val radius = remember(canvasSize) { min(canvasSize.width, canvasSize.height) / 2f }
    val center = remember(canvasSize) { Offset(canvasSize.width / 2f, canvasSize.height / 2f) }

    // Calculate selector position based on HSL state
    val selectorAngle = hslColor.hue * (PI / 180f)
    val selectorRadius = hslColor.saturation * radius
    val selectorX = center.x + cos(selectorAngle).toFloat() * selectorRadius
    val selectorY = center.y + sin(selectorAngle).toFloat() * selectorRadius
    val selectorPosition = Offset(selectorX, selectorY)

    // Define gradients using Brush factory functions
    val hueColors = remember {
        List(361) { i ->
            val hsl = floatArrayOf(i.toFloat(), 1f, 0.5f) // Hue sweep at full saturation, mid lightness
            Color(androidx.core.graphics.ColorUtils.HSLToColor(hsl))
        }
    }
    val sweepBrush = remember(hueColors, center) { Brush.sweepGradient(hueColors, center) }

    val saturationBrush = remember(center, radius) {
        if (radius > 0f) {
            Brush.radialGradient(
                colors = listOf(Color.White, Color.Transparent), // Use calculated gray at center
                center = center,
                radius = radius
            )
        } else {
            SolidColor(Color.Transparent)
        }
    }

    // Selector visual properties
    val selectorOuterRadiusDp = 36.dp
    val selectorInnerRadiusDp = 30.dp

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(radius, center) {
                if (radius <= 0) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        if (radius > 0) {
                            val (hue, sat) = calculateHueSaturation(offset, center, radius)
                            onColorChanged(hue, sat)
                        }
                    },
                    onDrag = { change, _ ->
                        if (radius > 0) {
                            val (hue, sat) = calculateHueSaturation(change.position, center, radius)
                            onColorChanged(hue, sat)
                            change.consume()
                        }
                    }
                )
            }
    ) {
        if (radius > 0f) {
            // Draw the hue sweep gradient first
            drawCircle(brush = sweepBrush, radius = radius, center = center)

            // Draw the saturation radial gradient on top using Multiply blend mode
            // This now correctly blends from the lightness-based gray to the hue
            drawCircle(
                brush = saturationBrush,
                radius = radius,
                center = center,
                blendMode = BlendMode.Luminosity
            )

            // Draw the selector indicator
            val selectorOuterRadiusPx = selectorOuterRadiusDp.toPx()
            val selectorInnerRadiusPx = selectorInnerRadiusDp.toPx()

            // Draw outer border
            drawCircle(
                color = Color.White,
                radius = selectorOuterRadiusPx,
                center = selectorPosition,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            // Draw inner border
            drawCircle(
                color = Color.Black.copy(alpha = 0.5f),
                radius = selectorOuterRadiusPx - 1.dp.toPx(),
                center = selectorPosition,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )

            // Draw inner circle preview
            drawCircle(
                color = hslColor.toComposeColor(), // Use full color including lightness
                radius = selectorInnerRadiusPx,
                center = selectorPosition
            )
        }
    }
}

// Helper function to calculate Hue and Saturation from touch offset (Unchanged)
private fun calculateHueSaturation(offset: Offset, center: Offset, radius: Float): Pair<Float, Float> {
    val dx = offset.x - center.x
    val dy = offset.y - center.y
    val distance = sqrt(dx * dx + dy * dy).coerceAtMost(radius)
    val saturation = (distance / radius).coerceIn(0f, 1f)
    var angle = atan2(dy, dx)
    if (angle < 0) {
        angle += (2 * PI).toFloat()
    }
    val hue = (angle * (180f / PI.toFloat())).coerceIn(0f, 360f)
    return Pair(hue, saturation)
}
