package com.ongxeno.android.starbuttonbox.ui.button

import android.util.Log
import androidx.compose.animation.core.exponentialDecay // For decay spec
import androidx.compose.animation.core.tween // For snap/settle spec
import androidx.compose.foundation.ExperimentalFoundationApi // Correct OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.*
import kotlin.math.roundToInt

// Define states for the cover's position
@OptIn(ExperimentalFoundationApi::class)
private enum class CoverSlideState {
    CLOSED, // Cover is down
    OPEN    // Cover is up
}

// Default colors (adjust as needed)
private val CoverColor = Color(0xFFFFA500) // Orange cover like some safety switches
private val ActionButtonColor = Color(0xFF880000) // Red action button
private val ButtonTextColor = Color.White
private val FrameColor = Color.DarkGray // Background for the whole component

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SafetyButton(
    text: String, // Text for the action button
    modifier: Modifier = Modifier,
    totalHeight: Dp = 100.dp, // Re-added totalHeight parameter for fixed size
    coverColor: Color = CoverColor,
    actionButtonColor: Color = ActionButtonColor,
    textColor: Color = ButtonTextColor,
    autoCloseDelayMs: Long = 5000L,
    onSafeClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    // Calculate cover height based on the fixed totalHeight again
    val coverHeightPx = remember(totalHeight, density) { with(density) { (totalHeight / 2).toPx() } }

    // Define anchors (can be defined directly now)
    val anchors = remember(coverHeightPx) {
        DraggableAnchors {
            CoverSlideState.CLOSED at 0f
            CoverSlideState.OPEN at -coverHeightPx // Use calculated height
        }
    }

    // State for managing the cover's position and animation (now uses fixed anchors)
    val dragState = remember {
        AnchoredDraggableState(
            initialValue = CoverSlideState.CLOSED,
            anchors = anchors, // Use fixed anchors
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            snapAnimationSpec = tween(), // Animation for settling/snapping
            decayAnimationSpec = exponentialDecay() // Animation for flinging
        )
    }

    // Timeout job state
    var autoCloseJob by remember { mutableStateOf<Job?>(null) }

    // Effect to handle automatic closing and cancellation
    LaunchedEffect(dragState.currentValue, dragState.isAnimationRunning) {
        val target = dragState.targetValue
        val current = dragState.currentValue

        Log.d("SlidingCover", "Effect triggered. Current: $current, Target: $target, IsAnimating: ${dragState.isAnimationRunning}, Offset: ${try { dragState.requireOffset() } catch (e:IllegalStateException) { "NaN" }}")

        if (current == CoverSlideState.OPEN && !dragState.isAnimationRunning) {
            Log.d("SlidingCover", "Cover settled OPEN, starting/ensuring auto-close timer.")
            autoCloseJob?.cancel() // Cancel potentially old job first
            autoCloseJob = scope.launch {
                delay(autoCloseDelayMs)
                // Check again if still open and not animating before closing automatically
                if (isActive && dragState.currentValue == CoverSlideState.OPEN && !dragState.isAnimationRunning) {
                    Log.d("SlidingCover", "Auto-close timer finished, initiating slide to CLOSED.")
                    try {
                        dragState.animateTo(CoverSlideState.CLOSED)
                        Log.d("SlidingCover", "animateTo(CLOSED) finished.")
                    } catch (e: CancellationException) {
                        Log.d("SlidingCover", "animateTo(CLOSED) cancelled.")
                    } catch (e: Exception) {
                        Log.e("SlidingCover", "Error during animateTo(CLOSED)", e)
                    }
                } else {
                    Log.d("SlidingCover", "Auto-close timer finished, but state no longer OPEN or animation running/cancelled.")
                }
            }
        } else if (current == CoverSlideState.CLOSED && !dragState.isAnimationRunning) {
            Log.d("SlidingCover", "Cover settled CLOSED, cancelling any active auto-close timer.")
            autoCloseJob?.cancel()
        }
    }

    // Ensure job is cancelled when composable leaves screen
    DisposableEffect(Unit) {
        onDispose {
            autoCloseJob?.cancel()
        }
    }

    // Determine button visibility/alpha (logic simplified as coverHeightPx is known)
    val actionButtonAlpha by remember {
        derivedStateOf {
            if (coverHeightPx <= 0f) return@derivedStateOf 0f
            // Calculate progress based on offset relative to cover height
            val progress = (dragState.requireOffset() / -coverHeightPx).coerceIn(0f, 1f)
            // Return 0f if offset is NaN (initial state before anchors updated)
            if (progress.isNaN()) 0f else progress
        }
    }
    // Enable only when fully open and settled
    val isActionEnabled = dragState.currentValue == CoverSlideState.OPEN && !dragState.isAnimationRunning

    Box(
        modifier = modifier
            .height(totalHeight) // Use fixed height for the outer Box
            .clipToBounds()
            .background(FrameColor)
    ) {
        // --- Action Button (Bottom Layer) ---
        Button(
            onClick = {
                if (isActionEnabled) {
                    Log.d("SlidingCover", "Action Button Clicked!")
                    onSafeClick()
                    // Force cover closed immediately after successful click
                    scope.launch {
                        try {
                            delay(500L)
                            // Use snapTo for instant close after action
                            dragState.animateTo(CoverSlideState.CLOSED)
                        } catch (e: Exception) {
                            Log.e("SlidingCover", "Error during snapTo(CLOSED) after click", e)
                        }
                    }
                } else {
                    Log.d("SlidingCover", "Action Button Clicked but not enabled.")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f) // Occupy bottom half
                .align(Alignment.BottomCenter)
                .graphicsLayer { alpha = actionButtonAlpha }, // Fade in/out
            enabled = isActionEnabled, // Enable only when cover is fully open and settled
            shape = androidx.compose.ui.graphics.RectangleShape, // No rounded corners
            colors = ButtonDefaults.buttonColors(
                containerColor = actionButtonColor,
                contentColor = textColor,
                disabledContainerColor = actionButtonColor.copy(alpha = 0.5f), // Dim when disabled
                disabledContentColor = textColor.copy(alpha = 0.7f)
            ),
            contentPadding = PaddingValues(8.dp)
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // --- Sliding Cover (Top Layer) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f) // Occupy visual top half when closed
                .align(Alignment.BottomCenter) // Start aligned at bottom...
                .offset { // ...then offset based on drag state
                    // Use the offset from the draggable state
                    val currentOffset = try {
                        dragState.requireOffset() // Get offset or throw if not ready
                    } catch (e: IllegalStateException) {
                        0f // Default to 0f if offset isn't initialized yet
                    }
                    IntOffset(x = 0, y = currentOffset.roundToInt())
                }
                .background(coverColor)
                .anchoredDraggable( // Apply draggable behavior
                    state = dragState,
                    orientation = Orientation.Vertical
                )
                .padding(4.dp), // Optional padding inside cover content area
            contentAlignment = Alignment.Center
        ) {
            // Content for the cover itself
            Text("COVER", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}