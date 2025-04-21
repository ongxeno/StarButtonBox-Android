package com.ongxeno.android.starbuttonbox.utils // Or your preferred package

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

val LocalSoundPlayer = staticCompositionLocalOf<SoundPlayer?> { null }

/**
 * Manages SoundPool for playing short audio clips.
 * Handles loading sounds and ensures SoundPool is released.
 */
class SoundPlayer(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private var soundPool: SoundPool? = null
    private val soundIdCache = ConcurrentHashMap<Int, Int>()
    private val loadingSounds = ConcurrentHashMap.newKeySet<Int>()
    private var isSoundPoolLoaded = false

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5) // Allow up to 5 simultaneous sounds
            .setAudioAttributes(audioAttributes)
            .build()

        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                Log.d("SoundPlayer", "SoundPool loaded sample ID: $sampleId successfully.")
            } else {
                Log.e("SoundPlayer", "SoundPool failed to load sample ID: $sampleId, status: $status")
                val resIdToRemove = soundIdCache.entries.find { it.value == sampleId }?.key
                if (resIdToRemove != null) {
                    soundIdCache.remove(resIdToRemove)
                    loadingSounds.remove(resIdToRemove)
                }
            }
        }
        isSoundPoolLoaded = true // Assume ready after creation for simplicity here
        Log.d("SoundPlayer", "SoundPool initialized.")
    }

    /**
     * Preloads a sound resource if not already loaded or loading.
     * This should be called before trying to play the sound for the first time.
     *
     * @param resId The raw resource ID (e.g., R.raw.snes_press).
     */
    fun loadSound(resId: Int) {
        if (soundPool == null || soundIdCache.containsKey(resId) || loadingSounds.contains(resId)) {
            // Already loaded, loading, or SoundPool not ready
            return
        }

        loadingSounds.add(resId) // Mark as loading
        Log.d("SoundPlayer", "Attempting to load sound Res ID: $resId")

        // Launch loading in a background thread
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val soundId = soundPool?.load(context, resId, 1)
                if (soundId != null && soundId != 0) {
                    soundIdCache[resId] = soundId
                    Log.d("SoundPlayer", "Successfully initiated loading for Res ID: $resId, SoundPool ID: $soundId")
                } else {
                    Log.e("SoundPlayer", "Failed to initiate loading for Res ID: $resId. load() returned 0 or null.")
                }
            } catch (e: Exception) {
                Log.e("SoundPlayer", "Exception loading sound Res ID: $resId", e)
            } finally {
                loadingSounds.remove(resId)
            }
        }
    }

    /**
     * Plays a sound that has been loaded.
     *
     * @param resId The raw resource ID of the sound to play.
     * @param volume Playback volume (0.0 to 1.0).
     * @param priority Stream priority (0 = lowest).
     * @param loop Loop mode (0 = no loop, -1 = loop forever).
     * @param rate Playback rate (0.5 to 2.0, 1.0 = normal).
     */
    fun playSound(
        resId: Int,
        volume: Float = 1.0f,
        priority: Int = 1,
        loop: Int = 0,
        rate: Float = 1.0f
    ) {
        if (soundPool == null || !isSoundPoolLoaded) {
            Log.w("SoundPlayer", "SoundPool not initialized or not ready. Cannot play sound Res ID: $resId")
            return
        }

        val soundId = soundIdCache[resId]
        if (soundId != null) {
            soundPool?.play(soundId, volume, volume, priority, loop, rate)
            Log.d("SoundPlayer", "Playing sound Res ID: $resId (SoundPool ID: $soundId)")
        } else {
            Log.w("SoundPlayer", "Sound Res ID: $resId not loaded yet. Attempting to load and play.")
            loadSound(resId)
        }
    }

    /**
     * Releases the SoundPool resources. Call this when the SoundPlayer is no longer needed.
     */
    fun release() {
        Log.d("SoundPlayer", "Releasing SoundPool.")
        soundPool?.release()
        soundPool = null
        soundIdCache.clear()
        loadingSounds.clear()
        isSoundPoolLoaded = false
    }
}

/**
 * Composable helper to remember and manage the lifecycle of a SoundPlayer.
 */
@Composable
fun rememberSoundPlayer(): SoundPlayer? {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Remember the SoundPlayer instance across recompositions
    val soundPlayer = remember {
        Log.d("rememberSoundPlayer", "Creating new SoundPlayer instance.")
        SoundPlayer(context.applicationContext, coroutineScope)
    }

    // Use DisposableEffect to release the SoundPool when the composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            Log.d("rememberSoundPlayer", "Disposing SoundPlayer.")
            soundPlayer.release()
        }
    }

    return soundPlayer
}
