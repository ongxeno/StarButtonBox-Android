package com.ongxeno.android.starbuttonbox.utils // Or your preferred package

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
// Removed Composable-specific imports: Composable, DisposableEffect, remember, rememberCoroutineScope, staticCompositionLocalOf, LocalContext
import androidx.annotation.RawRes // Keep RawRes annotation
import dagger.hilt.android.qualifiers.ApplicationContext // Import Hilt context qualifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel // Import cancel
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject // Import Inject annotation
import javax.inject.Singleton // Import Singleton annotation

// Removed LocalSoundPlayer definition

/**
 * Manages SoundPool for playing short audio clips.
 * Handles loading sounds and ensures SoundPool is released.
 * Provided as a Singleton by Hilt.
 */
@Singleton // Ensure Hilt provides only one instance
class SoundPlayer @Inject constructor( // Use @Inject for Hilt
    @ApplicationContext private val context: Context // Inject application context
    // Removed CoroutineScope dependency
) {
    private var soundPool: SoundPool? = null
    private val soundIdCache = ConcurrentHashMap<Int, Int>() // Cache for loaded sound IDs
    private val loadingSounds = ConcurrentHashMap.newKeySet<Int>() // Track sounds currently loading
    private var isSoundPoolLoaded = false

    // Create a dedicated scope for this singleton's background tasks
    // Use SupervisorJob so failure of one loading job doesn't cancel others
    private val playerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        initializeSoundPool()
    }

    private fun initializeSoundPool() {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(5) // Allow up to 5 simultaneous sounds
                .setAudioAttributes(audioAttributes)
                .build()

            soundPool?.setOnLoadCompleteListener { sp, sampleId, status ->
                // Find the resource ID associated with the loaded sample ID
                // This part is tricky as the listener doesn't give us the original resId.
                // We rely on removing it from loadingSounds upon completion/failure.
                // A better approach might involve tracking futures or using a more complex map.
                var resIdFound: Int? = null
                for ((key, value) in soundIdCache.entries) {
                    if (value == sampleId) {
                        resIdFound = key
                        break
                    }
                }

                if (status == 0) {
                    Log.d("SoundPlayer", "SoundPool loaded sample ID: $sampleId (Res ID: $resIdFound) successfully.")
                    // If successfully loaded, it should already be in soundIdCache from loadSound()
                } else {
                    Log.e("SoundPlayer", "SoundPool failed to load sample ID: $sampleId (Res ID: $resIdFound), status: $status")
                    // Remove from cache if it exists (though it might not if load() failed earlier)
                    if (resIdFound != null) {
                        soundIdCache.remove(resIdFound)
                    }
                }
                // Remove from loading set once load attempt is complete (success or fail)
                // This requires iterating loadingSounds or finding the resId if possible
                // For simplicity, let's assume loadSound handles removal on immediate failure
                // and this listener handles removal on async failure/success.
                // We need a way to map sampleId back to resId reliably here if we want to remove from loadingSounds.
                // Let's stick to removing based on resId if found for now.
                if (resIdFound != null) {
                    loadingSounds.remove(resIdFound)
                }
            }
            isSoundPoolLoaded = true // Mark as ready
            Log.d("SoundPlayer", "SoundPool initialized successfully.")
        } catch (e: Exception) {
            Log.e("SoundPlayer", "Failed to initialize SoundPool", e)
            soundPool = null // Ensure soundPool is null if initialization failed
            isSoundPoolLoaded = false
        }
    }

    /**
     * Preloads a sound resource if not already loaded or loading.
     *
     * @param resId The raw resource ID (e.g., R.raw.snes_press).
     */
    fun loadSound(@RawRes resId: Int) {
        val currentSoundPool = soundPool // Capture current state
        if (currentSoundPool == null || !isSoundPoolLoaded || soundIdCache.containsKey(resId) || !loadingSounds.add(resId)) {
            // If pool not ready, already loaded, or failed to add to loading set (meaning it's already being loaded)
            if (currentSoundPool == null || !isSoundPoolLoaded) Log.w("SoundPlayer", "loadSound($resId): SoundPool not ready.")
            else if (soundIdCache.containsKey(resId)) Log.d("SoundPlayer", "loadSound($resId): Already loaded.")
            else Log.d("SoundPlayer", "loadSound($resId): Already loading.")
            return
        }

        Log.d("SoundPlayer", "Attempting to load sound Res ID: $resId")

        // Launch loading in the player's own scope
        playerScope.launch { // Use the internal playerScope
            var soundId: Int? = null
            try {
                soundId = currentSoundPool.load(context, resId, 1)
                if (soundId != null && soundId != 0) {
                    // Store the mapping from resource ID to SoundPool sample ID
                    soundIdCache[resId] = soundId
                    Log.d("SoundPlayer", "Successfully initiated loading for Res ID: $resId, SoundPool ID: $soundId")
                    // Completion/failure handled by setOnLoadCompleteListener which should remove from loadingSounds
                } else {
                    Log.e("SoundPlayer", "Failed to initiate loading for Res ID: $resId. load() returned 0 or null.")
                    loadingSounds.remove(resId) // Remove from loading if load call failed immediately
                }
            } catch (e: Exception) {
                Log.e("SoundPlayer", "Exception loading sound Res ID: $resId", e)
                loadingSounds.remove(resId) // Remove from loading on exception
                // If we had a valid soundId before exception, remove it from cache
                if (soundId != null) {
                    soundIdCache.entries.removeIf { it.value == soundId }
                }
            }
        }
    }

    /**
     * Plays a sound that has been loaded. Attempts to load if not already loaded.
     *
     * @param resId The raw resource ID of the sound to play.
     * @param volume Playback volume (0.0 to 1.0).
     * @param priority Stream priority (0 = lowest).
     * @param loop Loop mode (0 = no loop, -1 = loop forever).
     * @param rate Playback rate (0.5 to 2.0, 1.0 = normal).
     */
    fun playSound(
        @RawRes resId: Int,
        volume: Float = 1.0f,
        priority: Int = 1,
        loop: Int = 0,
        rate: Float = 1.0f
    ) {
        val currentSoundPool = soundPool // Local reference for thread safety
        if (currentSoundPool == null || !isSoundPoolLoaded) {
            Log.w("SoundPlayer", "SoundPool not initialized or not ready. Cannot play sound Res ID: $resId")
            return
        }

        val soundId = soundIdCache[resId]
        if (soundId != null) {
            // Sound is loaded, play it
            currentSoundPool.play(soundId, volume, volume, priority, loop, rate)
            // Log.d("SoundPlayer", "Playing sound Res ID: $resId (SoundPool ID: $soundId)") // Optional: reduce log spam
        } else {
            // Sound not loaded, attempt to load it (it might play next time if called again after loading)
            Log.w("SoundPlayer", "Sound Res ID: $resId not loaded. Attempting background load.")
            loadSound(resId) // Trigger loading
        }
    }

    /**
     * Releases the SoundPool resources and cancels ongoing loading tasks.
     * This should ideally be called when the application is terminating,
     * although Hilt manages the Singleton lifecycle.
     */
    fun release() {
        Log.d("SoundPlayer", "Releasing SoundPlayer resources...")
        soundPool?.release()
        soundPool = null
        soundIdCache.clear()
        loadingSounds.clear()
        playerScope.cancel() // Cancel the internal scope and its jobs
        isSoundPoolLoaded = false
        Log.d("SoundPlayer", "SoundPlayer released.")
    }
}

// Removed rememberSoundPlayer() composable helper
