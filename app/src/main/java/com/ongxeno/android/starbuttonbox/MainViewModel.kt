package com.ongxeno.android.starbuttonbox

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.* // Import all filled icons
import androidx.compose.runtime.Composable // Keep this import for the return type
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ongxeno.android.starbuttonbox.data.* // Import data classes
import com.ongxeno.android.starbuttonbox.datasource.LayoutRepository // Import new Repository
import com.ongxeno.android.starbuttonbox.datasource.SettingDatasource
import com.ongxeno.android.starbuttonbox.datasource.UdpSender
import com.ongxeno.android.starbuttonbox.ui.layout.* // Import layout composables
import com.ongxeno.android.starbuttonbox.ui.model.LayoutInfo // Import UI model
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the main application screen (MainActivity).
 * Manages UI state related to layouts/tabs, network connection, settings visibility,
 * FreeForm layouts, and handles user interactions using LayoutRepository.
 * Uses Hilt for dependency injection.
 */
@OptIn(ExperimentalCoroutinesApi::class) // For flatMapLatest
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingDatasource: SettingDatasource,
    // Inject the new LayoutRepository
    private val layoutRepository: LayoutRepository,
    // Inject other dependencies like SoundPlayer, VibratorManagerUtils if needed directly here
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _tag = "MainViewModel"

    // --- Loading State ---
    private val _isLoading = MutableStateFlow(true) // Start as loading
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- State Flows for UI ---

    // Network Configuration Flow exposed as StateFlow
    val networkConfigState: StateFlow<NetworkConfig?> = settingDatasource.networkConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Layout/Tab State (using LayoutRepository) ---

    // Selected Layout Index Flow exposed as StateFlow
    // This index corresponds to the position within the *enabled* layouts list
    val selectedLayoutIndexState: StateFlow<Int> = layoutRepository.selectedLayoutIndexFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0) // Default to 0

    // Flow of enabled LayoutInfo objects for the main UI tabs
    val enabledLayoutsState: StateFlow<List<LayoutInfo>> = layoutRepository.enabledLayoutDefinitionsFlow
        .map { definitions -> definitions.map { mapDefinitionToInfo(it) } } // Map definitions to UI Info
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow of all LayoutInfo objects (including disabled) for the management screen
    val allLayoutsState: StateFlow<List<LayoutInfo>> = layoutRepository.allLayoutDefinitionsFlow
        .map { definitions -> definitions.map { mapDefinitionToInfo(it) } } // Map definitions to UI Info
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Screen Visibility State ---
    private val _showSettingsScreen = MutableStateFlow(false)
    val showSettingsScreenState: StateFlow<Boolean> = _showSettingsScreen.asStateFlow()

    private val _showConnectionConfigDialog = MutableStateFlow(false)
    val showConnectionConfigDialogState: StateFlow<Boolean> = _showConnectionConfigDialog.asStateFlow()

    // New state for Manage Layouts screen visibility
    private val _showManageLayoutsScreen = MutableStateFlow(false)
    val showManageLayoutsScreenState: StateFlow<Boolean> = _showManageLayoutsScreen.asStateFlow()

    // --- Add/Delete Dialog State ---
    private val _showAddLayoutDialog = MutableStateFlow(false) // State for Add dialog
    val showAddLayoutDialogState: StateFlow<Boolean> = _showAddLayoutDialog.asStateFlow()

    private val _showDeleteConfirmationDialog = MutableStateFlow(false)
    val showDeleteConfirmationDialogState: StateFlow<Boolean> = _showDeleteConfirmationDialog.asStateFlow()

    private val _layoutToDeleteState = mutableStateOf<LayoutInfo?>(null) // Store the LayoutInfo to be deleted
    val layoutToDeleteState: State<LayoutInfo?> = _layoutToDeleteState

    // State for Import Result Dialog
    private val _importResult = MutableStateFlow<ImportResult>(ImportResult.Idle)
    val importResultState: StateFlow<ImportResult> = _importResult.asStateFlow()

    // State to hold the ID of the layout currently requested for export
    private val _layoutToExportId = MutableStateFlow<String?>(null)
    // val layoutToExportIdState: StateFlow<String?> = _layoutToExportId.asStateFlow() // Expose if needed elsewhere

    // --- Keep Screen On State ---
    val keepScreenOnState: StateFlow<Boolean> = settingDatasource.keepScreenOnFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- Internal State ---
    private var udpSender: UdpSender? = null
    private var udpSenderJob: Job? = null

    // JSON parser for import/export
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // --- FreeForm Layout Items State ---
    // Flow for the ID of the currently selected *enabled* layout
    private val selectedLayoutIdFlow: Flow<String?> = combine(
        selectedLayoutIndexState,
        enabledLayoutsState // Use the flow of enabled layouts
    ) { index, enabledLayouts ->
        enabledLayouts.getOrNull(index)?.id
    }.distinctUntilChanged()

    // StateFlow for the items of the *currently selected* FreeForm layout
    val currentFreeFormItemsState: StateFlow<List<FreeFormItemState>> = selectedLayoutIdFlow
        .flatMapLatest { layoutId ->
            if (layoutId != null) {
                Log.d(_tag, "Loading items for selected layoutId: $layoutId")
                layoutRepository.getLayoutItemsFlow(layoutId) // Get items flow from repository
            } else {
                Log.d(_tag, "No layout selected or selected layout not found, emitting empty list.")
                flowOf(emptyList()) // Emit empty list if no valid layout is selected
            }
        }
        .catch { e ->
            Log.e(_tag, "Error loading free form items", e)
            emit(emptyList()) // Emit empty list on error
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // --- Initialization ---
    init {
        Log.d(_tag, "ViewModel initialized")
        observeNetworkConfig()
        initializeAppData()
    }

    private fun initializeAppData() {
        viewModelScope.launch {
            // Keep isLoading true until explicitly set to false at the end
            _isLoading.value = true
            Log.d(_tag, "Starting app data initialization...")
            var defaultsAdded = false // Flag to track if defaults were added in this run

            try {
                // 1. Check network config first
                val config = settingDatasource.networkConfigFlow.firstOrNull()
                Log.d(_tag, "initializeAppData: Network config read: $config")
                if (config?.ip.isNullOrBlank() || config.port == null) {
                    Log.d(_tag, "Initial config missing, requesting connection config show.")
                    if (!_showConnectionConfigDialog.value) {
                        _showConnectionConfigDialog.value = true
                    }
                }

                // 2. Check if default layouts need to be added
                // Wait for the first emission from the raw definitions flow
                val currentDefinitions = layoutRepository.layoutDefinitionsFlow.first()
                layoutRepository.enabledLayoutDefinitionsFlow.first()
                Log.d(_tag, "initializeAppData: First definitions value from source: ${currentDefinitions.size} items")
                if (currentDefinitions.isEmpty()) {
                    Log.i(_tag, "Layout definitions are empty, adding defaults.")
                    layoutRepository.addDefaultLayouts() // Add and save defaults
                    defaultsAdded = true
                    // We still need to wait for the *next* emission containing the defaults
                    layoutRepository.layoutDefinitionsFlow
                        .filter { it.isNotEmpty() } // Wait until the map is no longer empty
                        .first() // Consume the first non-empty emission
                    Log.d(_tag, "Defaults added and confirmed loaded into definitions flow.")
                } else {
                    Log.d(_tag, "Layout definitions already exist.")
                }

                // 3. Wait for the mapped LayoutInfo StateFlows to reflect the loaded data
                // This ensures the UI receives the data *before* isLoading becomes false
                if (defaultsAdded) {
                    // If defaults were added, wait for the StateFlows to emit a non-empty list
                    allLayoutsState.filter { it.isNotEmpty() }.first()
                    Log.d(_tag, "LayoutInfo StateFlows confirmed updated after adding defaults.")
                } else {
                    // If defaults weren't added, just wait for the first emission (which might be empty if all are disabled)
                    // or a non-empty one if saved data exists.
                    val all = allLayoutsState.first()
                    val enable = enabledLayoutsState.first()
                    Log.d(_tag, "LayoutInfo StateFlows emitted initial value from existing data. all.size: ${all.size}, enable.size: ${enable.size}")
                }

            } catch (e: Exception) {
                Log.e(_tag, "Error during app initialization", e)
            } finally {
                // 4. Set loading to false only after all checks and potential waits are done
                Log.d(_tag, "App data initialization complete. Setting isLoading to false.")
                delay(500)
                _isLoading.value = false
            }
        }
    }

    /**
     * Observes network configuration changes and updates the UdpSender.
     */
    private fun observeNetworkConfig() {
        udpSenderJob?.cancel() // Cancel previous job if exists
        udpSenderJob = viewModelScope.launch {
            networkConfigState.collect { config -> // Collect from StateFlow
                Log.d(_tag, "Network config changed: $config")
                // Create/update UdpSender instance based on config
                udpSender = config?.let { (ip, port) ->
                    if (ip != null && port != null) {
                        Log.i(_tag, "Creating/Updating UdpSender for $ip:$port")
                        UdpSender(ip, port)
                    } else {
                        Log.w(_tag, "Invalid network config, clearing UdpSender.")
                        null // Set sender to null if config is invalid
                    }
                }
            }
        }
    }


    // --- Event Handlers ---

    // Settings Screen Events
    /** Saves network connection settings and hides the config dialog. */
    fun saveConnectionSettings(ip: String, port: Int) {
        viewModelScope.launch {
            settingDatasource.saveSettings(ip, port)
            _showConnectionConfigDialog.value = false // Hide dialog after saving
            Log.i(_tag, "Connection settings saved via ViewModel.")
        }
    }

    /** Updates the 'Keep Screen On' preference. */
    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingDatasource.saveKeepScreenOn(enabled)
            Log.d(_tag, "Keep screen on set to: $enabled")
        }
    }

    /** Shows the main settings screen. */
    fun showSettingsScreen() {
        Log.d(_tag, "Showing settings screen.")
        _showSettingsScreen.value = true
        _showManageLayoutsScreen.value = false // Ensure manage screen is hidden
    }

    /** Hides the main settings screen. */
    fun hideSettingsScreen() {
        Log.d(_tag, "Hiding settings screen.")
        _showSettingsScreen.value = false
    }

    /** Shows the Connection Configuration Dialog. */
    fun showConnectionConfigDialog() {
        Log.d(_tag, "Showing connection config dialog.")
        _showConnectionConfigDialog.value = true
        // Optionally hide other screens if needed
        // _showSettingsScreen.value = false
        // _showManageLayoutsScreen.value = false
    }

    /** Hides the Connection Configuration Dialog, showing a Toast if config is invalid. */
    fun hideConnectionConfigDialog(contextForToast: Context? = null) {
        viewModelScope.launch {
            val config = networkConfigState.value // Get current config state
            if (config?.ip != null && config.port != null) {
                // Allow hiding if config is valid
                Log.d(_tag, "Hiding connection config dialog.")
                _showConnectionConfigDialog.value = false
            } else {
                // Prevent hiding if config is invalid (likely initial setup)
                Log.d(_tag, "Attempted to hide connection config, but config is invalid.")
                // Show toast only if context was provided (e.g., user pressed back/outside during initial setup)
                contextForToast?.let {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(it, "Please save connection settings first", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Command Sending
    /** Sends a command identifier via UDP if the sender is configured. Shows config dialog otherwise. */
    fun sendCommand(commandIdentifier: String, contextForToast: Context) {
        val currentSender = udpSender // Local copy for thread safety
        if (currentSender != null) {
            Log.d(_tag, "Sending command: $commandIdentifier via $currentSender")
            currentSender.sendCommandAction(commandIdentifier) // Use the sender instance
        } else {
            // Handle case where settings are missing or invalid
            Log.w(_tag, "SendCommand failed: UdpSender not available (Settings likely missing).")
            // Show a Toast on the main thread
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(contextForToast, "Connection Settings required", Toast.LENGTH_SHORT).show()
            }
            _showConnectionConfigDialog.value = true // Prompt user to configure connection
        }
    }

    // Layout / Tab Selection
    /** Saves the index of the selected layout (relative to the enabled layouts list). */
    fun selectLayout(index: Int) {
        viewModelScope.launch {
            Log.d(_tag, "Saving selected layout index: $index")
            layoutRepository.saveSelectedLayoutIndex(index)
        }
    }

    // --- Manage Layouts Screen Events ---
    /** Shows the Manage Layouts screen and hides the main settings screen. */
    fun showManageLayoutsScreen() {
        Log.d(_tag, "Showing Manage Layouts screen.")
        _showManageLayoutsScreen.value = true
    }

    /** Hides the Manage Layouts screen. */
    fun hideManageLayoutsScreen() {
        Log.d(_tag, "Hiding Manage Layouts screen.")
        _showManageLayoutsScreen.value = false
        // Decide whether to automatically go back to the main settings screen
        // _showSettingsScreen.value = true
    }

    fun saveLayoutOrder(orderedIds: List<String>) {
        viewModelScope.launch {
            layoutRepository.saveLayoutOrder(orderedIds)
        }
    }

    /** Initiates the layout deletion process by showing the confirmation dialog. */
    fun requestDeleteLayout(layoutInfo: LayoutInfo) {
        if (layoutInfo.isDeletable) {
            _layoutToDeleteState.value = layoutInfo // Store the layout to be deleted
            _showDeleteConfirmationDialog.value = true // Show the confirmation dialog
        } else {
            Log.w(_tag, "Attempted to delete non-deletable layout: ${layoutInfo.id}")
            // Optionally show a toast message here to inform the user
        }
    }

    /** Confirms the deletion, calls the repository, and adjusts the selected index if needed. */
    fun confirmDeleteLayout() {
        viewModelScope.launch {
            _layoutToDeleteState.value?.let { layoutToDelete ->
                Log.i(_tag, "Confirming deletion of layout: ${layoutToDelete.id}")
                val currentIndex = selectedLayoutIndexState.value
                val enabledLayoutsBeforeDelete = enabledLayoutsState.value // Get enabled list *before* delete
                val deletedLayoutWasEnabled = enabledLayoutsBeforeDelete.any { it.id == layoutToDelete.id }
                val deletedLayoutIndexInEnabled = enabledLayoutsBeforeDelete.indexOfFirst { it.id == layoutToDelete.id }

                // Delete from repository
                layoutRepository.deleteLayout(layoutToDelete.id)

                // Adjust selected index ONLY if the deleted layout was enabled
                if (deletedLayoutWasEnabled && deletedLayoutIndexInEnabled != -1) {
                    if (deletedLayoutIndexInEnabled < currentIndex) {
                        // If deleted item was before current selection, shift selection left
                        selectLayout((currentIndex - 1).coerceAtLeast(0))
                    } else if (deletedLayoutIndexInEnabled == currentIndex) {
                        // If deleted item *was* the current selection
                        // Select the previous item, or the new first item if deleting the first one
                        selectLayout((currentIndex - 1).coerceAtLeast(0))
                    }
                    // If deleted item was after current selection, index remains valid
                }

                // Reset deletion state
                _layoutToDeleteState.value = null
                _showDeleteConfirmationDialog.value = false

            } ?: Log.e(_tag, "Confirm delete called but layoutToDelete was null.")
        }
    }


    /** Cancels the layout deletion process. */
    fun cancelDeleteLayout() {
        _layoutToDeleteState.value = null // Clear the stored layout
        _showDeleteConfirmationDialog.value = false // Hide the dialog
    }

    /** Toggles the enabled/visible state of a layout. */
    fun toggleLayoutEnabled(layoutId: String) {
        viewModelScope.launch {
            layoutRepository.toggleLayoutEnabled(layoutId)
            // Optional: Adjust selected index if the currently selected tab is hidden
            val currentIndex = selectedLayoutIndexState.value
            val currentEnabledLayouts = enabledLayoutsState.value // Get list *after* toggle might have changed it

            // Check if the currently selected index is now invalid (points beyond the new list size)
            // or if the item at that index is no longer the originally selected one (because it got hidden)
            val originallySelectedId = selectedLayoutIdFlow.first() // Get the ID before potential change
            if (currentIndex >= currentEnabledLayouts.size || currentEnabledLayouts.getOrNull(currentIndex)?.id != originallySelectedId) {
                // If the selection is now invalid, try selecting the first available enabled layout
                if (currentEnabledLayouts.isNotEmpty()) {
                    selectLayout(0)
                    Log.d(_tag, "Adjusted selected index to 0 after layout toggle.")
                } else {
                    // Handle case where no layouts are enabled (though maybe prevent hiding the last one?)
                    Log.w(_tag, "No enabled layouts left after toggle.")
                    // You might want to prevent hiding the last enabled layout in the UI
                }
            }
        }
    }

    /** Shows the dialog to add a new layout. */
    fun requestAddLayout() {
        Log.d(_tag, "Requesting Add Layout Dialog")
        _showAddLayoutDialog.value = true
    }

    /** Creates and saves a new FreeForm layout based on user input from the dialog. */
    fun confirmAddLayout(title: String, iconName: String) {
        viewModelScope.launch {
            // Generate a unique ID for the new layout
            val newId = "freeform_${UUID.randomUUID()}"
            val newLayout = LayoutDefinition(
                id = newId,
                title = title,
                layoutType = LayoutType.FREE_FORM, // Hardcoded as FreeForm
                iconName = iconName,
                isEnabled = true, // Enabled by default
                isUserDefined = true, // Marked as user-defined
                isDeletable = true, // User-defined layouts are deletable
                layoutItemsJson = null // Starts with no items
            )
            layoutRepository.addLayout(newLayout)
            Log.i(_tag, "Added new layout: $newLayout")
            _showAddLayoutDialog.value = false // Hide dialog after adding
        }
    }

    /** Cancels the add layout operation and hides the dialog. */
    fun cancelAddLayout() {
        _showAddLayoutDialog.value = false
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
        // The UI will observe this (if needed) or just launch the SAF picker
    }

    /** Clears the stored layout ID after an export attempt. */
    fun clearExportRequest() {
        _layoutToExportId.value = null
    }

    /**
     * Exports the layout identified by the current `_layoutToExportId` state to the given URI.
     * Called *after* the user selects a destination file via SAF.
     *
     * @param uri The content URI chosen by the user via SAF.
     */
    fun exportLayoutToFile(uri: Uri) {
        val layoutId = _layoutToExportId.value // Get the ID stored previously
        if (layoutId == null) {
            Log.e(_tag, "Export failed: No layout ID was set for export.")
            // TODO: Show error message to user
            clearExportRequest() // Clear the state even on failure
            return
        }

        viewModelScope.launch {
            Log.d(_tag, "Attempting to export layout '$layoutId' to URI: $uri")
            val definition = layoutRepository.allLayoutDefinitionsFlow.first()
                .find { it.id == layoutId }

            if (definition == null) {
                Log.e(_tag, "Export failed: Layout definition not found for ID '$layoutId'")
                // TODO: Show error message
            } else if (definition.layoutType != LayoutType.FREE_FORM) {
                Log.e(_tag, "Export failed: Layout '$layoutId' is not a FreeForm layout.")
                // TODO: Show error message
            } else {
                // Get the items
                val items = layoutRepository.getLayoutItemsFlow(layoutId).first()
                val exportedData = ExportedLayout(definition = definition, items = items)

                try {
                    val jsonString = json.encodeToString(ExportedLayout.serializer(), exportedData)
                    appContext.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(jsonString)
                        }
                    }
                    Log.i(_tag, "Successfully exported layout '$layoutId' to $uri")
                    // TODO: Show success message
                } catch (e: Exception) {
                    Log.e(_tag, "Export failed for layout '$layoutId' to $uri", e)
                    // TODO: Show error message
                }
            }
            // Clear the export request state regardless of success/failure
            clearExportRequest()
        }
    }

    /**
     * Imports a layout from a chosen file URI.
     * This function is called by the Composable after the user selects a file via SAF.
     *
     * @param uri The content URI chosen by the user via SAF.
     */
    fun importLayoutFromFile(uri: Uri) {
        viewModelScope.launch {
            Log.d(_tag, "Attempting to import layout from URI: $uri")
            try {
                val jsonString = appContext.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.readText()
                    }
                }

                if (jsonString.isNullOrBlank()) {
                    _importResult.value = ImportResult.Failure("Selected file is empty.")
                    return@launch
                }

                val importedLayout = json.decodeFromString(ExportedLayout.serializer(), jsonString)

                // --- Validation ---
                if (importedLayout.definition.layoutType != LayoutType.FREE_FORM) {
                    _importResult.value = ImportResult.Failure("Import failed: Only FreeForm layouts can be imported.")
                    return@launch
                }
                if (importedLayout.definition.title.isBlank()) {
                    _importResult.value = ImportResult.Failure("Import failed: Layout title cannot be empty.")
                    return@launch
                }
                // Basic check for icon name validity (optional, depends on how strict you want to be)
                try { mapIconName(importedLayout.definition.iconName) } catch (_: Exception) {
                    _importResult.value = ImportResult.Failure("Import failed: Invalid icon name '${importedLayout.definition.iconName}'.")
                    return@launch
                }


                // --- Create New Definition ---
                val newId = "freeform_${UUID.randomUUID()}" // Generate new ID to avoid collisions
                val itemsJson = try {
                    json.encodeToString(ListSerializer(FreeFormItemState.serializer()), importedLayout.items ?: emptyList())
                } catch (e: Exception) {
                    Log.e(_tag, "Error serializing imported items, saving as empty", e)
                    null // Save as null/empty if items are invalid
                }

                val newDefinition = importedLayout.definition.copy(
                    id = newId, // Use new ID
                    isUserDefined = true, // Imported layouts are treated as user-defined
                    isDeletable = true, // Imported layouts are deletable
                    isEnabled = true, // Enable by default
                    layoutItemsJson = itemsJson // Use the re-serialized (or null) items JSON
                )

                // Add to repository
                layoutRepository.addLayout(newDefinition)

                Log.i(_tag, "Successfully imported layout '${newDefinition.title}' with new ID '$newId'")
                _importResult.value = ImportResult.Success(newDefinition, importedLayout.items?.size ?: 0)

            } catch (e: Exception) {
                Log.e(_tag, "Import failed from $uri", e)
                _importResult.value = ImportResult.Failure("Import failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    // --- FreeForm Layout Item Events ---
    // These now need to get the current layout ID before saving items
    /** Saves the entire list of items for the currently selected FreeForm layout. */
    fun saveFreeFormLayout(items: List<FreeFormItemState>) {
        viewModelScope.launch {
            val layoutId = selectedLayoutIdFlow.first() // Get current selected layout ID
            if (layoutId != null) {
                Log.d(_tag, "Saving layout items for ID: $layoutId")
                layoutRepository.saveLayoutItems(layoutId, items)
            } else {
                Log.w(_tag, "Cannot save layout items, selectedLayoutId is null.")
            }
        }
    }

    // --- Helper / Mapping Functions (Moved from Repository) ---

    /** Maps a LayoutDefinition (from Repository) to a LayoutInfo (for UI). */
    private fun mapDefinitionToInfo(definition: LayoutDefinition): LayoutInfo {
        return LayoutInfo(
            id = definition.id,
            title = definition.title,
            icon = mapIconName(definition.iconName), // Map string name back to ImageVector
            type = definition.layoutType,
            isEnabled = definition.isEnabled,
            isDeletable = definition.isDeletable,
            content = mapLayoutTypeToContent(definition.layoutType, definition.id) // Map ID/Type to the correct Composable lambda
        )
    }

    /** Maps a stored icon name string to the corresponding Material Icon ImageVector. */
    private fun mapIconName(name: String): ImageVector {
        return when (name) {
            "Adjust" -> Icons.Filled.Adjust
            "Bolt" -> Icons.Filled.Bolt
            "Build" -> Icons.Filled.Build
            "Camera" -> Icons.Filled.Camera
            "Construction" -> Icons.Filled.Construction
            "DashboardCustomize" -> Icons.Filled.DashboardCustomize
            "Diamond" -> Icons.Filled.Diamond
            "Flag" -> Icons.Filled.Flag
            "Flight" -> Icons.Filled.Flight
            "Gamepad" -> Icons.Filled.Gamepad
            "Headset" -> Icons.Filled.Headset
            "HelpOutline" -> Icons.AutoMirrored.Filled.HelpOutline
            "Key" -> Icons.Filled.Key
            "Lightbulb" -> Icons.Filled.Lightbulb
            "LocalFireDepartment" -> Icons.Filled.LocalFireDepartment
            "Lock" -> Icons.Filled.Lock
            "Map" -> Icons.Filled.Map
            "Mic" -> Icons.Filled.Mic
            "Navigation" -> Icons.Filled.Navigation
            "Power" -> Icons.Filled.Power
            "Recycling" -> Icons.Filled.Recycling
            "Rocket" -> Icons.Filled.RocketLaunch
            "SettingsInputComponent" -> Icons.Filled.SettingsInputComponent
            "Shield" -> Icons.Filled.Shield
            "Speed" -> Icons.Filled.Speed
            "Star" -> Icons.Filled.Star
            "Tune" -> Icons.Filled.Tune
            "Videocam" -> Icons.Filled.Videocam
            "Warning" -> Icons.Filled.Warning
            "WbSunny" -> Icons.Filled.WbSunny
            "Widgets" -> Icons.Filled.Widgets
            else -> Icons.AutoMirrored.Filled.HelpOutline // Default fallback icon
        }
    }

    /** Maps a layout type and ID to its corresponding content Composable function lambda. */
    // No @Composable annotation needed here as it just returns the lambda
    private fun mapLayoutTypeToContent(type: LayoutType, layoutId: String): @Composable (MainViewModel) -> Unit {
        // Return the lambda directly. The @Composable context is handled by the caller (e.g., MainActivity)
        return when (type) {
            LayoutType.NORMAL_FLIGHT -> { vm -> NormalFlightLayout(vm) }
            LayoutType.DEMO -> { vm -> DemoLayout() }
            LayoutType.FREE_FORM -> { vm -> FreeFormLayout(vm) } // FreeFormLayout uses ViewModel's item flow
            LayoutType.PLACEHOLDER -> { vm -> PlaceholderLayout("Layout: $layoutId") }
            // Add cases for other specific types if they have unique composables
        }
    }


    // --- Cleanup ---
    override fun onCleared() {
        super.onCleared()
        udpSenderJob?.cancel() // Cancel the network observer job
        udpSender?.close() // Close the UDP socket if open
        Log.d(_tag, "ViewModel cleared.")
    }
}
