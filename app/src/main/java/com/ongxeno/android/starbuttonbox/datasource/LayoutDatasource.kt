package com.ongxeno.android.starbuttonbox.datasource

import android.content.Context
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

// Define the DataStore instance at the top level
private val Context.layoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "freeform_layouts")

/**
 * Handles saving and loading of FreeFormLayout states using Jetpack DataStore.
 * Default layout logic is handled by the FreeFormLayout composable itself.
 */
class LayoutDatasource(private val context: Context) {

    // Configure Kotlinx JSON serialization
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
    }

    // Function to create a Preferences key for a given layout ID
    private fun layoutPreferenceKey(layoutId: String): Preferences.Key<String> {
        return stringPreferencesKey("layout_$layoutId")
    }

    /**
     * Provides a Flow of the layout state (list of items) for a specific layout ID.
     * Emits the list whenever it changes in DataStore.
     * Returns an empty list if no data is found for the ID.
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
                        println("Error deserializing layout $layoutId: ${e.message}")
                        emptyList() // Return empty list on error
                    }
                }
            }
            .distinctUntilChanged() // Only emit when the list content actually changes
    }

    /**
     * Saves the current state (list of items) for a specific layout ID to DataStore.
     */
    suspend fun saveLayout(layoutId: String, items: List<FreeFormItemState>) {
        val key = layoutPreferenceKey(layoutId)
        try {
            // Serialize the list of states into a JSON string
            val jsonString = json.encodeToString(ListSerializer(FreeFormItemState.serializer()), items)
            context.layoutDataStore.edit { preferences ->
                preferences[key] = jsonString
            }
            println("Saved layout $layoutId state.")
        } catch (e: Exception) {
            println("Error serializing layout $layoutId: ${e.message}")
            // Handle error appropriately (e.g., log, show message)
        }
    }

    /**
     * Retrieves the current layout state once. Useful for initial loading if needed,
     * though using the Flow is generally preferred. Returns empty list if not found.
     */
    suspend fun getLayoutOnce(layoutId: String): List<FreeFormItemState> {
        return getLayoutFlow(layoutId).first() // Get the first emission from the flow
    }

    // --- Companion Object Removed ---
    // companion object { ... }
}
