package com.ongxeno.android.starbuttonbox.datasource

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ongxeno.android.starbuttonbox.data.FreeFormItemState
import com.ongxeno.android.starbuttonbox.data.LayoutDefinition // Use LayoutDefinition
import com.ongxeno.android.starbuttonbox.data.LayoutType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Define a single DataStore for all layout-related preferences
private val Context.layoutDataStore: DataStore<Preferences> by preferencesDataStore(name = "layout_prefs")

/**
 * Repository responsible for managing layout definitions, order, content (FreeForm items),
 * and selection state using DataStore for persistence.
 * Provides flows of LayoutDefinition objects; mapping to LayoutInfo happens closer to the UI.
 * Starts with an empty state on the very first run until defaults are explicitly added.
 *
 * @param context Application context.
 * @param externalScope Application-level CoroutineScope for managing StateFlows.
 */
@Singleton
class LayoutRepository @Inject constructor(
    private val context: Context,
    private val externalScope: CoroutineScope
) {

    private val TAG = "LayoutRepository"

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
        encodeDefaults = true
        classDiscriminator = "_type"
    }

    private object PrefKeys {
        val LAYOUT_DEFINITIONS = stringPreferencesKey("layout_definitions_map")
        val LAYOUT_ORDER_IDS = stringPreferencesKey("layout_order_ids")
        val SELECTED_LAYOUT_INDEX = intPreferencesKey("selected_layout_index")
    }

    // --- Flows for Data ---

    /** Flow for the ordered list of layout IDs. Returns empty list if not set. */
    private val layoutOrderIdsFlow: Flow<List<String>> = context.layoutDataStore.data
        .map { prefs ->
            prefs[PrefKeys.LAYOUT_ORDER_IDS]?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
        }
        .catch { e ->
            Log.e(TAG, "Error reading layout order", e); emit(emptyList()) // Emit empty on error too
        }

    /** Flow for the map of layout definitions. Returns empty map if not set. */
    // Make this public to allow ViewModel to check initial state
    val layoutDefinitionsFlow: Flow<Map<String, LayoutDefinition>> = context.layoutDataStore.data
        .map { prefs ->
            val jsonString = prefs[PrefKeys.LAYOUT_DEFINITIONS]
            if (jsonString.isNullOrBlank()) {
                Log.d(TAG, "No layout definitions found in DataStore, returning empty map.")
                emptyMap()
            } else {
                try {
                    json.decodeFromString(MapSerializer(String.serializer(), LayoutDefinition.serializer()), jsonString)
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding layout definitions, returning empty map: ${e.message}")
                    emptyMap() // Return empty map on decoding error
                }
            }
        }
        .catch { e ->
            Log.e(TAG, "Error reading layout definitions", e); emit(emptyMap()) // Emit empty on read error
        }

    /** Flow for the index of the currently selected layout/tab. */
    val selectedLayoutIndexFlow: Flow<Int> = context.layoutDataStore.data
        .map { preferences -> preferences[PrefKeys.SELECTED_LAYOUT_INDEX] ?: 0 }
        .catch { e ->
            Log.e(TAG, "Error reading selected layout index", e); emit(0)
        }

    /**
     * Flow providing the ordered list of **all** LayoutDefinition objects.
     * Initial value is an empty list.
     */
    val allLayoutDefinitionsFlow: Flow<List<LayoutDefinition>> = combine(
        layoutOrderIdsFlow,
        layoutDefinitionsFlow
    ) { orderedIds, definitionsMap ->
        orderedIds.mapNotNull { id -> definitionsMap[id] }
    }.stateIn(
        scope = externalScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList() // Start empty
    )

    /**
     * Flow providing the ordered list of **enabled** LayoutDefinition objects.
     * Initial value is an empty list.
     */
    val enabledLayoutDefinitionsFlow: Flow<List<LayoutDefinition>> = allLayoutDefinitionsFlow
        .map { allDefs -> allDefs.filter { it.isEnabled } }
        .stateIn(
            scope = externalScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList() // Start empty
        )


    /** Provides a Flow of the FreeForm items for a specific layout ID. */
    fun getLayoutItemsFlow(layoutId: String): Flow<List<FreeFormItemState>> {
        return layoutDefinitionsFlow // Use the base flow here
            .mapNotNull { definitionsMap -> definitionsMap[layoutId] }
            .map { definition ->
                if (definition.layoutType == LayoutType.FREE_FORM && !definition.layoutItemsJson.isNullOrBlank()) {
                    try {
                        json.decodeFromString(ListSerializer(FreeFormItemState.serializer()), definition.layoutItemsJson)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deserializing items for layout $layoutId: ${e.message}", e)
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }
            .distinctUntilChanged()
            .catch { e -> Log.e(TAG, "Error in getLayoutItemsFlow for $layoutId", e); emit(emptyList()) }
    }


    // --- Suspend Functions for Saving Data ---

    /** Saves the order of layout IDs. */
    suspend fun saveLayoutOrder(orderedIds: List<String>) {
        try {
            context.layoutDataStore.edit { prefs ->
                prefs[PrefKeys.LAYOUT_ORDER_IDS] = orderedIds.joinToString(",")
            }
            Log.i(TAG, "Saved new layout order: $orderedIds")
        } catch (e: IOException) {
            Log.e(TAG, "Error saving layout order", e)
        }
    }

    /** Saves the entire map of layout definitions. */
    private suspend fun saveLayoutDefinitions(definitions: Map<String, LayoutDefinition>) {
        try {
            val jsonString = json.encodeToString(MapSerializer(String.serializer(), LayoutDefinition.serializer()), definitions)
            context.layoutDataStore.edit { prefs ->
                prefs[PrefKeys.LAYOUT_DEFINITIONS] = jsonString
            }
            Log.i(TAG, "Saved layout definitions map.")
        } catch (e: Exception) { // Catch broader exceptions during serialization/saving
            Log.e(TAG, "Error saving layout definitions map", e)
        }
    }

    /** Saves the index of the selected layout. */
    suspend fun saveSelectedLayoutIndex(index: Int) {
        try {
            context.layoutDataStore.edit { prefs -> prefs[PrefKeys.SELECTED_LAYOUT_INDEX] = index }
        } catch (e: IOException) {
            Log.e(TAG, "Error saving selected layout index", e)
        }
    }

    /** Updates a single layout definition in the stored map. */
    suspend fun updateLayoutDefinition(definition: LayoutDefinition) {
        try {
            // Use transform which provides atomicity guarantees
            context.layoutDataStore.edit { prefs ->
                val currentJson = prefs[PrefKeys.LAYOUT_DEFINITIONS]
                val currentDefinitions = if (currentJson.isNullOrBlank()) {
                    mutableMapOf()
                } else {
                    try {
                        json.decodeFromString(MapSerializer(String.serializer(), LayoutDefinition.serializer()), currentJson).toMutableMap()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error decoding definitions in update, starting fresh.", e)
                        mutableMapOf()
                    }
                }
                currentDefinitions[definition.id] = definition
                prefs[PrefKeys.LAYOUT_DEFINITIONS] = json.encodeToString(MapSerializer(String.serializer(), LayoutDefinition.serializer()), currentDefinitions)
                Log.i(TAG, "Updated layout definition for '${definition.id}'.")
            }
        } catch(e: Exception) {
            Log.e(TAG, "Error updating layout definition for '${definition.id}'", e)
        }
    }

    /** Toggles the isEnabled status of a specific layout. */
    suspend fun toggleLayoutEnabled(layoutId: String) {
        try {
            context.layoutDataStore.edit { prefs ->
                val currentJson = prefs[PrefKeys.LAYOUT_DEFINITIONS]
                if (currentJson.isNullOrBlank()) {
                    Log.w(TAG, "Cannot toggle enabled status, definitions are empty.")
                    return@edit
                }
                try {
                    val currentDefinitions = json.decodeFromString(MapSerializer(String.serializer(), LayoutDefinition.serializer()), currentJson).toMutableMap()
                    val currentDefinition = currentDefinitions[layoutId]
                    if (currentDefinition != null) {
                        val updatedDefinition = currentDefinition.copy(isEnabled = !currentDefinition.isEnabled)
                        currentDefinitions[layoutId] = updatedDefinition
                        prefs[PrefKeys.LAYOUT_DEFINITIONS] = json.encodeToString(MapSerializer(String.serializer(), LayoutDefinition.serializer()), currentDefinitions)
                        Log.i(TAG, "Toggled isEnabled for layout '$layoutId' to ${updatedDefinition.isEnabled}.")
                    } else {
                        Log.w(TAG, "Layout ID '$layoutId' not found for toggling enabled status.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error decoding/updating definitions for toggle", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing DataStore for toggleLayoutEnabled", e)
        }
    }


    /** Saves the FreeForm items for a specific layout ID. */
    suspend fun saveLayoutItems(layoutId: String, items: List<FreeFormItemState>) {
        try {
            context.layoutDataStore.edit { prefs ->
                val currentJson = prefs[PrefKeys.LAYOUT_DEFINITIONS]
                val currentDefinitions = if (currentJson.isNullOrBlank()) {
                    Log.e(TAG, "Cannot save items, definitions map is empty.")
                    return@edit
                } else {
                    try {
                        json.decodeFromString(MapSerializer(String.serializer(), LayoutDefinition.serializer()), currentJson).toMutableMap()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error decoding definitions in saveLayoutItems, aborting.", e)
                        return@edit
                    }
                }

                val currentDefinition = currentDefinitions[layoutId]
                if (currentDefinition == null) {
                    Log.e(TAG, "Cannot save items for non-existent layout ID: $layoutId")
                    return@edit
                }
                if (currentDefinition.layoutType != LayoutType.FREE_FORM) {
                    Log.e(TAG, "Cannot save items for layout '$layoutId' - it's not a FREE_FORM layout.")
                    return@edit
                }

                try {
                    val itemsJson = json.encodeToString(ListSerializer(FreeFormItemState.serializer()), items)
                    currentDefinitions[layoutId] = currentDefinition.copy(layoutItemsJson = itemsJson)
                    prefs[PrefKeys.LAYOUT_DEFINITIONS] = json.encodeToString(MapSerializer(String.serializer(), LayoutDefinition.serializer()), currentDefinitions)
                    Log.d(TAG, "Saved items for FreeForm layout '$layoutId'.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error serializing items for layout '$layoutId'", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing DataStore for saveLayoutItems", e)
        }
    }

    /** Deletes a layout by its ID (removes from order and definitions). */
    suspend fun deleteLayout(layoutId: String) {
        try {
            context.layoutDataStore.edit { prefs ->
                // Update Definitions
                val currentDefJson = prefs[PrefKeys.LAYOUT_DEFINITIONS]
                var definitionRemoved = false
                if (!currentDefJson.isNullOrBlank()) {
                    try {
                        val currentDefinitions = json.decodeFromString(MapSerializer(String.serializer(), LayoutDefinition.serializer()), currentDefJson).toMutableMap()
                        if (currentDefinitions.remove(layoutId) != null) {
                            prefs[PrefKeys.LAYOUT_DEFINITIONS] = json.encodeToString(MapSerializer(String.serializer(), LayoutDefinition.serializer()), currentDefinitions)
                            definitionRemoved = true
                            Log.i(TAG, "Removed definition for '$layoutId'.")
                        }
                    } catch (e: Exception) { Log.e(TAG, "Error decoding/updating definitions for delete", e) }
                }

                // Update Order
                val currentOrderStr = prefs[PrefKeys.LAYOUT_ORDER_IDS]
                var orderRemoved = false
                if (!currentOrderStr.isNullOrBlank()) {
                    val currentOrder = currentOrderStr.split(',').filter { it.isNotBlank() }.toMutableList()
                    if (currentOrder.remove(layoutId)) {
                        prefs[PrefKeys.LAYOUT_ORDER_IDS] = currentOrder.joinToString(",")
                        orderRemoved = true
                        Log.i(TAG, "Removed '$layoutId' from order.")
                    }
                }

                if (!definitionRemoved && !orderRemoved) {
                    Log.w(TAG, "Attempted to delete non-existent layout ID: $layoutId")
                } else {
                    Log.i(TAG, "Successfully deleted layout '$layoutId'.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting layout '$layoutId'", e)
        }
    }

    /** Adds a new layout definition and updates the order. */
    suspend fun addLayout(newDefinition: LayoutDefinition) {
        try {
            context.layoutDataStore.edit { prefs ->
                // Add to Definitions
                val currentDefJson = prefs[PrefKeys.LAYOUT_DEFINITIONS]
                val currentDefinitions = if (currentDefJson.isNullOrBlank()) {
                    mutableMapOf()
                } else {
                    try {
                        json.decodeFromString(MapSerializer(String.serializer(), LayoutDefinition.serializer()), currentDefJson).toMutableMap()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error decoding definitions in addLayout, starting fresh.", e)
                        mutableMapOf()
                    }
                }
                // Check for ID collision (should be unlikely with UUID, but good practice)
                if (currentDefinitions.containsKey(newDefinition.id)) {
                    Log.e(TAG, "Layout ID collision detected for '${newDefinition.id}'. Aborting add.")
                    return@edit // Or handle differently, e.g., generate new ID
                }
                currentDefinitions[newDefinition.id] = newDefinition
                prefs[PrefKeys.LAYOUT_DEFINITIONS] = json.encodeToString(MapSerializer(String.serializer(), LayoutDefinition.serializer()), currentDefinitions)
                Log.i(TAG, "Added layout definition for '${newDefinition.id}'.")

                // Add to Order (append to the end)
                val currentOrderStr = prefs[PrefKeys.LAYOUT_ORDER_IDS]
                val currentOrder = if (currentOrderStr.isNullOrBlank()) {
                    mutableListOf()
                } else {
                    currentOrderStr.split(',').filter { it.isNotBlank() }.toMutableList()
                }
                currentOrder.add(newDefinition.id)
                prefs[PrefKeys.LAYOUT_ORDER_IDS] = currentOrder.joinToString(",")
                Log.i(TAG, "Added '${newDefinition.id}' to layout order.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding layout '${newDefinition.id}'", e)
        }
    }


    /** Adds the default layouts to DataStore. Should only be called if layouts are confirmed empty. */
    suspend fun addDefaultLayouts() {
        Log.i(TAG, "Adding default layouts to DataStore.")
        val defaultDefs = getDefaultLayoutDefinitions()
        val defaultOrder = getDefaultLayoutOrderIds()
        // Use separate edits for clarity, though one edit block could work
        saveLayoutDefinitions(defaultDefs)
        saveLayoutOrder(defaultOrder)
        saveSelectedLayoutIndex(0) // Select the first default tab
    }

    // --- Default Data ---

    // Default order now only contains the single default layout ID
    private fun getDefaultLayoutOrderIds(): List<String> = listOf(
        "normal_flight"
    )

    // Default definitions now only contain "Normal Flight", set to disabled (hidden)
    private fun getDefaultLayoutDefinitions(): Map<String, LayoutDefinition> = mapOf(
        "normal_flight" to LayoutDefinition(
            id = "normal_flight",
            title = "Normal Flight",
            layoutType = LayoutType.NORMAL_FLIGHT,
            iconName = "Rocket",
            isEnabled = false, // Default to hidden
            isDeletable = false // Cannot delete the default built-in layout
        )
        // Removed other default layouts
    )
}