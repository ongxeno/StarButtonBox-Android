package com.ongxeno.android.starbuttonbox.ui.button

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.annotation.RawRes
import com.ongxeno.android.starbuttonbox.R
import com.ongxeno.android.starbuttonbox.utils.LocalSoundPlayer

/**
 * A button that triggers actions and sounds while pressed and on release,
 * with press/release scaling animation.
 *
 * @param modifier Modifier for layout customization.
 * @param text Text displayed on the button.
 * @param onPress Lambda executed when the button is first pressed.
 * @param onRelease Lambda executed when the button is released or interaction cancelled.
 * @param enabled Controls if the button is interactive.
 * @param color The background color when not pressed.
 * @param pressedColor The background color when pressed.
 * @param disabledColor The background color when disabled.
 * @param contentColor The color of the text label.
 * @param disabledContentColor The color of the text label when disabled.
 */
@Composable
fun MomentaryButton(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    pressedColor: Color = MaterialTheme.colorScheme.primaryContainer,
    disabledColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    disabledContentColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
    @RawRes pressSoundResId: Int = R.raw.snes_press,
    @RawRes releaseSoundResId: Int = R.raw.snes_release,
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {}
) {
    val soundPlayer = LocalSoundPlayer.current
    LaunchedEffect(soundPlayer, pressSoundResId, releaseSoundResId, enabled) {
        if (enabled) {
            soundPlayer?.loadSound(pressSoundResId)
            soundPlayer?.loadSound(releaseSoundResId)
        }
    }

    var isPressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1.0f,
        animationSpec = if (isPressed) snap() else tween(durationMillis = 100),
        label = "ButtonScaleAnimation"
    )

    val currentBackgroundColor = when {
        !enabled -> disabledColor
        isPressed -> pressedColor
        else -> color
    }
    val currentContentColor = when {
        !enabled -> disabledContentColor
        isPressed -> contentColor
        else -> contentColor
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(32.dp))
            .background(currentBackgroundColor)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        onPress()
                        soundPlayer?.playSound(pressSoundResId)

                        waitForUpOrCancellation()
                        isPressed = false
                        onRelease()
                        soundPlayer?.playSound(releaseSoundResId)
                    }
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = currentContentColor,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
