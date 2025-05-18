package com.ongxeno.android.starbuttonbox.datasource

import ButtonEntity
import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ongxeno.android.starbuttonbox.data.FreeFormItemState
import com.ongxeno.android.starbuttonbox.data.FreeFormItemType
import com.ongxeno.android.starbuttonbox.data.LayoutType
import com.ongxeno.android.starbuttonbox.datasource.room.ButtonDao
import com.ongxeno.android.starbuttonbox.datasource.room.LayoutDao
import com.ongxeno.android.starbuttonbox.datasource.room.LayoutEntity
import com.ongxeno.android.starbuttonbox.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

// DataStore for selectedLayoutIndex only
private val Context.selectedLayoutIndexDataStore: DataStore<Preferences> by preferencesDataStore(name = "selected_layout_prefs")

/**
 * Repository responsible for managing layout definitions (now LayoutEntity),
 * their order, associated buttons (ButtonEntity), and selection state.
 * Uses Room for persistence of layouts and buttons, and DataStore for selected index.
 *
 * @param context Application context.
 * @param appScope Application-level CoroutineScope for managing StateFlows.
 * @param layoutDao DAO for layout operations.
 * @param buttonDao DAO for button operations.
 * @param settingDatasource Datasource for settings like isFirstLaunch.
 */
