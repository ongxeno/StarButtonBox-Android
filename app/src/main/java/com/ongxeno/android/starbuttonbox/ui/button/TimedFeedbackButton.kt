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
import com.ongxeno.android.starbuttonbox.ui.theme.GreyDarkSecondary
import com.ongxeno.android.starbuttonbox.ui.theme.OnDarkSurface
import com.ongxeno.android.starbuttonbox.ui.theme.OrangeDarkPrimary
import kotlinx.coroutines.launch

@Composable
fun TimedFeedbackButton(
    text: String,
    modifier: Modifier = Modifier,
    feedbackColor: Color = OrangeDarkPrimary,
    defaultColor: Color = GreyDarkSecondary,
    textColor: Color = OnDarkSurface,
    feedbackDurationMs: Long = 3000L,
    onClick: () -> Unit
) {
    val animatedColor = remember { Animatable(defaultColor) }
    val coroutineScope = rememberCoroutineScope()

    Button(
        onClick = {
            // 1. Trigger the primary action immediately
            onClick()

            // 2. Start the color fade animation
            coroutineScope.launch {
                animatedColor.snapTo(feedbackColor)
                animatedColor.animateTo(
                    targetValue = defaultColor,
                    animationSpec = tween(durationMillis = feedbackDurationMs.toInt())
                )
            }
        },
        modifier = modifier
            .height(IntrinsicSize.Min) // Adjust height as needed
            .padding(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = animatedColor.value
        ),
        contentPadding = PaddingValues(8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            lineHeight = 14.sp
        )
    }
}