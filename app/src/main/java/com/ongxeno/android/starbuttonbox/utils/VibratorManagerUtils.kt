package com.ongxeno.android.starbuttonbox.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
// Removed staticCompositionLocalOf and LocalVibrator
import javax.inject.Inject // Import Inject
import javax.inject.Singleton // Import Singleton

// Removed LocalVibrator definition

/**
 * Utility class to abstract vibration logic and handle API level differences.
 * Accepts a nullable Vibrator instance.
 */
// Add @Singleton if you want Hilt to manage its lifecycle via AppModule,
// otherwise remove it if it's only instantiated directly (e.g., in FeedbackViewModel)
// @Singleton - Removing this as FeedbackViewModel now instantiates it directly
class VibratorManagerUtils /*@Inject*/ constructor( // Remove @Inject if not managed by Hilt directly
    private val vibrator: Vibrator? // Accept nullable Vibrator
) {

    companion object {
        private const val TAG = "VibratorManagerUtils"
    }

    /**
     * Checks if the device has a vibrator.
     * @return True if a vibrator is available, false otherwise.
     */
    fun hasVibrator(): Boolean {
        return vibrator?.hasVibrator() ?: false
    }

    /**
     * Performs a vibration.
     *
     * @param durationMs The duration to vibrate in milliseconds.
     * @param amplitude The vibration amplitude (1-255). Use VibrationEffect.DEFAULT_AMPLITUDE for default. Ignored below API 26.
     */
    fun vibrate(durationMs: Long, amplitude: Int = VibrationEffect.DEFAULT_AMPLITUDE) {
        if (vibrator == null) {
            Log.w(TAG, "Vibrator service not available.")
            return
        }

        if (!hasVibrator()) {
            Log.w(TAG, "Device does not have a vibrator.")
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use VibrationEffect for newer APIs
                val effect = VibrationEffect.createOneShot(durationMs, amplitude)
                vibrator.vibrate(effect)
                Log.d(TAG, "Vibrating with effect: duration=$durationMs, amplitude=$amplitude")
            } else {
                // Use deprecated vibrate method for older APIs
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMs)
                Log.d(TAG, "Vibrating (legacy): duration=$durationMs")
            }
        } catch (e: Exception) {
            // Catch potential exceptions during vibration call
            Log.e(TAG, "Error triggering vibration", e)
        }
    }

    /**
     * Performs a short, predefined vibration effect (like a tick).
     * Uses VibrationEffect.EFFECT_TICK on API 29+ or falls back to custom duration.
     */
    fun vibrateTick() {
        if (vibrator == null || !hasVibrator()) return // Early exit if no vibrator

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val tickEffect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
                vibrator.vibrate(tickEffect)
                Log.d(TAG, "Vibrating with EFFECT_TICK")
            } else {
                // Fallback for older APIs
                vibrate(20) // Short vibration
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering tick vibration", e)
        }
    }

    /**
     * Cancels any ongoing vibration.
     */
    fun cancel() {
        if (vibrator == null || !hasVibrator()) return

        try {
            vibrator.cancel()
            Log.d(TAG, "Vibration cancelled.")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibration", e)
        }
    }
}

// Removed getVibratorManager function - Hilt provides Vibrator? via AppModule
