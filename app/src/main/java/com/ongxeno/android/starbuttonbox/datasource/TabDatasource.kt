/*
 * File: StarButtonBox/app/src/main/java/com/ongxeno/android/starbuttonbox/datasource/TabDatasource.kt
 * Modified to be a class and handle persistence of the selected tab index using DataStore.
 */
package com.ongxeno.android.starbuttonbox.datasource

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ongxeno.android.starbuttonbox.ui.layout.DemoLayout
import com.ongxeno.android.starbuttonbox.ui.layout.FreeFormLayout
import com.ongxeno.android.starbuttonbox.ui.layout.NormalFlightLayout
import com.ongxeno.android.starbuttonbox.ui.layout.PlaceholderLayout
import com.ongxeno.android.starbuttonbox.ui.model.TabInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Define the DataStore instance at the top level for Tab preferences
private val Context.tabDataStore: DataStore<Preferences> by preferencesDataStore(name = "tab_prefs")

/**
 * Provides the list of TabInfo objects and handles persistence for the selected tab index.
 */
class TabDatasource(private val context: Context) {

    // Define a key for storing the selected tab index
    private object PreferencesKeys {
        val SELECTED_TAB_INDEX = intPreferencesKey("selected_tab_index")
    }

    /**
     * A Flow representing the currently selected tab index stored in DataStore.
     * Defaults to 0 if no value is found.
     */
    val selectedTabIndexFlow: Flow<Int> = context.tabDataStore.data
        .map { preferences ->
            // Read the integer value associated with the key, defaulting to 0
            preferences[PreferencesKeys.SELECTED_TAB_INDEX] ?: 0
        }

    /**
     * Saves the given tab index to DataStore.
     * @param index The index of the tab to save.
     */
    suspend fun saveSelectedTabIndex(index: Int) {
        context.tabDataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_TAB_INDEX] = index
        }
    }

    /**
     * Returns the static list of tabs available in the application.
     */
    fun getTabs(): List<TabInfo> {
        // This list remains static for now
        return listOf(
            TabInfo(
                order = 0,
                title = "Normal Flight",
                icon = Icons.Filled.Rocket,
                content = { onCommand -> NormalFlightLayout(onCommand) }
            ),
            TabInfo(
                order = 1,
                title = "Free Form 1",
                icon = Icons.Filled.DashboardCustomize,
                content = @Composable { onCommand ->
                    FreeFormLayout(
                        layoutId = "freeform_1",
                        onCommand = onCommand
                    )
                }
            ),
            TabInfo(
                order = 2,
                title = "Free Form 2",
                icon = Icons.Filled.DashboardCustomize,
                content = @Composable { onCommand ->
                    FreeFormLayout(
                        layoutId = "freeform_2",
                        onCommand = onCommand
                    )
                }
            ),
            TabInfo(
                order = 3,
                title = "Salvage",
                icon = Icons.Filled.Recycling,
                content = { PlaceholderLayout("Salvage Layout Placeholder") }
            ),
            TabInfo(
                order = 4,
                title = "Mining",
                icon = Icons.Filled.Diamond,
                content = { PlaceholderLayout("Mining Layout Placeholder") }
            ),
            TabInfo(
                order = 5,
                title = "Combat",
                icon = Icons.Filled.LocalFireDepartment,
                content = { PlaceholderLayout("Combat Layout Placeholder") }
            ),
            TabInfo(
                order = 6,
                title = "Demo",
                icon = Icons.Filled.Widgets,
                content = { DemoLayout() }
            )
        ).sortedBy { it.order }
    }
}
