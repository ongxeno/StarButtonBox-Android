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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ongxeno.android.starbuttonbox.R
import com.ongxeno.android.starbuttonbox.utils.LocalSoundPlayer
import com.ongxeno.android.starbuttonbox.utils.LocalVibrator

/**
 * A button that triggers actions, sounds, and vibration while pressed and on release,
 * managing its own pressed state internally.
 *
 * Uses LocalVibrator and LocalSoundPlayer.
 *
 * @param modifier Modifier for layout customization.
 * @param text Text displayed on the button.
 * @param enabled Controls if the button is interactive.
 * @param color The background color when not pressed.
 * @param pressedColor The background color when pressed.
 * @param disabledColor The background color when disabled.
 * @param contentColor The color of the text label.
 * @param disabledContentColor The color of the text label when disabled.
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
    contentPadding: PaddingValues = PaddingValues(16.dp, 10.dp),
    @RawRes pressSoundResId: Int = R.raw.snes_press,
    @RawRes releaseSoundResId: Int = R.raw.snes_release,
    onPress: () -> Unit = {},
    onRelease: () -> Unit = {},
    vibrateOnPress: Boolean = true,
    vibrationDurationMs: Long = 40,
    vibrationAmplitude: Int = -1
) {
    var isPressed by remember { mutableStateOf(false) }

    MomentaryButton(
        modifier = modifier,
        text = text,
        isPressed = isPressed,
        onIsPressedChange = { pressed -> isPressed = pressed },
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
    isPressed: Boolean = false,
    onIsPressedChange: (Boolean) -> Unit,
    shape: Shape = RectangleShape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = PaddingValues(16.dp, 10.dp),
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

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1.0f,
        animationSpec = if (isPressed) snap() else tween(durationMillis = 100),
        label = "ButtonScaleAnimation"
    )

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
        if (vibrateOnPress && vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val clampedAmplitude = when (vibrationAmplitude) {
                    -1 -> VibrationEffect.EFFECT_TICK
                    else -> vibrationAmplitude.coerceIn(1, 255)
                }
                val effect = VibrationEffect.createOneShot(vibrationDurationMs, clampedAmplitude)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(vibrationDurationMs)
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(currentBackgroundColor)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                awaitPointerEventScope {
                    while (true) {
                        awaitFirstDown(requireUnconsumed = false)
                        onIsPressedChange(true)
                        onVibrate()
                        onPress()
                        soundPlayer?.playSound(pressSoundResId)

                        waitForUpOrCancellation()
                        onIsPressedChange(false)
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
            fontSize = 12.sp,
            lineHeight = 14.sp,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
