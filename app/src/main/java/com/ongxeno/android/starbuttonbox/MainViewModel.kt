package com.ongxeno.android.starbuttonbox

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.State // Keep for compatibility if needed elsewhere, but not for these states
import androidx.compose.runtime.mutableStateOf // Keep for compatibility if needed elsewhere
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ongxeno.android.starbuttonbox.data.FreeFormItemState
import com.ongxeno.android.starbuttonbox.data.FreeFormItemType
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import com.ongxeno.android.starbuttonbox.datasource.LayoutDatasource
import com.ongxeno.android.starbuttonbox.datasource.SettingDatasource
import com.ongxeno.android.starbuttonbox.datasource.TabDatasource
import com.ongxeno.android.starbuttonbox.datasource.UdpSender
import com.ongxeno.android.starbuttonbox.ui.model.TabInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.* // Ensure StateFlow and MutableStateFlow are imported
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for the main application screen (MainActivity).
 * Manages UI state related to tabs, network connection, settings visibility,
 * FreeForm layouts, and handles user interactions.
 * Uses Hilt for dependency injection.
 */
@OptIn(ExperimentalCoroutinesApi::class) // Needed for flatMapLatest
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingDatasource: SettingDatasource,
    private val tabDatasource: TabDatasource,
    private val layoutDatasource: LayoutDatasource
    // Other dependencies like SoundPlayer, VibratorManagerUtils could be injected here
) : ViewModel() {

    private val _tag = "MainViewModel" // Logging tag

    // --- State Flows for UI ---

    // Network Configuration (IP/Port) from SettingDatasource
    val networkConfigState: StateFlow<NetworkConfig?> = settingDatasource.networkConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null) // Start null, share while subscribed

    // Currently selected tab index from TabDatasource
    val selectedTabIndexState: StateFlow<Int?> = tabDatasource.selectedTabIndexFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null) // Start null

    // List of available tabs (currently static, could be a Flow if dynamic)
    val tabItems: List<TabInfo> = tabDatasource.getTabs()

    // --- StateFlow for Settings Screen Visibility ---
    // Controls whether the main SettingsLayout is shown
    private val _showSettingsScreen = MutableStateFlow(false) // Use MutableStateFlow internally
    val showSettingsScreenState: StateFlow<Boolean> = _showSettingsScreen.asStateFlow() // Expose as immutable StateFlow

    // --- StateFlow for Connection Configuration Dialog Visibility ---
    // Controls whether the specific ConnectionConfigDialog (IP/Port) is shown
    private val _showConnectionConfigDialog = MutableStateFlow(false) // Use MutableStateFlow internally
    val showConnectionConfigDialogState: StateFlow<Boolean> = _showConnectionConfigDialog.asStateFlow() // Expose as immutable StateFlow

    // --- StateFlow for 'Keep Screen On' Preference ---
    // Exposes the keepScreenOn setting from SettingDatasource as a StateFlow
    val keepScreenOnState: StateFlow<Boolean> = settingDatasource.keepScreenOnFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false) // Default to false

    // --- Internal State ---
    private var udpSender: UdpSender? = null // Instance for sending UDP commands
    private var udpSenderJob: Job? = null // Job observing network config changes

    // --- FreeForm Layout State ---
    // Flow determining the layout ID based on the selected tab
    private val currentLayoutIdFlow: Flow<String?> = selectedTabIndexState
        .filterNotNull()
        .map { index ->
            // Map tab index/title to a specific layout ID (example logic)
            if (index >= 0 && index < tabItems.size) {
                when (tabItems[index].title) {
                    "Free Form 1" -> "freeform_1"
                    "Free Form 2" -> "freeform_2"
                    else -> null // Not a FreeForm tab
                }
            } else {
                null
            }
        }
        .distinctUntilChanged() // Only emit when the layout ID actually changes

    // StateFlow containing the list of items for the *currently selected* FreeForm layout
    val currentFreeFormItemsState: StateFlow<List<FreeFormItemState>> = currentLayoutIdFlow
        .flatMapLatest { layoutId -> // Switch to the new layout's flow when layoutId changes
            if (layoutId != null) {
                Log.d(_tag, "Loading items for layoutId: $layoutId")
                layoutDatasource.getLayoutFlow(layoutId) // Get flow from datasource
            } else {
                Log.d(_tag, "No FreeForm layout selected, emitting empty list.")
                flowOf(emptyList()) // Emit empty list if no FreeForm tab selected
            }
        }
        .catch { e -> // Handle errors during flow collection
            Log.e(_tag, "Error loading free form items", e)
            emit(emptyList()) // Emit empty list on error
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) // Initial state is empty


    // --- Initialization ---
    init {
        Log.d(_tag, "ViewModel initialized")
        observeNetworkConfig() // Start observing network config changes
        checkInitialSetup() // Check if IP/Port need to be set on first launch
    }

    /**
     * Observes the networkConfigFlow and creates/updates the UdpSender instance accordingly.
     * Cancels the previous observer job if it exists.
     */
    private fun observeNetworkConfig() {
        udpSenderJob?.cancel() // Cancel any existing job
        udpSenderJob = viewModelScope.launch {
            settingDatasource.networkConfigFlow.collect { config -> // Collect emissions from the flow
                Log.d(_tag, "Network config changed: $config")
                // Create a new UdpSender if config is valid, otherwise set to null
                udpSender = config?.let { (ip, port) ->
                    if (ip != null && port != null) {
                        Log.i(_tag, "Creating/Updating UdpSender for $ip:$port")
                        UdpSender(ip, port)
                    } else {
                        Log.w(_tag, "Invalid network config, clearing UdpSender.")
                        null
                    }
                }
            }
        }
    }

    /**
     * Checks if the initial network configuration (IP/Port) is missing.
     * If so, triggers the Connection Configuration Dialog to be shown by updating the StateFlow.
     */
    private fun checkInitialSetup() {
        viewModelScope.launch {
            // Get the first value emitted by the flow (the current stored value)
            val config = settingDatasource.networkConfigFlow.firstOrNull()
            if (config?.ip == null || config.port == null) {
                Log.d(_tag, "Initial config missing, requesting connection config show.")
                _showConnectionConfigDialog.value = true // Update the MutableStateFlow
            }
        }
    }


    // --- Event Handlers ---

    /**
     * Saves the provided IP address and port number using the SettingDatasource.
     * Hides the Connection Configuration Dialog upon successful save by updating the StateFlow.
     *
     * @param ip The IP address string.
     * @param port The port number.
     */
    fun saveConnectionSettings(ip: String, port: Int) {
        viewModelScope.launch {
            settingDatasource.saveSettings(ip, port)
            _showConnectionConfigDialog.value = false // Update the MutableStateFlow
            Log.i(_tag, "Connection settings saved via ViewModel.")
        }
    }

    /**
     * Updates the 'Keep Screen On' preference using the SettingDatasource.
     *
     * @param enabled The desired state (true to enable, false to disable).
     */
    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingDatasource.saveKeepScreenOn(enabled)
            Log.d(_tag, "Keep screen on set to: $enabled")
        }
    }

    /**
     * Sends a command string via UDP to the configured target.
     * Shows the Connection Configuration Dialog if the UdpSender is not available.
     *
     * @param commandIdentifier The command string to send.
     * @param contextForToast Context needed to display a Toast message on failure.
     */
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
            _showConnectionConfigDialog.value = true // Update the MutableStateFlow
        }
    }

    /**
     * Saves the selected tab index using the TabDatasource.
     *
     * @param index The index of the tab to select.
     */
    fun selectTab(index: Int) {
        viewModelScope.launch {
            Log.d(_tag, "Saving selected tab index: $index")
            tabDatasource.saveSelectedTabIndex(index)
        }
    }

    // --- Functions to Control Settings Screen/Dialog Visibility ---

    /** Shows the main SettingsLayout screen by updating the StateFlow. */
    fun showSettingsScreen() {
        Log.d(_tag, "Showing settings screen.")
        _showSettingsScreen.value = true // Update the MutableStateFlow
    }

    /** Hides the main SettingsLayout screen by updating the StateFlow. */
    fun hideSettingsScreen() {
        Log.d(_tag, "Hiding settings screen.")
        _showSettingsScreen.value = false // Update the MutableStateFlow
    }

    /** Shows the Connection Configuration Dialog (IP/Port settings) by updating the StateFlow. */
    fun showConnectionConfigDialog() {
        Log.d(_tag, "Showing connection config dialog.")
        _showConnectionConfigDialog.value = true // Update the MutableStateFlow
        // Optionally hide the main settings screen when showing the dialog
        // _showSettingsScreen.value = false
    }

    /**
     * Hides the Connection Configuration Dialog by updating the StateFlow.
     * Only allows hiding if the network configuration is valid (IP/Port are set).
     * Shows a Toast message if hiding is attempted during initial setup without saving.
     *
     * @param contextForToast Optional context to show a Toast if hiding fails due to invalid config.
     */
    fun hideConnectionConfigDialog(contextForToast: Context? = null) {
        viewModelScope.launch {
            val config = networkConfigState.value // Get current config state
            if (config?.ip != null && config.port != null) {
                // Allow hiding if config is valid
                Log.d(_tag, "Hiding connection config dialog.")
                _showConnectionConfigDialog.value = false // Update the MutableStateFlow
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

    // --- FreeForm Layout Event Handlers --- (Existing code unchanged)
    /** Saves the entire list of items for the current FreeForm layout. */
    fun saveFreeFormLayout(items: List<FreeFormItemState>) { /* ... */ }
    /** Saves changes to a single FreeForm item within the current layout. */
    fun saveFreeFormItem(updatedItem: FreeFormItemState) { /* ... */ }
    /** Adds a new item to the current FreeForm layout. */
    fun addFreeFormItem(text: String, commandString: String, type: FreeFormItemType, textSizeSp: Float?, backgroundColorHex: String?) { /* ... */ }
    /** Deletes an item from the current FreeForm layout by its ID. */
    fun deleteFreeFormItem(itemId: String) { /* ... */ }


    // --- Cleanup ---
    override fun onCleared() {
        super.onCleared()
        udpSenderJob?.cancel() // Cancel the observer job
        udpSender?.close() // Close the UDP socket if open
        Log.d(_tag, "ViewModel cleared, UdpSender observer cancelled and sender closed.")
    }
}
