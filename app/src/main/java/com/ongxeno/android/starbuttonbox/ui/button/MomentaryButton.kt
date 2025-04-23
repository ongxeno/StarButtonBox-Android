/*
 * File: StarButtonBox/app/src/main/java/com/ongxeno/android/starbuttonbox/ui/button/MomentaryButton.kt
 * Restored the second constructor overload for internal state management,
 * uses ButtonColors, and includes textSize parameter.
 */
package com.ongxeno.android.starbuttonbox.ui.button

import android.os.Build
import android.os.VibrationEffect
import androidx.annotation.RawRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.ongxeno.android.starbuttonbox.R
import com.ongxeno.android.starbuttonbox.utils.LocalSoundPlayer
import com.ongxeno.android.starbuttonbox.utils.LocalVibrator
import kotlin.math.max
import kotlin.math.min

// Define the amount the button should appear to shrink (total reduction in Dp)
private val pressShrinkDp = 4.dp
private const val minScaleFactor = 0.8f // Minimum scale factor

/**
 * MomentaryButton overload that manages its own pressed state internally.
 *
 * @param modifier Modifier for layout customization.
 * @param text Text displayed on the button.
 * @param enabled Controls if the button is interactive.
 * @param onPress Lambda executed when the button is physically pressed down.
 * @param onRelease Lambda executed when the button is physically released or interaction cancelled.
 * @param shape Shape of the button's background/clip area.
 * @param colors ButtonColors defining container and content colors for enabled/disabled states.
 * @param textSize Custom text size for the button label.
 * @param contentPadding Base padding applied *inside* the button's visual area.
 * @param pressSoundResId Raw resource ID for the sound to play on press.
 * @param releaseSoundResId Raw resource ID for the sound to play on release.
 * @param vibrateOnPress Enable vibration on press.
 * @param vibrationDurationMs Duration of the vibration.
 * @param vibrationAmplitude Intensity of the vibration (1-255, API 26+). -1 for default.
 */
@Composable
fun MomentaryButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {},
    shape: Shape = RectangleShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    textSize: TextUnit = TextUnit.Unspecified, // Added textSize
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    @RawRes pressSoundResId: Int = R.raw.snes_press,
    @RawRes releaseSoundResId: Int = R.raw.snes_release,
    vibrateOnPress: Boolean = true,
    vibrationDurationMs: Long = 40,
    vibrationAmplitude: Int = -1
) {
    var isPressed by remember { mutableStateOf(false) }

    // Call the base implementation, passing the internal state and setter
    MomentaryButton(
        modifier = modifier,
        text = text,
        isPressed = isPressed, // Pass the internal state
        onIsPressedChange = { pressed -> isPressed = pressed }, // Pass the state setter
        enabled = enabled,
        shape = shape,
        colors = colors,
        textSize = textSize, // Pass textSize
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
 * Base implementation of MomentaryButton with external state management.
 *
 * @param modifier Modifier for layout customization.
 * @param text Text displayed on the button.
 * @param enabled Controls if the button is interactive.
 * @param isPressed Whether the button is currently in the pressed state (controlled externally).
 * @param onIsPressedChange Callback invoked when the button's pressed state should change.
 * @param onPress Lambda executed when the button is physically pressed down.
 * @param onRelease Lambda executed when the button is physically released or interaction cancelled.
 * @param shape Shape of the button's background/clip area.
 * @param colors ButtonColors defining container and content colors for enabled/disabled states.
 * @param textSize Custom text size for the button label.
 * @param contentPadding Base padding applied *inside* the button's visual area.
 * @param pressSoundResId Raw resource ID for the sound to play on press.
 * @param releaseSoundResId Raw resource ID for the sound to play on release.
 * @param vibrateOnPress Enable vibration on press.
 * @param vibrationDurationMs Duration of the vibration.
 * @param vibrationAmplitude Intensity of the vibration (1-255, API 26+). -1 for default.
 */
@Composable
fun MomentaryButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    isPressed: Boolean, // State provided by caller
    onIsPressedChange: (Boolean) -> Unit, // Callback provided by caller
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {},
    shape: Shape = RectangleShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    textSize: TextUnit = TextUnit.Unspecified, // Added textSize parameter
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    @RawRes pressSoundResId: Int = R.raw.snes_press,
    @RawRes releaseSoundResId: Int = R.raw.snes_release,
    vibrateOnPress: Boolean = true,
    vibrationDurationMs: Long = 40,
    vibrationAmplitude: Int = -1
) {
    // isPressed state is now managed externally via parameters

    val soundPlayer = LocalSoundPlayer.current
    val vibrator = LocalVibrator.current

    LaunchedEffect(soundPlayer, pressSoundResId, releaseSoundResId, enabled) {
        if (enabled && soundPlayer != null) {
            soundPlayer.loadSound(pressSoundResId)
            soundPlayer.loadSound(releaseSoundResId)
        }
    }

    var measuredSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    val targetScale = remember(measuredSize, pressShrinkDp) {
        if (measuredSize == IntSize.Zero) {
            1.0f
        } else {
            val widthPx = measuredSize.width.toFloat()
            val heightPx = measuredSize.height.toFloat()
            val reductionPx = with(density) { pressShrinkDp.toPx() }
            val smallerDimensionPx = min(widthPx, heightPx)
            if (smallerDimensionPx <= reductionPx) {
                minScaleFactor
            } else {
                ((smallerDimensionPx - reductionPx) / smallerDimensionPx)
                    .coerceIn(minScaleFactor, 1.0f)
            }
        }
    }

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) targetScale else 1.0f,
        animationSpec = if (isPressed && enabled) snap() else tween(durationMillis = 100),
        label = "ButtonScaleAnimation"
    )

    val currentContainerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
    val currentContentColor = if (enabled) colors.contentColor else colors.disabledContentColor

    val onVibrate = {
        if (vibrateOnPress && enabled && vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val clampedAmplitude = when (vibrationAmplitude) {
                    -1 -> VibrationEffect.EFFECT_TICK
                    else -> vibrationAmplitude.coerceIn(1, 255)
                }
                try {
                    val effect = VibrationEffect.createOneShot(vibrationDurationMs, clampedAmplitude)
                    vibrator.vibrate(effect)
                } catch (e: IllegalArgumentException) {
                    println("Vibration failed (API 29+): ${e.message}")
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(vibrationDurationMs)
                }
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(vibrationDurationMs)
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { layoutCoordinates ->
                measuredSize = layoutCoordinates.size
            }
            .graphicsLayer {
                scaleX = animatedScale
                scaleY = animatedScale
            }
            .clip(shape)
            .background(currentContainerColor)
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
            .padding(contentPadding)
    ) {
        Text(
            text = text,
            color = currentContentColor,
            textAlign = TextAlign.Center,
            fontSize = if (textSize != TextUnit.Unspecified) textSize else 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
