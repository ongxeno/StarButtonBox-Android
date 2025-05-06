package com.ongxeno.android.starbuttonbox

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ongxeno.android.starbuttonbox.data.FreeFormItemState
import com.ongxeno.android.starbuttonbox.data.LayoutDefinition
import com.ongxeno.android.starbuttonbox.data.LayoutType
import com.ongxeno.android.starbuttonbox.data.Macro
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import com.ongxeno.android.starbuttonbox.data.toUi
import com.ongxeno.android.starbuttonbox.datasource.LayoutRepository
import com.ongxeno.android.starbuttonbox.datasource.MacroRepository
import com.ongxeno.android.starbuttonbox.datasource.SettingDatasource
import com.ongxeno.android.starbuttonbox.ui.layout.LayoutInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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
    private val layoutRepository: LayoutRepository,
    private val macroRepository: MacroRepository,
) : ViewModel() {

    private val _tag = "MainViewModel"

    // --- Loading State ---
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- State Flows for UI ---
    val networkConfigState: StateFlow<NetworkConfig?> = settingDatasource.networkConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Selected index relative to the *enabled* list
    val selectedLayoutIndexState: StateFlow<Int> = layoutRepository.selectedLayoutIndexFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Only need the *enabled* layouts for the main UI tabs
    val enabledLayoutsState: StateFlow<List<LayoutInfo>> =
        layoutRepository.enabledLayoutDefinitionsFlow
            .map { definitions ->
                definitions.map {
                    LayoutInfo(
                        id = it.id,
                        title = it.title,
                        iconName = it.iconName,
                        type = it.layoutType,
                        isEnabled = it.isEnabled
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Add Layout Dialog State (for main screen '+' button) ---
    private val _showAddLayoutDialog = MutableStateFlow(false) // Re-added state
    val showAddLayoutDialogState: StateFlow<Boolean> = _showAddLayoutDialog.asStateFlow()

    private val _showConnectionConfigDialog = MutableStateFlow(false)
    val showConnectionConfigDialogState: StateFlow<Boolean> =
        _showConnectionConfigDialog.asStateFlow()

    // --- Keep Screen On State ---
    val keepScreenOnState: StateFlow<Boolean> = settingDatasource.keepScreenOnFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- FreeForm Layout Items State ---
    // Still needed for the currently selected tab's content
    private val selectedLayoutIdFlow: Flow<String?> = combine(
        selectedLayoutIndexState, enabledLayoutsState
    ) { index, enabledLayouts -> enabledLayouts.getOrNull(index)?.id }
        .distinctUntilChanged()

    val currentFreeFormItemsState: StateFlow<List<FreeFormItemState>> = selectedLayoutIdFlow
        .flatMapLatest { layoutId ->
            if (layoutId != null) layoutRepository.getLayoutItemsFlow(layoutId)
            else flowOf(emptyList())
        }
        .catch { e -> Log.e(_tag, "Error loading free form items", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // State for ALL macros (used by AddEditButtonDialog)
    // Filter by a specific game if needed, e.g., Star Citizen
    val allMacrosState: StateFlow<List<Macro>> = macroRepository.getAllMacros() // Or getMacrosByGameId(Game.STAR_CITIZEN_4_1_1.id)
        .map { list -> list.map { entity -> entity.toUi() }}
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Initialization ---
    init {
        Log.d(_tag, "ViewModel initialized")
        observeNetworkConfig()
        initializeAppData() // Keep initialization logic here
    }

    private fun observeNetworkConfig() {
        viewModelScope.launch {
            networkConfigState.collect { config -> // Collect from StateFlow
                Log.d(_tag, "Network config changed: $config")
            }
        }
    }

    private fun initializeAppData() {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d(_tag, "Starting app data initialization...")
            try {
                // 1. Check network config
                val config = settingDatasource.networkConfigFlow.firstOrNull()
                Log.d(_tag, "initializeAppData: Network config read: $config")
                if (config?.ip.isNullOrBlank() || config?.port == null) {
                    if (!_showConnectionConfigDialog.value) { _showConnectionConfigDialog.value = true }
                }

                // 2. Check if default layouts need to be added
                val currentDefinitions = layoutRepository.layoutDefinitionsFlow.first()
                Log.d(_tag, "initializeAppData: Definitions count from source: ${currentDefinitions.size}")
                if (currentDefinitions.isEmpty()) {
                    Log.i(_tag, "Layout definitions are empty, adding default.")
                    layoutRepository.addDefaultLayouts() // Use the updated function name
                    // Wait for the enabled list to potentially update
                    enabledLayoutsState.filter { it.isNotEmpty() }.first() // Wait if needed
                }

            } catch (e: Exception) {
                Log.e(_tag, "Error during app initialization", e)
            } finally {
                Log.d(_tag, "App data initialization complete. Setting isLoading to false.")
                delay(500)
                _isLoading.value = false
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

    // Layout / Tab Selection
    /** Saves the index of the selected layout (relative to the enabled layouts list). */
    fun selectLayout(index: Int) {
        viewModelScope.launch {
            Log.d(_tag, "Saving selected layout index: $index")
            layoutRepository.saveSelectedLayoutIndex(index)
        }
    }

    // --- Add Layout Events (Triggered from Main Screen) ---
    /** Shows the dialog to add a new layout. */
    fun requestAddLayout() { // Re-added function
        Log.d(_tag, "Requesting Add Layout Dialog from Main Screen")
        _showAddLayoutDialog.value = true
    }

    /** Creates and saves a new FreeForm layout based on user input from the dialog. */
    fun confirmAddLayout(title: String, iconName: String) { // Re-added function (simplified for Add only)
        viewModelScope.launch {
            val newId = "freeform_${UUID.randomUUID()}"
            val newLayout = LayoutDefinition(
                id = newId, title = title, layoutType = LayoutType.FREE_FORM,
                iconName = iconName, isEnabled = true, isUserDefined = true,
                isDeletable = true, layoutItemsJson = null
            )
            layoutRepository.addLayout(newLayout)
            Log.i(_tag, "Added new layout from Main Screen: $newLayout")
            _showAddLayoutDialog.value = false // Hide dialog after adding
        }
    }

    /** Cancels the add layout operation and hides the dialog. */
    fun cancelAddLayout() { // Re-added function
        _showAddLayoutDialog.value = false
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
}
