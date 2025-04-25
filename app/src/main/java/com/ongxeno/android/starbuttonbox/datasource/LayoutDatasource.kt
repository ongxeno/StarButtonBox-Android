package com.ongxeno.android.starbuttonbox.datasource

import android.content.Context
import android.util.Log // Added Log import
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// Command import no longer needed here
import com.ongxeno.android.starbuttonbox.data.FreeFormItemState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import java.io.IOException // Added IOException import

// Define the DataStore instance at the top level
private val Context.layoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "freeform_layouts")

/**
 * Handles saving and loading of FreeFormLayout states using Jetpack DataStore.
 * Default layout logic is handled by the FreeFormLayout composable itself.
 */
class LayoutDatasource(private val context: Context) {

    companion object {
        private const val TAG = "LayoutDatasource" // Tag for logging
    }

    // Configure Kotlinx JSON serialization
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    // Function to create a Preferences key for a given layout ID
    private fun layoutPreferenceKey(layoutId: String): Preferences.Key<String> {
        return stringPreferencesKey("layout_$layoutId") // Use imported function
    }

    /**
     * Provides a Flow of the layout state (list of items) for a specific layout ID.
     * Emits the list whenever it changes in DataStore.
     * Returns an empty list if no data is found for the ID or on deserialization error.
     */
    fun getLayoutFlow(layoutId: String): Flow<List<FreeFormItemState>> {
        val key = layoutPreferenceKey(layoutId)
        return context.layoutDataStore.data
            .map { preferences ->
                val jsonString = preferences[key]
                if (jsonString.isNullOrBlank()) {
                    emptyList() // Return empty list if no data saved
                } else {
                    try {
                        // Deserialize the JSON string back into a list of states
                        json.decodeFromString(ListSerializer(FreeFormItemState.serializer()), jsonString)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deserializing layout $layoutId: ${e.message}", e)
                        emptyList() // Return empty list on error
                    }
                }
            }
            .distinctUntilChanged() // Only emit when the list content actually changes
    }

    /**
     * Gets default items for a given layout ID.
     * TODO: Implement actual default item logic if needed.
     */
    fun getDefaultItemsForLayout(layoutId: String): List<FreeFormItemState> {
        Log.d(TAG, "Providing empty default items for layoutId: $layoutId")
        // Replace with actual default item creation logic based on layoutId if necessary
        return emptyList()
    }


    /**
     * Saves the current state (list of items) for a specific layout ID to DataStore.
     */
    suspend fun saveLayout(layoutId: String, items: List<FreeFormItemState>) {
        val key = layoutPreferenceKey(layoutId)
        try {
            // Serialize the list of states into a JSON string
            val jsonString = json.encodeToString(ListSerializer(FreeFormItemState.serializer()), items)
            context.layoutDataStore.edit { preferences -> // Use imported function
                preferences[key] = jsonString
            }
            Log.d(TAG,"Saved layout $layoutId state.")
        } catch (e: Exception) {
            Log.e(TAG,"Error serializing/saving layout $layoutId: ${e.message}", e)
            // Handle error appropriately (e.g., log, show message)
        }
    }

    /**
     * Retrieves the current layout state once. Useful for initial loading if needed,
     * though using the Flow is generally preferred. Returns empty list if not found or on error.
     */
    suspend fun getLayoutOnce(layoutId: String): List<FreeFormItemState> {
        return try {
            getLayoutFlow(layoutId).first() // Get the first emission from the flow
        } catch (e: Exception) {
            Log.e(TAG, "Error getting layout once for $layoutId", e)
            emptyList()
        }
    }
}
