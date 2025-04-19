package com.ongxeno.android.starbuttonbox.ui.button

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ongxeno.android.starbuttonbox.ui.theme.ButtonColor
import com.ongxeno.android.starbuttonbox.ui.theme.ButtonTextColor
import com.ongxeno.android.starbuttonbox.ui.theme.ToggleOnColor
import kotlinx.coroutines.launch

@Composable
fun TimedFeedbackButton(
    text: String,
    modifier: Modifier = Modifier,
    feedbackColor: Color = ToggleOnColor, // Color to start fade from
    defaultColor: Color = ButtonColor,   // Color to fade back to
    textColor: Color = ButtonTextColor,
    feedbackDurationMs: Long = 3000L,    // Duration of the fade animation
    onClick: () -> Unit                   // Action to perform on click
) {
    // Use Animatable to control the color animation smoothly
    // Initialize with the defaultColor
    val animatedColor = remember { Animatable(defaultColor) }
    val scope = rememberCoroutineScope()

    Button(
        onClick = {
            // 1. Trigger the primary action immediately
            onClick()

            // 2. Start the color fade animation
            scope.launch {
                // Immediately snap to the feedback color when pressed
                animatedColor.snapTo(feedbackColor)
                // Animate back to the default color over the specified duration
                animatedColor.animateTo(
                    targetValue = defaultColor,
                    animationSpec = tween(durationMillis = feedbackDurationMs.toInt()) // Specify duration
                )
            }
        },
        modifier = modifier
            .height(IntrinsicSize.Min) // Adjust height as needed
            .padding(4.dp),
        colors = ButtonDefaults.buttonColors(
            // Use the current value of the animation for the container color
            containerColor = animatedColor.value
        ),
        contentPadding = PaddingValues(8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            textAlign = TextAlign.Center,
            fontSize = 12.sp, // Adjust font size
            lineHeight = 14.sp
        )
    }
}