package com.ongxeno.android.starbuttonbox.ui.button

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ComponentPowerControl(
    componentName: String,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onActionClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            componentName,
            color = Color.LightGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Column(
            modifier = Modifier.width(IntrinsicSize.Min),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MomentaryButton(
                text = "+",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                onClick = onIncrease
            )
            Spacer(Modifier.height(4.dp))

            TimedFeedbackButton(
                text = "TOGGLE",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                onClick = onActionClick
            )

            Spacer(Modifier.height(4.dp))
            MomentaryButton(
                text = "-",
                modifier = Modifier.fillMaxWidth().height(48.dp),
                onClick = onDecrease
            )
        }
    }
}