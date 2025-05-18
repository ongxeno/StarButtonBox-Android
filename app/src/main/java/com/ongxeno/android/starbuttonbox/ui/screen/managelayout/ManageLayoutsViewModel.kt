package com.ongxeno.android.starbuttonbox.ui.screen.managelayout

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
import com.ongxeno.android.starbuttonbox.datasource.AppLocalWebServer
import com.ongxeno.android.starbuttonbox.datasource.ConnectionManager
import com.ongxeno.android.starbuttonbox.datasource.LayoutRepository
import com.ongxeno.android.starbuttonbox.datasource.room.LayoutEntity
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
    private val connectionManager: ConnectionManager,
    private val appLocalWebServer: AppLocalWebServer,
    @ApplicationContext private val appContext: Context,
    private val json: Json
) : ViewModel() {

    private val _tag = "ManageLayoutsVM"

    val manageLayoutsState: StateFlow<List<ManageLayoutInfo>> =
        layoutRepository.allLayoutsFlow
            .map { layoutEntities ->
                layoutEntities.map { entity -> entity.toManageLayoutInfo() }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val connectionStatusState: StateFlow<ConnectionStatus> = connectionManager.connectionStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionStatus.NO_CONFIG)

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

    private val _showImportFromPcDialog = MutableStateFlow(false)
    val showImportFromPcDialogState: StateFlow<Boolean> = _showImportFromPcDialog.asStateFlow()

    private val _importFromPcStatusMessage = MutableStateFlow<String?>(null)
    val importFromPcStatusMessageState: StateFlow<String?> = _importFromPcStatusMessage.asStateFlow()

    private val _layoutToExportId = MutableStateFlow<String?>(null)

    fun saveLayoutOrder(orderedIds: List<String>) {
        viewModelScope.launch { layoutRepository.saveLayoutOrder(orderedIds) }
    }

    fun toggleLayoutEnabled(layoutId: String) {
        viewModelScope.launch { layoutRepository.toggleLayoutEnabled(layoutId) }
    }

    fun requestDeleteLayout(layoutInfo: ManageLayoutInfo?) {
        _layoutToDeleteState.value = layoutInfo
        if (layoutInfo != null && layoutInfo.isDeletable) {
            _showDeleteConfirmationDialog.value = true
        } else if (layoutInfo != null && !layoutInfo.isDeletable) {
            Log.w(_tag, "Attempted to delete non-deletable layout: ${layoutInfo.id}")
        }
    }

    fun confirmDeleteLayout() {
        viewModelScope.launch {
            _layoutToDeleteState.value?.let { layoutRepository.deleteLayout(it.id) }
            _layoutToDeleteState.value = null
            _showDeleteConfirmationDialog.value = false
        }
    }

    fun cancelDeleteLayout() {
        _layoutToDeleteState.value = null
        _showDeleteConfirmationDialog.value = false
    }

    fun requestAddLayout() {
        _layoutToEditState.value = null
        _showAddEditLayoutDialog.value = true
    }

    fun requestEditLayout(layoutInfo: ManageLayoutInfo) {
        if (layoutInfo.type == LayoutType.FREE_FORM || layoutInfo.isUserDefined) {
            _layoutToEditState.value = layoutInfo
            _showAddEditLayoutDialog.value = true
        } else {
            Log.w(_tag, "Edit requested for non-editable/non-user-defined layout: ${layoutInfo.id}")
        }
    }

    fun confirmSaveLayout(title: String, iconName: String, existingId: String?) {
        viewModelScope.launch {
            if (existingId == null) {
                layoutRepository.addLayout(title, LayoutType.FREE_FORM, iconName, emptyList())
                Log.i(_tag, "Added new FreeForm layout: $title")
            } else {
                val layoutToUpdate = layoutRepository.allLayoutsFlow.first().find { it.id == existingId }
                if (layoutToUpdate != null && (layoutToUpdate.isUserDefined || LayoutType.valueOf(layoutToUpdate.layoutTypeString) == LayoutType.FREE_FORM)) {
                    val updatedEntity = layoutToUpdate.copy(title = title, iconName = iconName)
                    layoutRepository.updateLayoutEntity(updatedEntity)
                    Log.i(_tag, "Updated layout entity: ${updatedEntity.title}")
                } else {
                    Log.w(_tag, "Edit not allowed or layout not found for ID: $existingId")
                }
            }
            _showAddEditLayoutDialog.value = false
            _layoutToEditState.value = null
        }
    }

    fun cancelAddEditLayout() {
        _showAddEditLayoutDialog.value = false
        _layoutToEditState.value = null
    }

    fun dismissImportResult() { _importResult.value = ImportResult.Idle }
    fun requestExportLayout(layoutId: String) { _layoutToExportId.value = layoutId }
    fun clearExportRequest() { _layoutToExportId.value = null }

    fun exportLayoutToFile(uri: Uri) {
        val layoutId = _layoutToExportId.value
        if (layoutId == null) { Log.w(_tag, "Export requested but no layout ID set."); clearExportRequest(); return }

        viewModelScope.launch {
            Log.d(_tag, "exportLayoutToFile: Exporting layout ID: $layoutId")
            val layoutEntity = layoutRepository.allLayoutsFlow.first().find { it.id == layoutId }

            if (layoutEntity == null) {
                Log.e(_tag, "exportLayoutToFile: LayoutEntity not found for ID '$layoutId'")
                _importResult.value = ImportResult.Failure("Export failed: Layout not found.")
            } else if (LayoutType.valueOf(layoutEntity.layoutTypeString) != LayoutType.FREE_FORM) {
                Log.e(_tag, "exportLayoutToFile: Layout '$layoutId' is not FreeForm. Type: ${layoutEntity.layoutTypeString}")
                _importResult.value = ImportResult.Failure("Export failed: Only FreeForm layouts can be exported.")
            } else {
                val items = layoutRepository.getLayoutItemsFlow(layoutId).first()
                Log.d(_tag, "exportLayoutToFile: Fetched ${items.size} items for layout ID '$layoutId' for export.")
                if (items.isEmpty()) {
                    Log.w(_tag, "exportLayoutToFile: Exporting layout '$layoutId' with 0 items.")
                }

                val definitionForExport = LayoutDefinition(
                    id = layoutEntity.id, title = layoutEntity.title,
                    layoutType = LayoutType.valueOf(layoutEntity.layoutTypeString),
                    iconName = layoutEntity.iconName, isEnabled = layoutEntity.isEnabled,
                    isUserDefined = layoutEntity.isUserDefined, isDeletable = layoutEntity.isDeletable,
                    layoutItemsJson = null
                )
                val exportedData = ExportedLayout(definition = definitionForExport, items = items)
                try {
                    val jsonString = json.encodeToString(ExportedLayout.serializer(), exportedData)
                    appContext.contentResolver.openOutputStream(uri)?.use { os -> OutputStreamWriter(os).use { it.write(jsonString) } }
                        ?: throw IOException("Failed to open output stream for URI: $uri")
                    Log.i(_tag, "Successfully exported layout '$layoutId' with ${items.size} items to $uri")
                    _importResult.value = ImportResult.Success(definitionForExport, items.size)
                } catch (e: Exception) {
                    Log.e(_tag, "Export failed for layout '$layoutId' to $uri", e)
                    _importResult.value = ImportResult.Failure("Export failed: ${e.localizedMessage ?: "Unknown error"}")
                }
            }
            clearExportRequest()
        }
    }

    fun importLayoutFromFile(uri: Uri) {
        viewModelScope.launch {
            Log.d(_tag, "importLayoutFromFile: Importing from URI: $uri")
            _importResult.value = ImportResult.Idle
            try {
                val jsonString = appContext.contentResolver.openInputStream(uri)?.use { BufferedReader(InputStreamReader(it)).readText() }
                if (jsonString.isNullOrBlank()) {
                    _importResult.value = ImportResult.Failure("Import failed: File is empty"); return@launch
                }
                val importedData = json.decodeFromString(ExportedLayout.serializer(), jsonString)
                val importedDefinition = importedData.definition
                Log.d(_tag, "importLayoutFromFile: Parsed ${importedData.items?.size ?: 0} items for layout '${importedDefinition.title}'")

                if (importedDefinition.layoutType != LayoutType.FREE_FORM) {
                    _importResult.value = ImportResult.Failure("Import failed: Layout type must be FreeForm"); return@launch
                }
                if (importedDefinition.title.isBlank()) {
                    _importResult.value = ImportResult.Failure("Import failed: Layout title cannot be empty"); return@launch
                }

                // Regenerate IDs for buttons
                val newButtonsWithNewIds = importedData.items?.map { itemState ->
                    itemState.copy(id = UUID.randomUUID().toString()) // Assign new button ID
                } ?: emptyList()

                Log.d(_tag, "importLayoutFromFile: Regenerated IDs for ${newButtonsWithNewIds.size} buttons.")

                // The LayoutRepository.addLayout method will generate a new ID for the LayoutEntity.
                // We pass the imported definition's properties (title, icon, etc.)
                // and the buttons with newly generated IDs.
                layoutRepository.addLayout(
                    title = importedDefinition.title,
                    layoutType = LayoutType.FREE_FORM, // Already validated
                    iconName = importedDefinition.iconName,
                    initialButtons = newButtonsWithNewIds // Pass buttons with new IDs
                )
                Log.i(_tag, "Successfully imported layout '${importedDefinition.title}' as a new layout.")
                // For the success message, we can still show the original title and item count.
                _importResult.value = ImportResult.Success(importedDefinition, newButtonsWithNewIds.size)
            } catch (e: Exception) {
                Log.e(_tag, "Import failed from $uri", e)
                _importResult.value = ImportResult.Failure("Import failed: ${e.localizedMessage ?: "Invalid file format"}")
            }
        }
    }

    fun initiatePcImport() {
        Log.d(_tag, "Initiating Import from PC")
        _showImportFromPcDialog.value = true
        _importFromPcStatusMessage.value = "Starting import server..."
        viewModelScope.launch {
            val serverUrl = appLocalWebServer.startServer(AppLocalWebServer.ServerMode.LAYOUT_IMPORT, ::handleReceivedLayoutJson)
            if (serverUrl != null) {
                val success = connectionManager.sendTriggerImportBrowser(serverUrl)
                _importFromPcStatusMessage.value = if (success) "Waiting for PC browser connection at\n$serverUrl\n(Ensure PC is on the same Wi-Fi)"
                                                 else "Failed to notify PC server. Check connection."
            } else {
                _importFromPcStatusMessage.value = "Error starting import server on device."
            }
        }
    }

    private fun handleReceivedLayoutJson(jsonContent: String) {
        Log.d(_tag, "handleReceivedLayoutJson: Received JSON from PC (Size: ${jsonContent.length})")
        _importFromPcStatusMessage.value = "Processing imported layout..."
        _showImportFromPcDialog.value = false
        viewModelScope.launch {
            var tempFile: File? = null
            try {
                tempFile = File.createTempFile("pc_imported_layout_", ".json", appContext.cacheDir)
                FileOutputStream(tempFile).use { fos -> OutputStreamWriter(fos).use { it.write(jsonContent) } }
                Log.d(_tag, "handleReceivedLayoutJson: Saved to temp file: ${tempFile.absolutePath}")
                importLayoutFromFile(Uri.fromFile(tempFile))
            } catch (e: Exception) {
                Log.e(_tag, "Error handling received layout JSON from PC", e)
                _importResult.value = ImportResult.Failure("Error processing PC import: ${e.localizedMessage}")
            } finally {
                appLocalWebServer.stopServer()
                _importFromPcStatusMessage.value = null
                tempFile?.delete()
                Log.d(_tag, "handleReceivedLayoutJson: Temp file deleted, Ktor server stopped.")
            }
        }
    }

    fun cancelPcImport() {
        viewModelScope.launch { appLocalWebServer.stopServer() }
        _showImportFromPcDialog.value = false
        _importFromPcStatusMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { appLocalWebServer.stopServer() }
    }
}
