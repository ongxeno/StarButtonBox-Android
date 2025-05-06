package com.ongxeno.android.starbuttonbox.ui // Or a more specific package like ui.feedback

import android.util.Log
import androidx.annotation.RawRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ongxeno.android.starbuttonbox.R
import com.ongxeno.android.starbuttonbox.utils.SoundPlayer
import com.ongxeno.android.starbuttonbox.utils.VibratorManagerUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel dedicated to handling UI feedback like sounds and vibrations.
 */
@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val soundPlayer: SoundPlayer,
    private val vibratorManagerUtils: VibratorManagerUtils
) : ViewModel() {

    private val _tag = "FeedbackViewModel"

    init {
        Log.d(_tag, "FeedbackViewModel Initialized (Vibrator available: ${vibratorManagerUtils.hasVibrator()})") // Check via util method
        // Optional: Preload common sounds on initialization
        preloadSounds(
            R.raw.snes_press,
            R.raw.snes_release,
            R.raw.super8_open,
            R.raw.super8_close
        )
    }

    fun preloadSounds(vararg soundResIds: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            soundResIds.forEach {
                try {
                    soundPlayer.loadSound(it)
                    Log.d(_tag, "Preloaded sound ID: $it")
                } catch (e: Exception) {
                    Log.e(_tag, "Error preloading sound ID: $it", e)
                }
            }
        }
    }

    /**
     * Plays a sound effect.
     * @param soundResId The raw resource ID of the sound to play.
     */
    fun playSound(@RawRes soundResId: Int) {
        try {
            soundPlayer.playSound(soundResId)
        } catch (e: Exception) {
            Log.e(_tag, "Error playing sound ID: $soundResId", e)
        }
    }

    /**
     * Triggers a vibration effect using the injected VibratorManagerUtils.
     * @param durationMs Duration of the vibration in milliseconds.
     * @param amplitude Vibration intensity (1-255, API 26+). -1 for default system intensity/effect.
     */
    fun vibrate(durationMs: Long = 40, amplitude: Int = -1) {
        vibratorManagerUtils.vibrate(durationMs, amplitude)
    }
}
