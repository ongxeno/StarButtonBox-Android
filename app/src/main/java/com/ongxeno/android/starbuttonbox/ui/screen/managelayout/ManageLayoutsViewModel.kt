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
import com.ongxeno.android.starbuttonbox.data.ConnectionStatus
import com.ongxeno.android.starbuttonbox.data.ExportedLayout
import com.ongxeno.android.starbuttonbox.data.FreeFormItemState
import com.ongxeno.android.starbuttonbox.data.ImportResult
import com.ongxeno.android.starbuttonbox.data.LayoutDefinition
import com.ongxeno.android.starbuttonbox.data.LayoutType
import com.ongxeno.android.starbuttonbox.datasource.ConnectionManager
import com.ongxeno.android.starbuttonbox.datasource.LayoutRepository
import com.ongxeno.android.starbuttonbox.datasource.AppLocalWebServer // <-- Import Ktor Web Server
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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ManageLayoutsViewModel @Inject constructor(
    private val layoutRepository: LayoutRepository,
    private val connectionManager: ConnectionManager, // <-- Inject ConnectionManager
    private val appLocalWebServer: AppLocalWebServer, // <-- Inject PcImportWebServer
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
                        isDeletable = it.isDeletable,
                        isUserDefined = it.isUserDefined
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Connection Status ---
    val connectionStatusState: StateFlow<ConnectionStatus> = connectionManager.connectionStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionStatus.NO_CONFIG)

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

    // --- State for Import from PC Dialog ---
    private val _showImportFromPcDialog = MutableStateFlow(false)
    val showImportFromPcDialogState: StateFlow<Boolean> = _showImportFromPcDialog.asStateFlow()

    private val _importFromPcStatusMessage = MutableStateFlow<String?>(null)
    val importFromPcStatusMessageState: StateFlow<String?> = _importFromPcStatusMessage.asStateFlow()
    // ---

    // --- Export State ---
    private val _layoutToExportId = MutableStateFlow<String?>(null)

    // JSON parser
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // --- Event Handlers (Existing ones remain unchanged) ---

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
                val currentDefinitionsMap = layoutRepository.layoutDefinitionsFlow.first()
                val definitionToUpdate = currentDefinitionsMap[existingId]

                if (definitionToUpdate != null) {
                    val updatedDefinition = definitionToUpdate.copy(title = title, iconName = iconName)
                    layoutRepository.updateLayoutDefinition(updatedDefinition)
                    Log.i(_tag, "Updated layout definition: $updatedDefinition")
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

    // --- Import / Export (Existing file import/export remain unchanged) ---
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
        if (layoutId == null) {
            Log.w(_tag, "Export requested but no layout ID was set.")
            clearExportRequest()
            return
        }

        viewModelScope.launch {
            Log.d(_tag, "Attempting to export layout '$layoutId' to URI: $uri")
            val definition = layoutRepository.allLayoutDefinitionsFlow.first().find { it.id == layoutId }
            val items = layoutRepository.getLayoutItemsFlow(layoutId).first()

            if (definition == null) {
                Log.e(_tag, "Export failed: Layout definition not found for ID '$layoutId'")
                _importResult.value = ImportResult.Failure("Export failed: Layout not found.")
            } else if (definition.layoutType != LayoutType.FREE_FORM) {
                Log.e(_tag, "Export failed: Layout '$layoutId' is not a FreeForm layout.")
                _importResult.value = ImportResult.Failure("Export failed: Only FreeForm layouts can be exported.")
            } else {
                val exportedData = ExportedLayout(definition = definition, items = items)
                try {
                    val jsonString = json.encodeToString(ExportedLayout.serializer(), exportedData)
                    appContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer -> writer.write(jsonString) }
                    } ?: throw IOException("Failed to open output stream for URI: $uri")
                    Log.i(_tag, "Successfully exported layout '$layoutId' to $uri")
                    _importResult.value = ImportResult.Success(definition, items.size)
                } catch (e: Exception) {
                    Log.e(_tag, "Export failed for layout '$layoutId' to $uri", e)
                    _importResult.value = ImportResult.Failure("Export failed: ${e.localizedMessage ?: "Unknown error"}")
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

                if (jsonString.isNullOrBlank()) {
                    Log.w(_tag, "Import failed: File content is blank or null from URI: $uri")
                    _importResult.value = ImportResult.Failure("Import failed: File is empty")
                    return@launch
                }

                val importedLayout = json.decodeFromString(ExportedLayout.serializer(), jsonString)

                // --- Validation ---
                if (importedLayout.definition.layoutType != LayoutType.FREE_FORM) {
                    Log.w(_tag, "Import failed: Imported layout type is not FREE_FORM. Type: ${importedLayout.definition.layoutType}")
                    _importResult.value = ImportResult.Failure("Import failed: Layout type must be FreeForm")
                    return@launch
                }
                if (importedLayout.definition.title.isBlank()) {
                    Log.w(_tag, "Import failed: Imported layout title is blank.")
                    _importResult.value = ImportResult.Failure("Import failed: Layout title cannot be empty")
                    return@launch
                }
                val iconVector = IconMapper.getIconVector(importedLayout.definition.iconName)
                if (iconVector == Icons.AutoMirrored.Filled.HelpOutline && importedLayout.definition.iconName != "HelpOutline") {
                    Log.w(_tag, "Import failed: Imported layout has an invalid icon name: ${importedLayout.definition.iconName}")
                    _importResult.value = ImportResult.Failure("Import failed: Invalid icon name '${importedLayout.definition.iconName}'")
                    return@launch
                }
                // --- End Validation ---

                val newId = "freeform_${UUID.randomUUID()}"
                val itemsJson = try {
                    json.encodeToString(ListSerializer(FreeFormItemState.serializer()), importedLayout.items ?: emptyList())
                } catch (e: Exception) {
                    Log.e(_tag, "Error serializing items during import for new ID $newId", e)
                    null
                }
                val newDefinition = importedLayout.definition.copy(
                    id = newId,
                    isUserDefined = true,
                    isDeletable = true,
                    isEnabled = true,
                    layoutItemsJson = itemsJson
                )

                layoutRepository.addLayout(newDefinition)
                Log.i(_tag, "Successfully imported layout '${newDefinition.title}' with new ID '$newId'")
                _importResult.value = ImportResult.Success(newDefinition, importedLayout.items?.size ?: 0)

            } catch (e: Exception) {
                Log.e(_tag, "Import failed from $uri", e)
                _importResult.value = ImportResult.Failure("Import failed: ${e.localizedMessage ?: "Invalid file format"}")
            }
        }
    }


    // --- NEW: Import from PC Functions ---
    /** Initiates the Import from PC process. */
    fun initiatePcImport() {
        Log.d(_tag, "Initiating Import from PC")
        _showImportFromPcDialog.value = true
        _importFromPcStatusMessage.value = "Starting import server..."

        viewModelScope.launch { // Launch in VM scope for suspend functions
            val serverUrl = appLocalWebServer.startServer(AppLocalWebServer.ServerMode.LAYOUT_IMPORT, ::handleReceivedLayoutJson)

            if (serverUrl != null) {
                Log.i(_tag, "PC Import Server started at: $serverUrl")
                // Send trigger packet to PC server via ConnectionManager
                val success = connectionManager.sendTriggerImportBrowser(serverUrl) // Assuming this function exists now
                if (success) {
                    _importFromPcStatusMessage.value = "Waiting for PC browser connection..."
                } else {
                    _importFromPcStatusMessage.value = "Failed to notify PC server. Please check connection."
                    // Consider stopping the server if notification fails
                    // pcImportWebServer.stopServer()
                    // _showImportFromPcDialog.value = false // Optionally close dialog on immediate failure
                }
            } else {
                Log.e(_tag, "Failed to start PC Import Server.")
                _importFromPcStatusMessage.value = "Error starting import server on device."
                // Optionally close dialog after a delay
                // delay(3000)
                // _showImportFromPcDialog.value = false
            }
        }
    }

    /** Handles the JSON content received from the Ktor server. */
    private fun handleReceivedLayoutJson(jsonContent: String) {
        Log.d(_tag, "Handling received layout JSON from PC (Size: ${jsonContent.length})")

        // --- Set status message and dismiss IMMEDIATELY ---
        // Update status first
        _importFromPcStatusMessage.value = "Processing imported layout..."
        // Then immediately set the flag to dismiss the ImportFromPcDialog
        // This ensures the UI knows to hide the progress dialog BEFORE starting the import work.
        _showImportFromPcDialog.value = false
        Log.d(_tag, "Set showImportFromPcDialog to false. Value: ${_showImportFromPcDialog.value}")
        // --- End Immediate Dismiss ---

        viewModelScope.launch { // Perform file operations and import off the main thread
            var tempFileUri: Uri? = null
            var tempFile: File? = null
            try {
                // 1. Save jsonContent to a temporary file
                tempFile = File.createTempFile("imported_layout_", ".json", appContext.cacheDir)
                FileOutputStream(tempFile).use { fos ->
                    OutputStreamWriter(fos).use { writer ->
                        writer.write(jsonContent)
                    }
                }
                tempFileUri = Uri.fromFile(tempFile) // Get Uri for the temp file
                Log.d(_tag, "Saved received JSON to temporary file: $tempFileUri")

                // 2. Call existing import function with the temp file Uri
                importLayoutFromFile(tempFileUri) // This will update _importResult

                // 3. Update status based on the outcome (importResult will be updated by importLayoutFromFile)
                // The ImportResultDialog will show the final success/failure.
                // We can update the status message here for intermediate feedback.
                // Note: importLayoutFromFile is async, so importResult might not be updated *immediately*.
                // We rely on the ImportResultDialog showing the final state.
                _importFromPcStatusMessage.value = "Import processing complete. Check result dialog." // Indicate processing finished

            } catch (e: Exception) {
                Log.e(_tag, "Error handling received layout JSON", e)
                _importFromPcStatusMessage.value = "Error processing import: ${e.localizedMessage}"
                _importResult.value = ImportResult.Failure("Error processing import: ${e.localizedMessage}")
            } finally {
                // 4. Stop the Ktor server
                appLocalWebServer.stopServer()
                Log.d(_tag, "Stopped Ktor server after handling JSON.")

                _importFromPcStatusMessage.value = null

                // 5. Clean up the temporary file
                tempFile?.let { fileToDelete ->
                    if (fileToDelete.exists()) {
                        if (fileToDelete.delete()) {
                            Log.d(_tag, "Deleted temporary import file: ${fileToDelete.path}")
                        } else {
                            Log.w(_tag, "Failed to delete temporary import file: ${fileToDelete.path}")
                        }
                    }
                }
            }
        }
    }

    /** Cancels the Import from PC process. */
    fun cancelPcImport() {
        Log.d(_tag, "Cancelling Import from PC")
        viewModelScope.launch {
            appLocalWebServer.stopServer() // Stop the Ktor server
        }
        _showImportFromPcDialog.value = false
        _importFromPcStatusMessage.value = null
        // TODO: (Optional) Send cancellation message to PC server via ConnectionManager if needed
    }
    // --- End Import from PC Functions ---

    /** Ensure Ktor server is stopped when ViewModel is cleared */
    override fun onCleared() {
        super.onCleared()
        Log.d(_tag, "ManageLayoutsViewModel cleared. Stopping Ktor server if running.")
        viewModelScope.launch {
            appLocalWebServer.stopServer()
        }
    }
}
