package com.ongxeno.android.starbuttonbox.datasource

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
import com.ongxeno.android.starbuttonbox.datasource.room.ButtonEntity
import com.ongxeno.android.starbuttonbox.datasource.room.LayoutDao
import com.ongxeno.android.starbuttonbox.datasource.room.LayoutEntity
import com.ongxeno.android.starbuttonbox.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.selectedLayoutIndexDataStore: DataStore<Preferences> by preferencesDataStore(name = "selected_layout_prefs")

@Singleton
class LayoutRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope,
    private val layoutDao: LayoutDao,
    private val buttonDao: ButtonDao,
    private val settingDatasource: SettingDatasource
) {

    private val tag = "LayoutRepository"

    private object PrefKeys {
        val SELECTED_LAYOUT_INDEX = intPreferencesKey("selected_layout_index_v2")
    }

    val selectedLayoutIndexFlow: Flow<Int> = context.selectedLayoutIndexDataStore.data
        .map { preferences -> preferences[PrefKeys.SELECTED_LAYOUT_INDEX] ?: 0 }
        .catch { e -> Log.e(tag, "Error reading selected layout index", e); emit(0) }
        .stateIn(appScope, SharingStarted.WhileSubscribed(5000), 0)

    val allLayoutsFlow: Flow<List<LayoutEntity>> = layoutDao.getAllLayoutsOrdered()
        .stateIn(scope = appScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val enabledLayoutsFlow: Flow<List<LayoutEntity>> = allLayoutsFlow
        .map { allLayouts -> allLayouts.filter { it.isEnabled } }
        .stateIn(scope = appScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    /**
     * Provides a COLD Flow of FreeFormItemState list for a specific layout ID.
     * This ensures that collectors like .first() will trigger a fresh query.
     */
    fun getLayoutItemsFlow(layoutId: String): Flow<List<FreeFormItemState>> {
        Log.d(tag, "getLayoutItemsFlow: Called for layoutId: $layoutId (returning cold flow)")
        return buttonDao.getButtonsForLayout(layoutId) // This is a Flow from Room
            .map { buttonEntities ->
                Log.d(tag, "getLayoutItemsFlow: DAO emitted ${buttonEntities.size} ButtonEntities for layoutId: $layoutId")
                buttonEntities.map { entity ->
                    FreeFormItemState(
                        id = entity.id,
                        type = try { FreeFormItemType.valueOf(entity.buttonTypeString) } catch (e: IllegalArgumentException) {
                            Log.w(tag, "Invalid buttonTypeString '${entity.buttonTypeString}' for button ${entity.id}, defaulting to MOMENTARY_BUTTON.")
                            FreeFormItemType.MOMENTARY_BUTTON
                        },
                        text = entity.label,
                        macroId = entity.macroId,
                        gridCol = entity.gridCol,
                        gridRow = entity.gridRow,
                        gridWidth = entity.gridWidth,
                        gridHeight = entity.gridHeight,
                        textSizeSp = entity.labelSizeSp,
                        backgroundColorHex = entity.backgroundColorHex
                    )
                }
            }
            .distinctUntilChanged() // Still useful to avoid unnecessary downstream processing if the list content hasn't changed
            .catch { e -> Log.e(tag, "Error in getLayoutItemsFlow for $layoutId", e); emit(emptyList()) }
        // REMOVED .stateIn() here. MainViewModel will use .stateIn() on this cold flow.
    }

    suspend fun saveLayoutOrder(orderedIds: List<String>) {
        try {
            layoutDao.updateLayoutOrder(orderedIds)
            Log.i(tag, "Saved new layout order via DAO: $orderedIds")
        } catch (e: Exception) {
            Log.e(tag, "Error saving layout order via DAO", e)
        }
    }

    suspend fun saveSelectedLayoutIndex(index: Int) {
        try {
            context.selectedLayoutIndexDataStore.edit { prefs -> prefs[PrefKeys.SELECTED_LAYOUT_INDEX] = index }
            Log.i(tag, "Selected layout index saved: $index")
        } catch (e: IOException) {
            Log.e(tag, "Error saving selected layout index", e)
        }
    }

    suspend fun updateLayoutEntity(layoutEntity: LayoutEntity) {
        try {
            layoutDao.updateLayout(layoutEntity)
            Log.i(tag, "Updated layout entity for '${layoutEntity.id}'.")
        } catch(e: Exception) {
            Log.e(tag, "Error updating layout entity for '${layoutEntity.id}'", e)
        }
    }

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

    suspend fun saveLayoutItems(layoutId: String, items: List<FreeFormItemState>) {
        Log.d(tag, "saveLayoutItems: Called for layoutId: $layoutId with ${items.size} items.")
        if (items.isNotEmpty()) {
            items.forEachIndexed { index, item ->
                Log.d(tag, "saveLayoutItems: Item $index to save: ID=${item.id}, Text='${item.text}', Macro='${item.macroId}', Pos=(${item.gridCol},${item.gridRow}), Size=(${item.gridWidth}x${item.gridHeight})")
            }
        }

        try {
            val layout = layoutDao.getLayoutById(layoutId).firstOrNull()
            if (layout == null) {
                Log.e(tag, "saveLayoutItems: Cannot save items, LayoutEntity not found for ID: $layoutId")
                return
            }
            if (LayoutType.valueOf(layout.layoutTypeString) != LayoutType.FREE_FORM) {
                Log.e(tag, "saveLayoutItems: Cannot save items for layout '$layoutId' - it's not a FREE_FORM layout. Type: ${layout.layoutTypeString}")
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
                    orderInLayout = index
                )
            }
            Log.d(tag, "saveLayoutItems: Converted to ${buttonEntities.size} ButtonEntities for layoutId: $layoutId")

            buttonDao.deleteButtonsByLayoutId(layoutId)
            Log.d(tag, "saveLayoutItems: Deleted old buttons for layoutId: $layoutId")
            if (buttonEntities.isNotEmpty()) {
                buttonDao.insertAllButtons(buttonEntities)
                Log.i(tag, "saveLayoutItems: Inserted ${buttonEntities.size} new buttons for layoutId: $layoutId.")
            } else {
                Log.i(tag, "saveLayoutItems: No new buttons to insert for layoutId: $layoutId (items list was empty).")
            }
        } catch (e: Exception) {
            Log.e(tag, "saveLayoutItems: Error saving layout items for '$layoutId'", e)
        }
    }

    suspend fun deleteLayout(layoutId: String) {
        try {
            val affectedRows = layoutDao.deleteLayoutById(layoutId)
            if (affectedRows > 0) {
                Log.i(tag, "Successfully deleted layout '$layoutId' and its buttons (cascade).")
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

    suspend fun addLayout(
        title: String,
        layoutType: LayoutType,
        iconName: String,
        initialButtons: List<FreeFormItemState>? = null
    ) {
        try {
            val currentLayouts = allLayoutsFlow.first()
            val newOrderIndex = currentLayouts.size

            val newLayoutEntity = LayoutEntity(
                id = if (layoutType == LayoutType.FREE_FORM) "freeform_${UUID.randomUUID()}" else layoutType.name.lowercase(),
                title = title,
                layoutTypeString = layoutType.name,
                iconName = iconName,
                isEnabled = true,
                isUserDefined = layoutType == LayoutType.FREE_FORM,
                isDeletable = layoutType == LayoutType.FREE_FORM,
                orderIndex = newOrderIndex
            )
            layoutDao.insertLayout(newLayoutEntity)
            Log.i(tag, "Added new layout: ${newLayoutEntity.title} (ID: ${newLayoutEntity.id}) with orderIndex: $newOrderIndex")

            if (layoutType == LayoutType.FREE_FORM && initialButtons != null) {
                Log.d(tag, "addLayout: Saving ${initialButtons.size} initial buttons for new FreeForm layout ${newLayoutEntity.id}")
                saveLayoutItems(newLayoutEntity.id, initialButtons)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error adding layout '$title'", e)
        }
    }

    suspend fun addDefaultLayoutsIfFirstLaunch() {
        val isFirstLaunch = settingDatasource.isFirstLaunchFlow.first()
        if (isFirstLaunch) {
            val currentLayouts = layoutDao.getAllLayoutsOrdered().first()
            if (currentLayouts.isEmpty()) {
                Log.i(tag, "First launch and no layouts in DB. Populating default layouts.")
                try {
                    val normalFlight = LayoutEntity(id = "normal_flight", title = "Flight Controls", layoutTypeString = LayoutType.NORMAL_FLIGHT.name, iconName = "RocketLaunch", isEnabled = true, isUserDefined = false, isDeletable = false, orderIndex = 0)
                    layoutDao.insertLayout(normalFlight)

                    val autoDragDrop = LayoutEntity(id = "auto_drag_drop", title = "Auto Drag", layoutTypeString = LayoutType.AUTO_DRAG_AND_DROP.name, iconName = "Mouse", isEnabled = true, isUserDefined = false, isDeletable = false, orderIndex = 1)
                    layoutDao.insertLayout(autoDragDrop)

                    val exampleFreeFormId = "freeform_example_${UUID.randomUUID()}"
                    val exampleFreeForm = LayoutEntity(id = exampleFreeFormId, title = "My First Panel", layoutTypeString = LayoutType.FREE_FORM.name, iconName = "DashboardCustomize", isEnabled = true, isUserDefined = true, isDeletable = true, orderIndex = 2)
                    layoutDao.insertLayout(exampleFreeForm)

                    val defaultButtons = listOf(
                        FreeFormItemState(id = "btn_ex1_${UUID.randomUUID()}", text = "Button 1", gridCol = 0, gridRow = 0, gridWidth = 10, gridHeight = 4, macroId = null),
                        FreeFormItemState(id = "btn_ex2_${UUID.randomUUID()}", text = "Button 2", gridCol = 10, gridRow = 0, gridWidth = 10, gridHeight = 4, macroId = null)
                    )
                    Log.d(tag, "addDefaultLayoutsIfFirstLaunch: Attempting to save ${defaultButtons.size} default buttons for layout $exampleFreeFormId")
                    saveLayoutItems(exampleFreeFormId, defaultButtons)

                    Log.i(tag, "Default layouts populated into Room DB.")
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
