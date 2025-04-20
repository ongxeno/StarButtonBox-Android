package com.ongxeno.android.starbuttonbox.ui.button

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ongxeno.android.starbuttonbox.ui.theme.GreyDarkSecondary
import com.ongxeno.android.starbuttonbox.ui.theme.OnDarkSurface


@Composable
fun MomentaryButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = GreyDarkSecondary,
    textColor: Color = OnDarkSurface,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(IntrinsicSize.Min) // Adjust height as needed
            .padding(4.dp),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
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

