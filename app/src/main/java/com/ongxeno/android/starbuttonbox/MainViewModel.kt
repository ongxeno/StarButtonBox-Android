package com.ongxeno.android.starbuttonbox

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ongxeno.android.starbuttonbox.data.ConnectionStatus
import com.ongxeno.android.starbuttonbox.data.FreeFormItemState
import com.ongxeno.android.starbuttonbox.data.LayoutType
import com.ongxeno.android.starbuttonbox.data.Macro
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import com.ongxeno.android.starbuttonbox.data.toUi
import com.ongxeno.android.starbuttonbox.datasource.ConnectionManager
import com.ongxeno.android.starbuttonbox.datasource.LayoutRepository
import com.ongxeno.android.starbuttonbox.datasource.MacroRepository
import com.ongxeno.android.starbuttonbox.datasource.SettingDatasource
import com.ongxeno.android.starbuttonbox.ui.layout.LayoutInfo
import com.ongxeno.android.starbuttonbox.ui.layout.toLayoutInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingDatasource: SettingDatasource,
    private val layoutRepository: LayoutRepository,
    private val macroRepository: MacroRepository,
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _tag = "MainViewModel"

    // --- Loading State ---
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // --- Expose Connection Status from ConnectionManager ---
    val connectionStatus: StateFlow<ConnectionStatus> = connectionManager.connectionStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionStatus.NO_CONFIG)

    // --- New: Expose Latest Response Time from ConnectionManager ---
    val latestResponseTimeMs: StateFlow<Long?> = connectionManager.latestResponseTimeMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)


    // --- State Flows for UI (Layouts, etc.) ---
    val networkConfigState: StateFlow<NetworkConfig?> = settingDatasource.networkConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedLayoutIndexState: StateFlow<Int> = layoutRepository.selectedLayoutIndexFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val enabledLayoutsState: StateFlow<List<LayoutInfo>> =
        layoutRepository.enabledLayoutsFlow
            .map { layoutEntities ->
                layoutEntities.map { entity -> entity.toLayoutInfo() }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAddLayoutDialog = MutableStateFlow(false)
    val showAddLayoutDialogState: StateFlow<Boolean> = _showAddLayoutDialog.asStateFlow()

    private val _showConnectionConfigDialog = MutableStateFlow(false)
    val showConnectionConfigDialogState: StateFlow<Boolean> =
        _showConnectionConfigDialog.asStateFlow()

    val keepScreenOnState: StateFlow<Boolean> = settingDatasource.keepScreenOnFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val selectedLayoutIdFlow: Flow<String?> = combine(
        selectedLayoutIndexState, enabledLayoutsState
    ) { index, enabledLayouts -> enabledLayouts.getOrNull(index)?.id }
        .distinctUntilChanged()

    val currentFreeFormItemsState: StateFlow<List<FreeFormItemState>> = selectedLayoutIdFlow
        .flatMapLatest { layoutId ->
            if (layoutId != null) {
                Log.d(_tag, "Selected layout ID changed to: $layoutId, fetching items.")
                layoutRepository.getLayoutItemsFlow(layoutId)
            } else {
                Log.d(_tag, "No layout selected or layout ID is null, emitting empty list for items.")
                flowOf(emptyList())
            }
        }
        .catch { e -> Log.e(_tag, "Error in currentFreeFormItemsState flow", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMacrosState: StateFlow<List<Macro>> = macroRepository.getAllMacros()
        .map { list -> list.map { entity -> entity.toUi() }}
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Initialization ---
    init {
        Log.d(_tag, "ViewModel initialized. ConnectionManager state: ${connectionManager.getCurrentConnectionStatus()}")
        // Network config observation is now primarily handled by ConnectionManager.
        // MainViewModel can observe connectionManager.connectionStatus for UI updates.
        initializeAppData()
    }

    private fun initializeAppData() {
        viewModelScope.launch {
            _isLoading.value = true // Start loading
            Log.d(_tag, "MainViewModel: Starting app data initialization/checks...")
            try {
                // 1. Check initial network config for dialog display.
                val initialConfig = settingDatasource.networkConfigFlow.firstOrNull()
                Log.d(_tag, "MainViewModel: Initial network config read: $initialConfig")
                if (initialConfig?.ip.isNullOrBlank() || initialConfig?.port == null) {
                    if (!_showConnectionConfigDialog.value) {
                        _showConnectionConfigDialog.value = true
                    }
                }

                // 2. Default layout population is now handled by LayoutRepository on first launch.
                layoutRepository.addDefaultLayoutsIfFirstLaunch() // Call the new method

                val currentLayouts = layoutRepository.allLayoutsFlow.first() // Check LayoutEntity
                Log.d(_tag, "MainViewModel: Current layouts count from DB: ${currentLayouts.size}")
                if (enabledLayoutsState.value.isEmpty() && currentLayouts.isNotEmpty()) {
                    Log.w(_tag, "MainViewModel: No layouts are enabled, but definitions exist. User might need to enable them in settings.")
                }

            } catch (e: Exception) {
                Log.e(_tag, "Error during MainViewModel app data initialization", e)
            } finally {
                _isLoading.value = false // Finish loading
                Log.d(_tag, "MainViewModel: App data initialization/checks complete.")
            }
        }
    }

    // --- Event Handlers ---

    fun saveConnectionSettings(ip: String, port: Int) {
        viewModelScope.launch {
            settingDatasource.saveSettings(ip, port)
            // ConnectionManager will pick up the new settings via its own collection of networkConfigFlow
            _showConnectionConfigDialog.value = false
            Log.i(_tag, "Connection settings saved via MainViewModel.")
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingDatasource.saveKeepScreenOn(enabled)
            Log.d(_tag, "Keep screen on set to: $enabled")
        }
    }

    fun showConnectionConfigDialog() {
        Log.d(_tag, "Showing connection config dialog from MainViewModel.")
        _showConnectionConfigDialog.value = true
        // ConnectionManager might also be triggered by SettingViewModel if that dialog is used
    }

    fun hideConnectionConfigDialog(contextForToast: Context? = null) {
        viewModelScope.launch {
            val config = networkConfigState.value
            if (config?.ip != null && config.port != null) {
                Log.d(_tag, "Hiding connection config dialog from MainViewModel.")
                _showConnectionConfigDialog.value = false
            } else {
                Log.d(_tag, "Attempted to hide connection config, but config is invalid.")
                contextForToast?.let {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(it, "Please save connection settings first", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun selectLayout(index: Int) {
        viewModelScope.launch {
            Log.d(_tag, "Saving selected layout index: $index")
            layoutRepository.saveSelectedLayoutIndex(index)
        }
    }

    fun requestAddLayout() {
        Log.d(_tag, "Requesting Add Layout Dialog from Main Screen")
        _showAddLayoutDialog.value = true
    }

    fun confirmAddLayout(title: String, iconName: String) {
        viewModelScope.launch {
            // The "+" button on MainScreen always adds a new FreeForm layout
            layoutRepository.addLayout(
                title = title,
                layoutType = LayoutType.FREE_FORM,
                iconName = iconName,
                initialButtons = emptyList() // New FreeForm layouts start empty
            )
            Log.i(_tag, "Added new FreeForm layout from Main Screen: $title")
            _showAddLayoutDialog.value = false

            // Select the newly added layout (it's appended to the end)
            val newLayoutIndex = layoutRepository.allLayoutsFlow.first().size -1
            if(newLayoutIndex >= 0) {
                // Ensure the new layout is enabled before trying to select it in the enabled list
                val newLayoutId = layoutRepository.allLayoutsFlow.first().lastOrNull()?.id
                if (newLayoutId != null) {
                    // Wait for enabledLayoutsState to update if necessary
                    val updatedEnabledLayouts = enabledLayoutsState.first { layouts -> layouts.any {it.id == newLayoutId} }
                    val newEnabledIndex = updatedEnabledLayouts.indexOfFirst { it.id == newLayoutId }
                    if (newEnabledIndex != -1) {
                        selectLayout(newEnabledIndex)
                    }
                }
            }
        }
    }

    fun cancelAddLayout() {
        _showAddLayoutDialog.value = false
    }

    fun saveFreeFormLayout(items: List<FreeFormItemState>) {
        viewModelScope.launch {
            val layoutId = selectedLayoutIdFlow.first()
            if (layoutId != null) {
                Log.d(_tag, "Saving ${items.size} layout items for ID: $layoutId")
                layoutRepository.saveLayoutItems(layoutId, items)
            } else {
                Log.w(_tag, "Cannot save layout items, selectedLayoutId is null.")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // ConnectionManager is a @Singleton, its lifecycle is tied to the application.
        // If it were scoped to this ViewModel, you'd call connectionManager.onCleared() here.
        Log.d(_tag, "MainViewModel cleared.")
    }
}
