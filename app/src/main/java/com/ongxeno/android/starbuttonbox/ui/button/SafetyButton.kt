package com.ongxeno.android.starbuttonbox.ui.button

import android.util.Log
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SafetyButton(
    text: String,
    modifier: Modifier = Modifier,
    coverColor: Color = OrangeDarkPrimary,
    actionButtonColor: Color = ActionButtonColor,
    textColor: Color = OnDarkSurface,
    autoCloseDelayMs: Long = 5000L,
    onSafeClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var componentHeightPx by remember { mutableFloatStateOf(0f) }

    val coverHeightPx by remember(componentHeightPx) {
        derivedStateOf { componentHeightPx / 2f }
    }

    val anchors = remember(coverHeightPx) {
        if (coverHeightPx > 0f) {
            DraggableAnchors {
                CoverSlideState.CLOSED at 0f
                CoverSlideState.OPEN at -coverHeightPx
            }
        } else {
            DraggableAnchors { CoverSlideState.CLOSED at 0f }
        }
    }

    val dragState = remember {
        AnchoredDraggableState(
            initialValue = CoverSlideState.CLOSED,
            anchors = anchors,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(),
            decayAnimationSpec = exponentialDecay()
        )
    }

    LaunchedEffect(anchors) {
        dragState.updateAnchors(anchors)
    }

    var autoCloseTimerJob by remember { mutableStateOf<Job?>(null) }

    var momentaryButtonPressed by remember { mutableStateOf(false) }


    LaunchedEffect(dragState.currentValue, dragState.isAnimationRunning, momentaryButtonPressed) {
        val isCoverOpenAndSettled = dragState.currentValue == CoverSlideState.OPEN && !dragState.isAnimationRunning
        val shouldStartTimer = isCoverOpenAndSettled && !momentaryButtonPressed

        // Only start timer if coverHeight is valid (component has been measured)
        if (shouldStartTimer && coverHeightPx > 0f) {
            Log.d("SafetyButton", "Conditions met. Starting auto-close timer.")
            autoCloseTimerJob?.cancel()
            autoCloseTimerJob = scope.launch {
                delay(autoCloseDelayMs)
                if (isActive && dragState.currentValue == CoverSlideState.OPEN && !dragState.isAnimationRunning && !momentaryButtonPressed) {

                    Log.d("SafetyButton", "Auto-close timer finished. Launching separate job to close cover.")
                    // Launch the animation in a SEPARATE job, not tied to autoCloseTimerJob
                    scope.launch {
                        try {
                            dragState.animateTo(CoverSlideState.CLOSED)
                            Log.d("SafetyButton", "animateTo(CLOSED) finished.")
                        } catch (e: CancellationException) {
                            Log.d("SafetyButton", "animateTo(CLOSED) cancelled.")
                        } catch (e: Exception) {
                            Log.e("SafetyButton", "Error during animateTo(CLOSED)", e)
                        }
                    }
                } else {
                    Log.d("SafetyButton", "Auto-close timer finished, but state changed during delay. Not closing.")
                }
            }
        } else {
            autoCloseTimerJob?.cancel()
        }
    }

    LaunchedEffect(dragState) {

        snapshotFlow { dragState.currentValue }
            .distinctUntilChanged() // Only react on change
            .filter { it == CoverSlideState.CLOSED } // Only react when it becomes CLOSED
            .collect {
                Log.d("SafetyButton", "Cover became CLOSED, ensuring momentary button pressed state is false.")
                momentaryButtonPressed = false
                // Also cancel the timer explicitly when cover closes, just in case
                autoCloseTimerJob?.cancel()
            }
    }


    DisposableEffect(Unit) {
        onDispose {
            autoCloseTimerJob?.cancel()
        }
    }

    val isActionEnabled =
        dragState.currentValue == CoverSlideState.OPEN && !dragState.isAnimationRunning
    val actionButtonAlpha by remember {
        derivedStateOf {
            // Prevent division by zero or NaN if measured height isn't available yet
            if (coverHeightPx <= 0f) return@derivedStateOf 0f
            // Use requireOffset safely, default to 0 if calculation is not possible yet
            val offset = try { dragState.requireOffset() } catch (e: IllegalStateException) { 0f }
            val progress = (offset / -coverHeightPx).coerceIn(0f, 1f)
            if (progress.isNaN()) 0f else progress
        }
    }


    Box(
        modifier = modifier
            .clipToBounds()
            .background(GreyDarkSecondary)
            .onSizeChanged { size ->
                componentHeightPx = size.height.toFloat()
            }
    ) {
        // Check if measured height is valid before rendering children that depend on it
        if (componentHeightPx > 0f) {
            // --- Momentary Button (Action Button) ---
            MomentaryButton(
                // Pass the hoisted state and its update lambda
                isPressed = momentaryButtonPressed,
                onIsPressedChange = { pressed ->
                    momentaryButtonPressed = if (isActionEnabled) pressed else false
                },
                onPress = {
                    if (isActionEnabled) {
                        Log.d("SlidingCover", "Action Button Clicked!")
                        onSafeClick()
                    }
                },
                onRelease = {
                    scope.launch {
                        try {
                            delay(500L)
                            dragState.animateTo(CoverSlideState.CLOSED)
                        } catch (e: Exception) {
                            Log.e("SlidingCover", "Error during snapTo(CLOSED) after click", e)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth() // Takes bottom half
                    .fillMaxHeight(0.5f)
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { alpha = actionButtonAlpha },
                    enabled = isActionEnabled,
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = actionButtonColor,
                    contentColor = textColor,
                    disabledContainerColor = actionButtonColor.copy(alpha = 0.5f),
                    disabledContentColor = textColor.copy(alpha = 0.7f)
                ),
                contentPadding = PaddingValues(8.dp),
                text = text,
            )

            // --- Cover ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()// Takes top half initially
                    .fillMaxHeight(0.5f)
                    .align(Alignment.BottomCenter)
                    .offset {// Align to bottom, offset pushes it up
                        val currentOffset = try {
                            dragState.requireOffset()
                        } catch (e: IllegalStateException) { 0f }
                        IntOffset(x = 0, y = currentOffset.roundToInt())
                    }
                    .background(coverColor)
                    .anchoredDraggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        enabled = coverHeightPx > 0f
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("COVER", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        } else {
            // Optionally show a placeholder or empty space while waiting for measurement
            Spacer(modifier = Modifier.fillMaxSize())
        }
    }
}
