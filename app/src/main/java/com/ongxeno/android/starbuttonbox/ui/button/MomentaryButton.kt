package com.ongxeno.android.starbuttonbox.ui.button

import android.os.Build
import android.os.VibrationEffect
import androidx.annotation.RawRes
import androidx.compose.animation.core.animateFloatAsState // Import animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
// import androidx.compose.foundation.layout.fillMaxSize // No longer needed for inner box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer // Import graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned // Import onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp // Import Dp
import androidx.compose.ui.unit.IntSize // Import IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ongxeno.android.starbuttonbox.R
import com.ongxeno.android.starbuttonbox.utils.LocalSoundPlayer
import com.ongxeno.android.starbuttonbox.utils.LocalVibrator
import kotlin.math.max // Import max

// Define the amount the button should appear to shrink (total reduction)
private val pressShrinkDp = 8.dp
private const val minScaleFactor = 0.5f // Prevent scaling down too much

/**
 * A button that triggers actions, sounds, and vibration while pressed and on release.
 * This overload manages its own pressed state internally for convenience.
 * Press effect is a dynamic scale based on fixed dp reduction.
 *
 * Uses LocalVibrator and LocalSoundPlayer.
 *
 * @param modifier Modifier for layout customization (applied to the outer touch area).
 * @param text Text displayed on the button.
 * @param enabled Controls if the button is interactive.
 * @param shape Shape of the button's background/clip area.
 * @param colors ButtonColors defining container and content colors for different states.
 * @param contentPadding Base padding applied *inside* the button's visual area, around the text.
 * @param pressSoundResId Raw resource ID for the sound to play on press.
 * @param releaseSoundResId Raw resource ID for the sound to play on release.
 * @param onPress Lambda executed when the button is physically pressed down (if enabled).
 * @param onRelease Lambda executed when the button is physically released or interaction cancelled (if enabled).
 * @param vibrateOnPress Enable vibration on press (if enabled).
 * @param vibrationDurationMs Duration of the vibration in milliseconds.
 * @param vibrationAmplitude Intensity of the vibration (1-255, requires API 26+). Use -1 for default amplitude.
 */
@Composable
fun MomentaryButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    shape: Shape = RectangleShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp), // Default padding
    @RawRes pressSoundResId: Int = R.raw.snes_press,
    @RawRes releaseSoundResId: Int = R.raw.snes_release,
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {},
    vibrateOnPress: Boolean = true,
    vibrationDurationMs: Long = 40,
    vibrationAmplitude: Int = -1
) {
    var isPressed by remember { mutableStateOf(false) }

    // Call the internal implementation, passing the state and callbacks
    MomentaryButton(
        modifier = modifier,
        text = text,
        isPressed = isPressed, // Pass the internal state
        onIsPressedChange = { pressed -> isPressed = pressed }, // Pass the state setter
        enabled = enabled,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        pressSoundResId = pressSoundResId,
        releaseSoundResId = releaseSoundResId,
        onPress = onPress,
        onRelease = onRelease,
        vibrateOnPress = vibrateOnPress,
        vibrationDurationMs = vibrationDurationMs,
        vibrationAmplitude = vibrationAmplitude
    )
}

/**
 * Base implementation of MomentaryButton with dynamic scale shrink effect.
 * This overload requires the caller to provide and manage the `isPressed` state
 * via the `isPressed` parameter and `onIsPressedChange` callback.
 *
 * @param modifier Modifier for layout customization.
 * @param text Text displayed on the button.
 * @param enabled Controls if the button is interactive.
 * @param isPressed Whether the button is currently in the pressed state (controlled externally).
 * @param onIsPressedChange Callback invoked when the button's pressed state should change due to user interaction.
 * @param shape Shape of the button's background/clip area.
 * @param colors ButtonColors defining container and content colors for different states.
 * @param contentPadding Base padding applied *inside* the button's visual area, around the text.
 * @param pressSoundResId Raw resource ID for the sound to play on press.
 * @param releaseSoundResId Raw resource ID for the sound to play on release.
 * @param onPress Lambda executed when the button is physically pressed down (if enabled).
 * @param onRelease Lambda executed when the button is physically released or interaction cancelled (if enabled).
 * @param vibrateOnPress Enable vibration on press (if enabled).
 * @param vibrationDurationMs Duration of the vibration in milliseconds.
 * @param vibrationAmplitude Intensity of the vibration (1-255, requires API 26+). Use -1 for default amplitude.
 */