@Singleton
class LayoutRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope,
    private val layoutDao: LayoutDao,
    private val buttonDao: ButtonDao,
    private val settingDatasource: SettingDatasource // For isFirstLaunch check
) {

    private val tag = "LayoutRepository"

    private object PrefKeys {
        val SELECTED_LAYOUT_INDEX = intPreferencesKey("selected_layout_index_v2") // v2 to avoid conflict if old key exists
    }

    // --- Flows for Data ---

    /** Flow for the index of the currently selected layout/tab. */
    val selectedLayoutIndexFlow: Flow<Int> = context.selectedLayoutIndexDataStore.data
        .map { preferences -> preferences[PrefKeys.SELECTED_LAYOUT_INDEX] ?: 0 }
        .catch { e ->
            Log.e(tag, "Error reading selected layout index", e); emit(0)
        }
        .stateIn(appScope, SharingStarted.WhileSubscribed(5000), 0)


    /**
     * Flow providing the ordered list of **all** LayoutEntity objects.
     * Initial value is an empty list.
     */
    val allLayoutsFlow: Flow<List<LayoutEntity>> = layoutDao.getAllLayoutsOrdered()
        .stateIn(
            scope = appScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * Flow providing the ordered list of **enabled** LayoutEntity objects.
     * Initial value is an empty list.
     */
    val enabledLayoutsFlow: Flow<List<LayoutEntity>> = allLayoutsFlow
        .map { allLayouts -> allLayouts.filter { it.isEnabled } }
        .stateIn(
            scope = appScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Provides a Flow of FreeFormItemState list for a specific layout ID. */
    fun getLayoutItemsFlow(layoutId: String): Flow<List<FreeFormItemState>> {
        return buttonDao.getButtonsForLayout(layoutId)
            .map { buttonEntities ->
                buttonEntities.map { entity ->
                    // Map ButtonEntity to FreeFormItemState
                    FreeFormItemState(
                        id = entity.id,
                        type = try { FreeFormItemType.valueOf(entity.buttonTypeString) } catch (e: IllegalArgumentException) { FreeFormItemType.MOMENTARY_BUTTON },
                        text = entity.label,
                        macroId = entity.macroId,
                        gridCol = entity.gridCol,
                        gridRow = entity.gridRow,
                        gridWidth = entity.gridWidth,
                        gridHeight = entity.gridHeight,
                        textSizeSp = entity.labelSizeSp,
                        backgroundColorHex = entity.backgroundColorHex
                        // orderInLayout is part of ButtonEntity, not directly in FreeFormItemState
                    )
                }
            }
            .distinctUntilChanged()
            .catch { e -> Log.e(tag, "Error in getLayoutItemsFlow for $layoutId", e); emit(emptyList()) }
            .stateIn(appScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }


    // --- Suspend Functions for Saving Data ---

    /** Saves the order of layout IDs by updating their orderIndex. */
    suspend fun saveLayoutOrder(orderedIds: List<String>) {
        try {
            layoutDao.updateLayoutOrder(orderedIds)
            Log.i(tag, "Saved new layout order via DAO: $orderedIds")
        } catch (e: Exception) {
            Log.e(tag, "Error saving layout order via DAO", e)
        }
    }

    /** Saves the index of the selected layout. */
    suspend fun saveSelectedLayoutIndex(index: Int) {
        try {
            context.selectedLayoutIndexDataStore.edit { prefs -> prefs[PrefKeys.SELECTED_LAYOUT_INDEX] = index }
            Log.i(tag, "Selected layout index saved: $index")
        } catch (e: IOException) {
            Log.e(tag, "Error saving selected layout index", e)
        }
    }

    /** Updates a single layout definition. */
    suspend fun updateLayoutEntity(layoutEntity: LayoutEntity) {
        try {
            layoutDao.updateLayout(layoutEntity)
            Log.i(tag, "Updated layout entity for '${layoutEntity.id}'.")
        } catch(e: Exception) {
            Log.e(tag, "Error updating layout entity for '${layoutEntity.id}'", e)
        }
    }

    /** Toggles the isEnabled status of a specific layout. */
    suspend fun toggleLayoutEnabled(layoutId: String) {
        try {
            val currentLayout = layoutDao.getLayoutById(layoutId).firstOrNull()
            if (currentLayout != null) {
                val updatedLayout = currentLayout.copy(isEnabled = !currentLayout.isEnabled)
                layoutDao.updateLayout(updatedLayout)
                Log.i(tag, "Toggled isEnabled for layout '$layoutId' to ${updatedLayout.isEnabled}.")
            } else {
                Log.w(tag, "Layout ID '$layoutId' not found for toggling enabled status.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error accessing/updating DB for toggleLayoutEnabled", e)
        }
    }

    /** Saves the FreeForm items for a specific layout ID. */
    suspend fun saveLayoutItems(layoutId: String, items: List<FreeFormItemState>) {
        try {
            val layout = layoutDao.getLayoutById(layoutId).firstOrNull()
            if (layout == null) {
                Log.e(tag, "Cannot save items for non-existent layout ID: $layoutId")
                return
            }
            if (LayoutType.valueOf(layout.layoutTypeString) != LayoutType.FREE_FORM) {
                Log.e(tag, "Cannot save items for layout '$layoutId' - it's not a FREE_FORM layout.")
                return
            }

            val buttonEntities = items.mapIndexed { index, itemState ->
                ButtonEntity(
                    id = itemState.id,
                    layoutId = layoutId,
                    gridCol = itemState.gridCol,
                    gridRow = itemState.gridRow,
                    gridWidth = itemState.gridWidth,
                    gridHeight = itemState.gridHeight,
                    buttonTypeString = itemState.type.name,
                    macroId = itemState.macroId,
                    label = itemState.text,
                    labelSizeSp = itemState.textSizeSp,
                    backgroundColorHex = itemState.backgroundColorHex,
                    orderInLayout = index // Maintain order from the list
                )
            }
            // Perform in transaction for atomicity
            buttonDao.deleteButtonsByLayoutId(layoutId) // Clear old buttons
            buttonDao.insertAllButtons(buttonEntities) // Insert new buttons
            Log.i(tag, "Saved ${buttonEntities.size} items for FreeForm layout '$layoutId'.")
        } catch (e: Exception) {
            Log.e(tag, "Error saving layout items for '$layoutId'", e)
        }
    }

    /** Deletes a layout by its ID. Associated buttons are deleted by cascade. */
    suspend fun deleteLayout(layoutId: String) {
        try {
            val affectedRows = layoutDao.deleteLayoutById(layoutId)
            if (affectedRows > 0) {
                Log.i(tag, "Successfully deleted layout '$layoutId' and its buttons (cascade).")
                // Adjust selected index if the deleted layout was selected or before the selected one
                val currentSelectedIndex = selectedLayoutIndexFlow.first()
                val allLayoutsAfterDelete = allLayoutsFlow.first()
                if (currentSelectedIndex >= allLayoutsAfterDelete.size && allLayoutsAfterDelete.isNotEmpty()) {
                    saveSelectedLayoutIndex(allLayoutsAfterDelete.size - 1)
                } else if (allLayoutsAfterDelete.isEmpty()) {
                    saveSelectedLayoutIndex(0)
                }
            } else {
                Log.w(tag, "Attempted to delete non-existent layout ID: $layoutId")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error deleting layout '$layoutId'", e)
        }
    }

    /** Adds a new layout definition and its buttons (if any). */
    suspend fun addLayout(
        title: String,
        layoutType: LayoutType,
        iconName: String,
        initialButtons: List<FreeFormItemState>? = null // For FREE_FORM layouts
    ) {
        try {
            val currentLayouts = allLayoutsFlow.first()
            val newOrderIndex = currentLayouts.size // Append to the end

            val newLayoutEntity = LayoutEntity(
                id = if (layoutType == LayoutType.FREE_FORM) "freeform_${UUID.randomUUID()}" else layoutType.name.lowercase(),
                title = title,
                layoutTypeString = layoutType.name,
                iconName = iconName,
                isEnabled = true,
                isUserDefined = layoutType == LayoutType.FREE_FORM, // Only FreeForm is user-defined for now
                isDeletable = layoutType == LayoutType.FREE_FORM,   // Only FreeForm is deletable
                orderIndex = newOrderIndex
            )
            layoutDao.insertLayout(newLayoutEntity)
            Log.i(tag, "Added new layout: ${newLayoutEntity.title} (ID: ${newLayoutEntity.id})")

            if (layoutType == LayoutType.FREE_FORM && initialButtons != null) {
                saveLayoutItems(newLayoutEntity.id, initialButtons)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error adding layout '$title'", e)
        }
    }

    /** Adds the default layouts to the Room database if it's the first launch and DB is empty. */
    suspend fun addDefaultLayoutsIfFirstLaunch() {
        val isFirstLaunch = settingDatasource.isFirstLaunchFlow.first()
        if (isFirstLaunch) {
            val currentLayouts = layoutDao.getAllLayoutsOrdered().first()
            if (currentLayouts.isEmpty()) {
                Log.i(tag, "First launch and no layouts in DB. Populating default layouts.")
                try {
                    // Normal Flight Layout
                    val normalFlight = LayoutEntity(
                        id = "normal_flight",
                        title = "Flight Controls",
                        layoutTypeString = LayoutType.NORMAL_FLIGHT.name,
                        iconName = "RocketLaunch", // Updated icon name
                        isEnabled = true, // Enable by default
                        isUserDefined = false,
                        isDeletable = false,
                        orderIndex = 0
                    )
                    layoutDao.insertLayout(normalFlight)

                    // Auto Drag & Drop Layout
                    val autoDragDrop = LayoutEntity(
                        id = "auto_drag_drop",
                        title = "Auto Drag",
                        layoutTypeString = LayoutType.AUTO_DRAG_AND_DROP.name,
                        iconName = "Mouse", // Updated icon name
                        isEnabled = true, // Enable by default
                        isUserDefined = false,
                        isDeletable = false,
                        orderIndex = 1
                    )
                    layoutDao.insertLayout(autoDragDrop)

                    // Example FreeForm Layout (Optional - can be added by user)
                    val exampleFreeFormId = "freeform_example_${UUID.randomUUID()}"
                    val exampleFreeForm = LayoutEntity(
                        id = exampleFreeFormId,
                        title = "My First Panel",
                        layoutTypeString = LayoutType.FREE_FORM.name,
                        iconName = "DashboardCustomize",
                        isEnabled = true,
                        isUserDefined = true,
                        isDeletable = true,
                        orderIndex = 2
                    )
                    layoutDao.insertLayout(exampleFreeForm)
                    // Add some default buttons to this example FreeForm layout
                    val defaultButtons = listOf(
                        FreeFormItemState(id = exampleFreeFormId, text = "Button 1", gridCol = 0, gridRow = 0, gridWidth = 10, gridHeight = 4, macroId = null),
                        FreeFormItemState(id = exampleFreeFormId, text = "Button 2", gridCol = 10, gridRow = 0, gridWidth = 10, gridHeight = 4, macroId = null)
                    )
                    saveLayoutItems(exampleFreeFormId, defaultButtons)


                    Log.i(tag, "Default layouts populated into Room DB.")
                    // SplashViewModel will call settingDatasource.setFirstLaunchCompleted()
                } catch (e: Exception) {
                    Log.e(tag, "Error populating default layouts into Room DB", e)
                }
            } else {
                Log.d(tag, "Not first launch or layouts already exist in DB. Skipping default layout population.")
            }
        } else {
            Log.d(tag, "Not first launch. Skipping default layout population.")
        }
    }
}
