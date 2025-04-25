/*
 * File: StarButtonBox/app/src/main/java/com/ongxeno/android/starbuttonbox/ui/button/MomentaryButton.kt
 * Updated to inject and use FeedbackViewModel directly via hiltViewModel().
 * Removed onPlaySound and onVibrate callbacks.
 */
package com.ongxeno.android.starbuttonbox.ui.button

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
import androidx.hilt.navigation.compose.hiltViewModel // Import hiltViewModel
import com.ongxeno.android.starbuttonbox.R // Keep R for default sound IDs
import com.ongxeno.android.starbuttonbox.ui.FeedbackViewModel // Import FeedbackViewModel
import kotlin.math.min

// Define the amount the button should appear to shrink (total reduction in Dp)
private val pressShrinkDp = 4.dp
private const val minScaleFactor = 0.8f // Minimum scale factor

/**
 * MomentaryButton overload that manages its own pressed state internally.
 * Uses FeedbackViewModel for sound and vibration.
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
 * @param feedbackViewModel Injected instance of FeedbackViewModel.
 */
@Composable
fun MomentaryButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {},
    // Removed onPlaySound, onVibrate parameters
    shape: Shape = RectangleShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    textSize: TextUnit = TextUnit.Unspecified,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    @RawRes pressSoundResId: Int = R.raw.snes_press, // Default sound
    @RawRes releaseSoundResId: Int = R.raw.snes_release, // Default sound
    vibrateOnPress: Boolean = true,
    vibrationDurationMs: Long = 40, // Default duration
    vibrationAmplitude: Int = -1, // Default amplitude
    feedbackViewModel: FeedbackViewModel = hiltViewModel() // Inject FeedbackViewModel
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
        textSize = textSize,
        contentPadding = contentPadding,
        pressSoundResId = pressSoundResId,
        releaseSoundResId = releaseSoundResId,
        onPress = onPress,
        onRelease = onRelease,
        feedbackViewModel = feedbackViewModel, // Pass ViewModel instance
        vibrateOnPress = vibrateOnPress,
        vibrationDurationMs = vibrationDurationMs,
        vibrationAmplitude = vibrationAmplitude
    )
}


/**
 * Base implementation of MomentaryButton with external state management.
 * Renamed to MomentaryButtonBase to avoid conflict.
 * Uses FeedbackViewModel for sound and vibration.
 *
 * @param modifier Modifier for layout customization.
 * @param text Text displayed on the button.
 * @param enabled Controls if the button is interactive.
 * @param isPressed Whether the button is currently in the pressed state (controlled externally).
 * @param onIsPressedChange Callback invoked when the button's pressed state should change.
 * @param onPress Lambda executed when the button is physically pressed down.
 * @param onRelease Lambda executed when the button is physically released or interaction cancelled.
 * @param feedbackViewModel Injected instance of FeedbackViewModel.
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
    feedbackViewModel: FeedbackViewModel, // Receive FeedbackViewModel instance
    // Removed onPlaySound, onVibrate parameters
    shape: Shape = RectangleShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    textSize: TextUnit = TextUnit.Unspecified,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    @RawRes pressSoundResId: Int = R.raw.snes_press,
    @RawRes releaseSoundResId: Int = R.raw.snes_release,
    vibrateOnPress: Boolean = true,
    vibrationDurationMs: Long = 40,
    vibrationAmplitude: Int = -1
) {

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
                        if (vibrateOnPress) { // Check flag before calling
                            feedbackViewModel.vibrate(vibrationDurationMs, vibrationAmplitude) // Call ViewModel
                        }
                        onPress() // Call passed onPress
                        feedbackViewModel.playSound(pressSoundResId) // Call ViewModel

                        waitForUpOrCancellation()
                        onIsPressedChange(false) // Use callback
                        onRelease() // Call passed onRelease
                        feedbackViewModel.playSound(releaseSoundResId) // Call ViewModel
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
