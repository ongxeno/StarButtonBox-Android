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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape // Explicit import
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
    totalHeight: Dp = 100.dp,
    coverColor: Color = OrangeDarkPrimary,
    actionButtonColor: Color = ActionButtonColor,
    textColor: Color = OnDarkSurface,
    autoCloseDelayMs: Long = 5000L,
    onSafeClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val coverHeightPx = remember(totalHeight, density) { with(density) { (totalHeight / 2).toPx() } }

    val anchors = remember(coverHeightPx) {
        DraggableAnchors {
            CoverSlideState.CLOSED at 0f
            CoverSlideState.OPEN at -coverHeightPx
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

    var autoCloseJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(dragState.currentValue, dragState.isAnimationRunning) {
        val target = dragState.targetValue
        val current = dragState.currentValue

        Log.d("SlidingCover", "Effect triggered. Current: $current, Target: $target, IsAnimating: ${dragState.isAnimationRunning}, Offset: ${try { dragState.requireOffset() } catch (e:IllegalStateException) { "NaN" }}")

        if (current == CoverSlideState.OPEN && !dragState.isAnimationRunning) {
            Log.d("SlidingCover", "Cover settled OPEN, starting/ensuring auto-close timer.")
            autoCloseJob?.cancel()
            autoCloseJob = scope.launch {
                delay(autoCloseDelayMs)
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

    DisposableEffect(Unit) {
        onDispose {
            autoCloseJob?.cancel()
        }
    }

    val actionButtonAlpha by remember {
        derivedStateOf {
            if (coverHeightPx <= 0f) return@derivedStateOf 0f
            val progress = (dragState.requireOffset() / -coverHeightPx).coerceIn(0f, 1f)
            if (progress.isNaN()) 0f else progress
        }
    }
    val isActionEnabled = dragState.currentValue == CoverSlideState.OPEN && !dragState.isAnimationRunning

    Box(
        modifier = modifier
            .height(totalHeight)
            .clipToBounds()
            .background(GreyDarkSecondary)
    ) {
        Button(
            onClick = {
                if (isActionEnabled) {
                    Log.d("SlidingCover", "Action Button Clicked!")
                    onSafeClick()
                    scope.launch {
                        try {
                            delay(500L)
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
            contentPadding = PaddingValues(8.dp)
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter)
                .offset {
                    val currentOffset = try {
                        dragState.requireOffset()
                    } catch (e: IllegalStateException) {
                        0f
                    }
                    IntOffset(x = 0, y = currentOffset.roundToInt())
                }
                .background(coverColor)
                .anchoredDraggable(
                    state = dragState,
                    orientation = Orientation.Vertical
                )
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("COVER", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
