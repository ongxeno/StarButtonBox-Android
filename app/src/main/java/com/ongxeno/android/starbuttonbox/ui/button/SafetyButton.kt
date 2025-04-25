package com.ongxeno.android.starbuttonbox.ui.button

import android.util.Log
import androidx.annotation.RawRes // Import for sound IDs
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel // Import hiltViewModel
import com.ongxeno.android.starbuttonbox.R // Import R for sound resources
import com.ongxeno.android.starbuttonbox.ui.FeedbackViewModel // Import FeedbackViewModel
import com.ongxeno.android.starbuttonbox.ui.theme.ActionButtonColor
import com.ongxeno.android.starbuttonbox.ui.theme.GreyDarkSecondary
import com.ongxeno.android.starbuttonbox.ui.theme.OnDarkSurface
import com.ongxeno.android.starbuttonbox.ui.theme.OrangeDarkPrimary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
private enum class CoverSlideState {
    CLOSED,
    OPEN
}

/**
 * A button with a sliding safety cover. Requires confirmation before executing the action.
 * Uses FeedbackViewModel for sound and vibration.
 *
 * @param text Text displayed on the action button underneath the cover.
 * @param modifier Modifier for layout customization.
 * @param coverColor Color of the sliding safety cover.
 * @param actionButtonColor Color of the action button revealed under the cover.
 * @param textColor Color of the text on the action button.
 * @param autoCloseDelayMs Delay in milliseconds before the cover automatically closes after opening.
 * @param onSafeClick Lambda executed only when the action button is pressed while the cover is fully open.
 * @param feedbackViewModel Injected instance of FeedbackViewModel.
 * @param coverOpenSoundResId Sound to play when the cover opens.
 * @param coverCloseSoundResId Sound to play when the cover closes.
 * @param actionPressSoundResId Sound to play when the action button is pressed.
 * @param actionReleaseSoundResId Sound to play when the action button is released.
 * @param vibrateOnAction Enable vibration when the action button is pressed.
 * @param vibrationDurationMs Duration of the vibration for the action press.
 * @param vibrationAmplitude Intensity of the vibration for the action press.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SafetyButton(
    text: String,
    modifier: Modifier = Modifier,
    coverColor: Color = OrangeDarkPrimary,
    actionButtonColor: Color = ActionButtonColor,
    textColor: Color = OnDarkSurface,
    autoCloseDelayMs: Long = 5000L,
    onSafeClick: () -> Unit,
    // Removed onPlaySound, onVibrate parameters
    feedbackViewModel: FeedbackViewModel = hiltViewModel(), // Inject FeedbackViewModel
    @RawRes coverOpenSoundResId: Int = R.raw.super8_open, // Default sounds
    @RawRes coverCloseSoundResId: Int = R.raw.super8_close,
    @RawRes actionPressSoundResId: Int = R.raw.snes_press,
    @RawRes actionReleaseSoundResId: Int = R.raw.snes_release,
    vibrateOnAction: Boolean = true, // Controls if feedbackViewModel.vibrate is called
    vibrationDurationMs: Long = 40, // Default duration for action vibration
    vibrationAmplitude: Int = -1 // Default amplitude for action vibration
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var componentHeightPx by remember { mutableFloatStateOf(0f) }

    // Calculate cover height based on component height
    val coverHeightPx by remember(componentHeightPx) {
        derivedStateOf { componentHeightPx / 2f }
    }

    // Define draggable anchors based on cover height
    val anchors = remember(coverHeightPx) {
        if (coverHeightPx > 0f) {
            DraggableAnchors {
                CoverSlideState.CLOSED at 0f
                CoverSlideState.OPEN at -coverHeightPx // Negative offset means sliding UP
            }
        } else {
            // Default anchors if height is not yet known
            DraggableAnchors { CoverSlideState.CLOSED at 0f }
        }
    }

    // State for the draggable cover
    val dragState = remember {
        AnchoredDraggableState(
            initialValue = CoverSlideState.CLOSED,
            anchors = anchors,
            positionalThreshold = { distance: Float -> distance * 0.5f }, // Threshold to snap
            velocityThreshold = { with(density) { 100.dp.toPx() } }, // Velocity threshold
            snapAnimationSpec = tween(), // Animation for snapping
            decayAnimationSpec = exponentialDecay() // Animation for flinging (not typically used here)
        )
    }

    // Update anchors when cover height changes
    LaunchedEffect(anchors) {
        dragState.updateAnchors(anchors)
    }

    var autoCloseTimerJob by remember { mutableStateOf<Job?>(null) }
    var momentaryButtonPressed by remember { mutableStateOf(false) } // State for the underlying momentary button

    // Effect to handle auto-closing the cover
    LaunchedEffect(dragState.currentValue, dragState.isAnimationRunning, momentaryButtonPressed) {
        val isCoverOpenAndSettled = dragState.targetValue == CoverSlideState.OPEN && !dragState.isAnimationRunning
        val shouldStartTimer = isCoverOpenAndSettled && !momentaryButtonPressed

        if (shouldStartTimer && coverHeightPx > 0f) {
            Log.d("SafetyButton", "Starting auto-close timer.")
            autoCloseTimerJob?.cancel() // Cancel previous timer
            autoCloseTimerJob = scope.launch {
                delay(autoCloseDelayMs)
                // Check conditions again after delay, in case state changed
                if (isActive && dragState.targetValue == CoverSlideState.OPEN && !dragState.isAnimationRunning && !momentaryButtonPressed) {
                    Log.d("SafetyButton", "Auto-closing cover.")
                    scope.launch { // Launch animation in a separate scope
                        try {
                            dragState.animateTo(CoverSlideState.CLOSED)
                        } catch (e: CancellationException) {
                            Log.d("SafetyButton", "Auto-close animateTo cancelled.")
                        } catch (e: Exception) {
                            Log.e("SafetyButton", "Error during auto-close animateTo", e)
                        }
                    }
                } else {
                    Log.d("SafetyButton", "Auto-close timer finished, but conditions no longer met.")
                }
            }
        } else {
            Log.d("SafetyButton", "Cancelling auto-close timer (conditions not met or cover height invalid).")
            autoCloseTimerJob?.cancel() // Cancel timer if conditions aren't met
        }
    }

    // Effect to play sounds when cover state changes and reset button state
    LaunchedEffect(dragState) {
        snapshotFlow { dragState.targetValue } // Observe the target value for smoother sound timing
            .distinctUntilChanged()
            .collect { targetState ->
                when(targetState) {
                    CoverSlideState.OPEN -> {
                        Log.d("SafetyButton", "Cover target OPEN.")
                        feedbackViewModel.playSound(coverOpenSoundResId) // Use ViewModel
                    }
                    CoverSlideState.CLOSED -> {
                        Log.d("SafetyButton", "Cover target CLOSED.")
                        feedbackViewModel.playSound(coverCloseSoundResId) // Use ViewModel
                        momentaryButtonPressed = false // Ensure button state resets when cover closes
                        autoCloseTimerJob?.cancel() // Cancel timer when cover starts closing
                    }
                }
            }
    }

    // Ensure timer is cancelled on dispose
    DisposableEffect(Unit) {
        onDispose {
            autoCloseTimerJob?.cancel()
        }
    }

    // Determine if the action button should be enabled (cover fully open and not animating)
    val isActionEnabled = dragState.currentValue == CoverSlideState.OPEN && !dragState.isAnimationRunning

    // Calculate alpha for the action button based on cover position
    val actionButtonAlpha by remember {
        derivedStateOf {
            if (coverHeightPx <= 0f) return@derivedStateOf 0f // Avoid division by zero
            val offset = try { dragState.requireOffset() } catch (e: IllegalStateException) { 0f }
            val progress = (offset / -coverHeightPx).coerceIn(0f, 1f) // Progress from 0 (closed) to 1 (open)
            if (progress.isNaN()) 0f else progress // Handle potential NaN
        }
    }


    Box(
        modifier = modifier
            .clipToBounds() // Clip children to the bounds of this Box
            .background(GreyDarkSecondary) // Background color of the whole component slot
            .onSizeChanged { size ->
                // Update component height when size changes
                componentHeightPx = size.height.toFloat()
            }
    ) {
        // Only render children if the height is measured
        if (componentHeightPx > 0f) {
            // --- Momentary Button (Action Button) ---
            // Positioned at the bottom half
            MomentaryButton(
                isPressed = momentaryButtonPressed,
                onIsPressedChange = { pressed ->
                    // Only allow pressing if the action is enabled
                    momentaryButtonPressed = if (isActionEnabled) pressed else false
                },
                onPress = {
                    if (isActionEnabled) {
                        Log.d("SafetyButton", "Action Button Pressed!")
                        onSafeClick() // Execute the safe click action
                        if(vibrateOnAction) feedbackViewModel.vibrate(vibrationDurationMs, vibrationAmplitude) // Trigger vibration via ViewModel
                    }
                },
                onRelease = {
                    // When released, start closing the cover after a short delay
                    scope.launch {
                        try {
                            delay(500L) // Wait briefly before closing
                            dragState.animateTo(CoverSlideState.CLOSED)
                        } catch (e: Exception) {
                            Log.e("SafetyButton", "Error animating cover closed after click", e)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f) // Occupies bottom half
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { alpha = actionButtonAlpha }, // Fade in as cover opens
                enabled = isActionEnabled, // Enable only when cover is fully open
                shape = RectangleShape, // No rounded corners for the bottom part
                colors = ButtonDefaults.buttonColors(
                    containerColor = actionButtonColor,
                    contentColor = textColor,
                    disabledContainerColor = actionButtonColor.copy(alpha = 0.5f), // Dim when disabled
                    disabledContentColor = textColor.copy(alpha = 0.7f)
                ),
                contentPadding = PaddingValues(8.dp),
                text = text,
                // Pass specific sound IDs and the injected FeedbackViewModel
                feedbackViewModel = feedbackViewModel,
                pressSoundResId = actionPressSoundResId,
                releaseSoundResId = actionReleaseSoundResId,
                vibrateOnPress = false // Vibration is handled in onPress above based on vibrateOnAction flag
            )

            // --- Cover ---
            // Positioned at the bottom half initially, then offset upwards
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f) // Occupies top half visually when closed
                    .align(Alignment.BottomCenter) // Align to bottom to make offset calculation easier
                    .offset {
                        // Calculate the vertical offset based on the drag state
                        val currentOffset = try {
                            dragState.requireOffset()
                        } catch (e: IllegalStateException) { 0f } // Default to 0 if offset not available
                        IntOffset(x = 0, y = currentOffset.roundToInt())
                    }
                    .background(coverColor)
                    .anchoredDraggable( // Make the cover draggable
                        state = dragState,
                        orientation = Orientation.Vertical,
                        enabled = coverHeightPx > 0f // Enable dragging only when height is known
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("COVER", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else {
            // Optional: Show a placeholder or empty space while waiting for measurement
            Spacer(modifier = Modifier.fillMaxSize())
        }
    }
}
