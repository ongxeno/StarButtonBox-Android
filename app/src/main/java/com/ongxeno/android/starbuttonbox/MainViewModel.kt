package com.ongxeno.android.starbuttonbox

import android.content.Context // Keep context import for Toast
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ongxeno.android.starbuttonbox.data.FreeFormItemState
import com.ongxeno.android.starbuttonbox.data.FreeFormItemType
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import com.ongxeno.android.starbuttonbox.datasource.LayoutDatasource // Import LayoutDatasource
import com.ongxeno.android.starbuttonbox.datasource.SettingDatasource
import com.ongxeno.android.starbuttonbox.datasource.TabDatasource
import com.ongxeno.android.starbuttonbox.datasource.UdpSender
import com.ongxeno.android.starbuttonbox.ui.model.TabInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.* // Import necessary Flow operators
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel for the main application screen.
 * Uses Hilt for dependency injection.
 * Manages UI state and interactions, including FreeForm layouts.
 */
@OptIn(ExperimentalCoroutinesApi::class) // For flatMapLatest
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingDatasource: SettingDatasource,
    private val tabDatasource: TabDatasource,
    private val layoutDatasource: LayoutDatasource // Inject LayoutDatasource
    // Inject other dependencies like SoundPlayer, VibratorManagerUtils if needed directly here
) : ViewModel() {

    private val _tag = "MainViewModel"

    // --- State ---

    // Network Configuration Flow exposed as StateFlow
    val networkConfigState: StateFlow<NetworkConfig?> = settingDatasource.networkConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Selected Tab Index Flow exposed as StateFlow
    val selectedTabIndexState: StateFlow<Int?> = tabDatasource.selectedTabIndexFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Tab definitions (consider making TabDatasource provide a Flow if tabs can change)
    val tabItems: List<TabInfo> = tabDatasource.getTabs() // Assuming tabs are static for now

    // State for controlling settings dialog/screen visibility
    private val _showSettings = mutableStateOf(false)
    val showSettings: State<Boolean> = _showSettings

    // Internal state for the UdpSender instance
    private var udpSender: UdpSender? = null
    private var udpSenderJob: Job? = null

    // --- FreeForm Layout State ---
    // Determine the current layoutId based on the selected tab
    private val currentLayoutIdFlow: Flow<String?> = selectedTabIndexState
        .filterNotNull() // Only proceed if tab index is not null
        .map { index ->
            if (index >= 0 && index < tabItems.size) {
                // Check if the selected tab's content lambda corresponds to a FreeFormLayout
                // This requires a way to identify FreeForm tabs, e.g., by title prefix or a dedicated type field in TabInfo
                // For now, let's assume titles "Free Form 1", "Free Form 2" map to IDs "freeform_1", "freeform_2"
                val tabTitle = tabItems[index].title
                when (tabTitle) {
                    "Free Form 1" -> "freeform_1"
                    "Free Form 2" -> "freeform_2"
                    // Add other FreeForm tabs here
                    else -> null // Not a FreeForm tab
                }
            } else {
                null // Invalid index
            }
        }
        .distinctUntilChanged() // Only react to changes in layoutId

    // StateFlow for the items of the *currently selected* FreeForm layout
    val currentFreeFormItemsState: StateFlow<List<FreeFormItemState>> = currentLayoutIdFlow
        .flatMapLatest { layoutId ->
            if (layoutId != null) {
                Log.d(_tag, "Loading items for layoutId: $layoutId")
                // Get the flow for the specific layoutId from the datasource
                layoutDatasource.getLayoutFlow(layoutId)
            } else {
                Log.d(_tag, "No FreeForm layout selected, emitting empty list.")
                flowOf(emptyList()) // Emit empty list if no FreeForm layout is selected
            }
        }
        .catch { e ->
            Log.e(_tag, "Error loading free form items", e)
            emit(emptyList()) // Emit empty list on error
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()) // Initial value is empty list


    init {
        Log.d(_tag, "ViewModel initialized")
        observeNetworkConfig()
        checkInitialSetup()
    }

    private fun observeNetworkConfig() {
        udpSenderJob?.cancel()
        udpSenderJob = viewModelScope.launch {
            settingDatasource.networkConfigFlow
                .onEach { config ->
                    Log.d(_tag, "Network config changed: $config")
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
                .collect()
        }
    }

    private fun checkInitialSetup() {
        viewModelScope.launch {
            val config = settingDatasource.networkConfigFlow.firstOrNull()
            if (config?.ip == null || config.port == null) {
                Log.d(_tag, "Initial config missing, requesting settings show.")
                _showSettings.value = true
            }
        }
    }


    // --- Events ---

    fun saveSettings(ip: String, port: Int) {
        viewModelScope.launch {
            settingDatasource.saveSettings(ip, port)
            _showSettings.value = false
            Log.i(_tag, "Settings saved via ViewModel.")
        }
    }

    fun sendCommand(commandIdentifier: String, contextForToast: Context) {
        val currentSender = udpSender
        if (currentSender != null) {
            Log.d(_tag, "Sending command: $commandIdentifier via $currentSender")
            currentSender.sendCommandAction(commandIdentifier)
        } else {
            Log.w(_tag, "SendCommand failed: UdpSender not available (Settings likely missing).")
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(contextForToast, "Settings required", Toast.LENGTH_SHORT).show()
            }
            _showSettings.value = true
        }
    }

    fun selectTab(index: Int) {
        viewModelScope.launch {
            Log.d(_tag, "Saving selected tab index: $index")
            tabDatasource.saveSelectedTabIndex(index)
        }
    }

    fun showSettingsDialog() {
        Log.d(_tag, "Showing settings dialog/screen.")
        _showSettings.value = true
    }

    fun hideSettingsDialog(contextForToast: Context) {
        viewModelScope.launch {
            val config = networkConfigState.value
            if (config?.ip != null && config.port != null) {
                Log.d(_tag, "Hiding settings dialog/screen.")
                _showSettings.value = false
            } else {
                Log.d(_tag, "Attempted to hide settings, but config is invalid.")
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(contextForToast, "Please save settings first", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- FreeForm Layout Event Handlers ---

    // Saves the entire layout (e.g., after drag/resize or add/delete)
    fun saveFreeFormLayout(items: List<FreeFormItemState>) {
        viewModelScope.launch {
            val layoutId = currentLayoutIdFlow.firstOrNull() // Get current layout ID
            if (layoutId != null) {
                Log.d(_tag, "Saving layout for ID: $layoutId")
                layoutDatasource.saveLayout(layoutId, items)
            } else {
                Log.w(_tag, "Cannot save layout, currentLayoutId is null.")
            }
        }
    }

    // Saves changes to a single item (e.g., from edit dialog)
    fun saveFreeFormItem(updatedItem: FreeFormItemState) {
        viewModelScope.launch {
            val layoutId = currentLayoutIdFlow.firstOrNull()
            if (layoutId != null) {
                // Create a new list with the updated item
                val currentItems = currentFreeFormItemsState.value.toMutableList()
                val index = currentItems.indexOfFirst { it.id == updatedItem.id }
                if (index != -1) {
                    currentItems[index] = updatedItem
                    Log.d(_tag, "Saving single item update for ID: ${updatedItem.id} in layout $layoutId")
                    layoutDatasource.saveLayout(layoutId, currentItems)
                } else {
                    Log.w(_tag, "Item not found for saving: ${updatedItem.id}")
                }
            } else {
                Log.w(_tag, "Cannot save item, currentLayoutId is null.")
            }
        }
    }

    // Adds a new item
    fun addFreeFormItem(text: String, commandString: String, type: FreeFormItemType, textSizeSp: Float?, backgroundColorHex: String?) {
        viewModelScope.launch {
            val layoutId = currentLayoutIdFlow.firstOrNull()
            if (layoutId != null) {
                val newItem = FreeFormItemState(
                    text = text,
                    commandString = commandString,
                    type = type,
                    textSizeSp = textSizeSp,
                    backgroundColorHex = backgroundColorHex
                    // Default gridCol/Row/Width/Height are set in data class constructor
                )
                val currentItems = currentFreeFormItemsState.value.toMutableList()
                currentItems.add(newItem)
                Log.d(_tag, "Adding new item to layout $layoutId")
                layoutDatasource.saveLayout(layoutId, currentItems)
            } else {
                Log.w(_tag, "Cannot add item, currentLayoutId is null.")
            }
        }
    }

    // Deletes an item
    fun deleteFreeFormItem(itemId: String) {
        viewModelScope.launch {
            val layoutId = currentLayoutIdFlow.firstOrNull()
            if (layoutId != null) {
                val currentItems = currentFreeFormItemsState.value.toMutableList()
                val removed = currentItems.removeIf { it.id == itemId }
                if(removed) {
                    Log.d(_tag, "Deleting item $itemId from layout $layoutId")
                    layoutDatasource.saveLayout(layoutId, currentItems)
                } else {
                    Log.w(_tag, "Item not found for deletion: $itemId")
                }
            } else {
                Log.w(_tag, "Cannot delete item, currentLayoutId is null.")
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        udpSenderJob?.cancel()
        udpSender?.close()
        Log.d(_tag, "ViewModel cleared, UdpSender observer cancelled.")
    }
}
