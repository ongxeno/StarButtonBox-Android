package com.ongxeno.android.starbuttonbox.ui.screen.managelayout // Or ui.manage_layouts

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ongxeno.android.starbuttonbox.data.ExportedLayout
import com.ongxeno.android.starbuttonbox.data.FreeFormItemState
import com.ongxeno.android.starbuttonbox.data.ImportResult
import com.ongxeno.android.starbuttonbox.data.LayoutDefinition
import com.ongxeno.android.starbuttonbox.data.LayoutType
import com.ongxeno.android.starbuttonbox.datasource.LayoutRepository
import com.ongxeno.android.starbuttonbox.utils.IconMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ManageLayoutsViewModel @Inject constructor(
    private val layoutRepository: LayoutRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _tag = "ManageLayoutsVM"

    // --- State Flows for UI ---
    val manageLayoutsState: StateFlow<List<ManageLayoutInfo>> =
        layoutRepository.allLayoutDefinitionsFlow
            .map { definitions ->
                definitions.map {
                    ManageLayoutInfo(
                        id = it.id,
                        title = it.title,
                        type = it.layoutType,
                        iconName = it.iconName,
                        isEnabled = it.isEnabled,
                        isDeletable = it.isDeletable
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dialog State ---
    private val _showAddEditLayoutDialog = MutableStateFlow(false)
    val showAddEditLayoutDialogState: StateFlow<Boolean> = _showAddEditLayoutDialog.asStateFlow()
    private val _layoutToEditState = mutableStateOf<ManageLayoutInfo?>(null)
    val layoutToEditState: State<ManageLayoutInfo?> = _layoutToEditState

    private val _showDeleteConfirmationDialog = MutableStateFlow(false)
    val showDeleteConfirmationDialogState: StateFlow<Boolean> = _showDeleteConfirmationDialog.asStateFlow()
    private val _layoutToDeleteState = mutableStateOf<ManageLayoutInfo?>(null)
    val layoutToDeleteState: State<ManageLayoutInfo?> = _layoutToDeleteState

    private val _importResult = MutableStateFlow<ImportResult>(ImportResult.Idle)
    val importResultState: StateFlow<ImportResult> = _importResult.asStateFlow()

    // --- Export State ---
    private val _layoutToExportId = MutableStateFlow<String?>(null)

    // JSON parser
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // --- Event Handlers (Moved from MainViewModel) ---

    /** Saves the new order of layouts. */
    fun saveLayoutOrder(orderedIds: List<String>) {
        viewModelScope.launch {
            layoutRepository.saveLayoutOrder(orderedIds)
        }
    }

    /** Toggles the enabled/visible state of a layout. */
    fun toggleLayoutEnabled(layoutId: String) {
        viewModelScope.launch {
            layoutRepository.toggleLayoutEnabled(layoutId)
        }
    }

    /** Initiates the layout deletion process. */
    fun requestDeleteLayout(layoutInfo: ManageLayoutInfo?) {
        _layoutToDeleteState.value = layoutInfo
        if (layoutInfo != null && layoutInfo.isDeletable) {
            _showDeleteConfirmationDialog.value = true
        } else if (layoutInfo != null && !layoutInfo.isDeletable) {
            Log.w(_tag, "Attempted to delete non-deletable layout: ${layoutInfo.id}")
        }
    }

    /** Confirms the deletion and calls the repository. */
    fun confirmDeleteLayout() {
        viewModelScope.launch {
            _layoutToDeleteState.value?.let { layoutToDelete ->
                Log.i(_tag, "Confirming deletion of layout: ${layoutToDelete.id}")
                layoutRepository.deleteLayout(layoutToDelete.id)
            } ?: Log.e(_tag, "Confirm delete called but layoutToDelete was null.")
            _layoutToDeleteState.value = null
            _showDeleteConfirmationDialog.value = false
        }
    }

    /** Cancels the layout deletion process. */
    fun cancelDeleteLayout() {
        _layoutToDeleteState.value = null
        _showDeleteConfirmationDialog.value = false
    }

    /** Shows the dialog to add a new layout. */
    fun requestAddLayout() {
        Log.d(_tag, "Requesting Add Layout Dialog")
        _layoutToEditState.value = null
        _showAddEditLayoutDialog.value = true
    }

    /** Shows the dialog to edit an existing layout. */
    fun requestEditLayout(layoutInfo: ManageLayoutInfo) {
        if (layoutInfo.type == LayoutType.FREE_FORM) {
            Log.d(_tag, "Requesting Edit Layout Dialog for ID: ${layoutInfo.id}")
            _layoutToEditState.value = layoutInfo
            _showAddEditLayoutDialog.value = true
        } else {
            Log.w(_tag, "Edit requested for non-editable layout type: ${layoutInfo.type} for ID: ${layoutInfo.id}")
        }
    }

    /** Creates or updates a layout based on user input from the dialog. */
    fun confirmSaveLayout(title: String, iconName: String, existingId: String?) {
        viewModelScope.launch {
            if (existingId == null) {
                // Add New Layout
                val newId = "freeform_${UUID.randomUUID()}"
                val newLayout = LayoutDefinition(
                    id = newId, title = title, layoutType = LayoutType.FREE_FORM,
                    iconName = iconName, isEnabled = true, isUserDefined = true,
                    isDeletable = true, layoutItemsJson = null
                )
                layoutRepository.addLayout(newLayout)
                Log.i(_tag, "Added new layout: $newLayout")
            } else {
                // Edit Existing Layout
                val currentDefinitions = layoutRepository.layoutDefinitionsFlow.first()
                val definitionToUpdate = currentDefinitions[existingId]
                if (definitionToUpdate != null) {
                    val updatedDefinition = definitionToUpdate.copy(title = title, iconName = iconName)
                    layoutRepository.updateLayoutDefinition(updatedDefinition)
                    Log.i(_tag, "Updated layout: $updatedDefinition")
                } else {
                    Log.e(_tag, "Attempted to edit non-existent layout ID: $existingId")
                }
            }
            _showAddEditLayoutDialog.value = false
            _layoutToEditState.value = null
        }
    }

    /** Cancels the add/edit layout operation. */
    fun cancelAddEditLayout() {
        _showAddEditLayoutDialog.value = false
        _layoutToEditState.value = null
    }

    /** Dismisses the import result dialog. */
    fun dismissImportResult() {
        _importResult.value = ImportResult.Idle
    }

    // --- Import / Export ---
    /** Stores the ID of the layout the user wants to export. */
    fun requestExportLayout(layoutId: String) {
        Log.d(_tag, "Requesting export for layout ID: $layoutId")
        _layoutToExportId.value = layoutId
    }

    /** Clears the stored layout ID after an export attempt. */
    fun clearExportRequest() {
        _layoutToExportId.value = null
    }

    /** Exports the layout identified by the current `_layoutToExportId` state to the given URI. */
    fun exportLayoutToFile(uri: Uri) {
        val layoutId = _layoutToExportId.value
        if (layoutId == null) { clearExportRequest(); return }

        viewModelScope.launch {
            Log.d(_tag, "Attempting to export layout '$layoutId' to URI: $uri")
            val definition = layoutRepository.allLayoutDefinitionsFlow.first().find { it.id == layoutId }

            if (definition == null || definition.layoutType != LayoutType.FREE_FORM) {
                Log.e(_tag, "Export failed: Layout not found or not FreeForm for ID '$layoutId'")
                // TODO: Show error message via state
            } else {
                val items = layoutRepository.getLayoutItemsFlow(layoutId).first()
                val exportedData = ExportedLayout(definition = definition, items = items)
                try {
                    val jsonString = json.encodeToString(ExportedLayout.serializer(), exportedData)
                    appContext.contentResolver.openOutputStream(uri)?.use { o -> OutputStreamWriter(o).use { it.write(jsonString) } }
                    Log.i(_tag, "Successfully exported layout '$layoutId' to $uri")
                    // TODO: Show success message via state
                } catch (e: Exception) {
                    Log.e(_tag, "Export failed for layout '$layoutId' to $uri", e)
                    // TODO: Show error message via state
                }
            }
            clearExportRequest()
        }
    }

    /** Imports a layout from a chosen file URI. */
    fun importLayoutFromFile(uri: Uri) {
        viewModelScope.launch {
            Log.d(_tag, "Attempting to import layout from URI: $uri")
            _importResult.value = ImportResult.Idle
            try {
                val jsonString = appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader -> reader.readText() }
                }

                if (jsonString.isNullOrBlank()) { return@launch }
                val importedLayout = json.decodeFromString(ExportedLayout.serializer(), jsonString)

                // Validation
                if (importedLayout.definition.layoutType != LayoutType.FREE_FORM) { return@launch }
                if (importedLayout.definition.title.isBlank()) { return@launch }
                if (IconMapper.getIconVector(importedLayout.definition.iconName) == Icons.AutoMirrored.Filled.HelpOutline && importedLayout.definition.iconName != "HelpOutline") { /* ... (handle invalid icon) ... */ return@launch }

                // Create New Definition
                val newId = "freeform_${UUID.randomUUID()}"
                val itemsJson = try { json.encodeToString(ListSerializer(FreeFormItemState.serializer()), importedLayout.items ?: emptyList()) } catch (e: Exception) { null }
                val newDefinition = importedLayout.definition.copy(
                    id = newId, isUserDefined = true, isDeletable = true, isEnabled = true,
                    layoutItemsJson = itemsJson, iconName = importedLayout.definition.iconName
                )

                layoutRepository.addLayout(newDefinition)
                Log.i(_tag, "Successfully imported layout '${newDefinition.title}' with new ID '$newId'")
                _importResult.value = ImportResult.Success(newDefinition, importedLayout.items?.size ?: 0)

            } catch (e: Exception) {
                Log.e(_tag, "Import failed from $uri", e)
                _importResult.value = ImportResult.Failure("Import failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

}