@Composable
fun MomentaryButton( // Removed 'private' keyword
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    isPressed: Boolean = false, // State provided by caller
    onIsPressedChange: (Boolean) -> Unit, // Callback provided by caller
    shape: Shape = RectangleShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp), // Default padding
    @RawRes pressSoundResId: Int = R.raw.snes_press,
    @RawRes releaseSoundResId: Int = R.raw.snes_release,
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {},
    vibrateOnPress: Boolean = true,
    vibrationDurationMs: Long = 40,
    vibrationAmplitude: Int = -1
) {
    val soundPlayer = LocalSoundPlayer.current
    LaunchedEffect(soundPlayer, pressSoundResId, releaseSoundResId, enabled) {
        if (enabled) {
            soundPlayer?.loadSound(pressSoundResId)
            soundPlayer?.loadSound(releaseSoundResId)
        }
    }

    // State to hold the measured size of the button in pixels
    var measuredSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    // Calculate the target scale factor based on measured size
    val targetScale = remember(measuredSize, pressShrinkDp) {
        if (measuredSize == IntSize.Zero) {
            1.0f // Default scale if size is not measured yet
        } else {
            val widthPx = measuredSize.width.toFloat()
            val heightPx = measuredSize.height.toFloat()
            val reductionPx = with(density) { pressShrinkDp.toPx() }

            val largerDimensionPx = max(widthPx, heightPx)

            if (largerDimensionPx <= reductionPx) {
                minScaleFactor // Avoid scaling up or to zero/negative if reduction is too large
            } else {
                // Calculate scale: (size - reduction) / size
                ((largerDimensionPx - reductionPx) / largerDimensionPx)
                    .coerceIn(minScaleFactor, 1.0f) // Ensure scale is within bounds
            }
        }
    }

    // Animate the scale factor
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1.0f,
        animationSpec = if (isPressed) snap() else tween(durationMillis = 100),
        label = "ButtonDynamicScale"
    )

    // Determine current colors based on state
    val currentBackgroundColor = when {
        !enabled -> colors.disabledContainerColor
        isPressed -> colors.containerColor.copy(alpha = 0.8f)
        else -> colors.containerColor
    }
    val currentContentColor = when {
        !enabled -> colors.disabledContentColor
        isPressed -> colors.contentColor.copy(alpha = 0.8f)
        else -> colors.contentColor
    }

    val vibrator = LocalVibrator.current
    val onVibrate = {
        if (vibrateOnPress && enabled && vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val clampedAmplitude = when (vibrationAmplitude) {
                    -1 -> VibrationEffect.DEFAULT_AMPLITUDE
                    else -> vibrationAmplitude.coerceIn(1, 255)
                }
                try {
                    val effect = VibrationEffect.createOneShot(vibrationDurationMs, clampedAmplitude)
                    vibrator.vibrate(effect)
                } catch (e: IllegalArgumentException) {
                    println("Vibration failed: ${e.message}")
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(vibrationDurationMs) // Fallback
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(vibrationDurationMs)
            }
        }
    }

    // Box handles size, touch input, scaling, clipping, and background
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier // Apply external modifiers (like size from FreeFormLayout) here
            .onGloballyPositioned { layoutCoordinates ->
                // Update measured size when layout changes
                measuredSize = layoutCoordinates.size
            }
            .graphicsLayer { // Apply scaling
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(shape)
            .background(currentBackgroundColor)
            .pointerInput(enabled) { // Pointer input detects press/release
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        onIsPressedChange(true) // Use callback
                        onVibrate()
                        onPress()
                        soundPlayer?.playSound(pressSoundResId)

                        waitForUpOrCancellation()
                        onIsPressedChange(false) // Use callback
                        onRelease()
                        soundPlayer?.playSound(releaseSoundResId)
                    }
                }
            }
            .padding(contentPadding) // Apply base content padding *inside* the background/clip
    ) {
        Text(
            text = text,
            color = currentContentColor,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
