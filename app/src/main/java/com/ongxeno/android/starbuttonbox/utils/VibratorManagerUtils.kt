package com.ongxeno.android.starbuttonbox.utils

import android.content.Context
import android.os.Build
import android.os.Vibrator
import androidx.compose.runtime.staticCompositionLocalOf

val LocalVibrator = staticCompositionLocalOf<Vibrator?> { null }

fun getVibratorManager(context: Context) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val vibratorManager =
        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
    vibratorManager?.defaultVibrator
} else {
    @Suppress("DEPRECATION")
    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
}