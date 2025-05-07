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
import com.ongxeno.android.starbuttonbox.data.LayoutDefinition
import com.ongxeno.android.starbuttonbox.data.LayoutType
import com.ongxeno.android.starbuttonbox.data.Macro
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import com.ongxeno.android.starbuttonbox.data.toUi
import com.ongxeno.android.starbuttonbox.datasource.ConnectionManager
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingDatasource: SettingDatasource,
    private val layoutRepository: LayoutRepository,
    private val macroRepository: MacroRepository,
    private val connectionManager: ConnectionManager // Inject ConnectionManager
) : ViewModel() {

    private val _tag = "MainViewModel"

    // --- Loading State ---
    private val _isLoading = MutableStateFlow(true)
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
            if (layoutId != null) layoutRepository.getLayoutItemsFlow(layoutId)
            else flowOf(emptyList())
        }
        .catch { e -> Log.e(_tag, "Error loading free form items", e); emit(emptyList()) }
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
            _isLoading.value = true
            Log.d(_tag, "Starting app data initialization...")
            try {
                // 1. Check initial network config for dialog display (ConnectionManager handles its own logic)
                val initialConfig = settingDatasource.networkConfigFlow.firstOrNull()
                Log.d(_tag, "initializeAppData: Initial network config read: $initialConfig")
                if (initialConfig?.ip.isNullOrBlank() || initialConfig?.port == null) {
                    // Show dialog if no config, ConnectionManager will also see this and be in NO_CONFIG
                    if (!_showConnectionConfigDialog.value) {
                        _showConnectionConfigDialog.value = true
                    }
                }

                // 2. Check if default layouts need to be added
                val currentDefinitions = layoutRepository.layoutDefinitionsFlow.first()
                Log.d(_tag, "initializeAppData: Definitions count from source: ${currentDefinitions.size}")
                if (currentDefinitions.isEmpty()) {
                    Log.i(_tag, "Layout definitions are empty, adding default.")
                    layoutRepository.addDefaultLayouts()
                    enabledLayoutsState.filter { it.isNotEmpty() }.first()
                }

            } catch (e: Exception) {
                Log.e(_tag, "Error during app initialization", e)
            } finally {
                Log.d(_tag, "App data initialization complete. Setting isLoading to false.")
                delay(500) // Small delay for UI to settle if needed
                _isLoading.value = false
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
            val newId = "freeform_${UUID.randomUUID()}"
            val newLayout = LayoutDefinition(
                id = newId, title = title, layoutType = LayoutType.FREE_FORM,
                iconName = iconName, isEnabled = true, isUserDefined = true,
                isDeletable = true, layoutItemsJson = null
            )
            layoutRepository.addLayout(newLayout)
            Log.i(_tag, "Added new layout from Main Screen: $newLayout")
            _showAddLayoutDialog.value = false
        }
    }

    fun cancelAddLayout() {
        _showAddLayoutDialog.value = false
    }

    fun saveFreeFormLayout(items: List<FreeFormItemState>) {
        viewModelScope.launch {
            val layoutId = selectedLayoutIdFlow.first()
            if (layoutId != null) {
                Log.d(_tag, "Saving layout items for ID: $layoutId")
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
