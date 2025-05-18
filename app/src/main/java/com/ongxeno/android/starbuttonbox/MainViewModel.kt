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
import com.ongxeno.android.starbuttonbox.datasource.room.LayoutEntity
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

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val connectionStatus: StateFlow<ConnectionStatus> = connectionManager.connectionStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionStatus.NO_CONFIG)

    val latestResponseTimeMs: StateFlow<Long?> = connectionManager.latestResponseTimeMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
    val showConnectionConfigDialogState: StateFlow<Boolean> = _showConnectionConfigDialog.asStateFlow()

    val keepScreenOnState: StateFlow<Boolean> = settingDatasource.keepScreenOnFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val selectedLayoutIdFlow: Flow<String?> = combine(
        selectedLayoutIndexState, enabledLayoutsState
    ) { index, currentEnabledLayouts -> currentEnabledLayouts.getOrNull(index)?.id }
        .distinctUntilChanged()

    val currentFreeFormItemsState: StateFlow<List<FreeFormItemState>> = selectedLayoutIdFlow
        .flatMapLatest { layoutId ->
            if (layoutId != null) {
                Log.d(_tag, "Selected layout ID changed to: $layoutId, fetching items for FreeFormLayout.")
                layoutRepository.getLayoutItemsFlow(layoutId)
            } else {
                Log.d(_tag, "No layout selected or layout ID is null, emitting empty list for FreeFormLayout items.")
                flowOf(emptyList())
            }
        }
        .catch { e -> Log.e(_tag, "Error in currentFreeFormItemsState flow for MainViewModel", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMacrosState: StateFlow<List<Macro>> = macroRepository.getAllMacros()
        .map { list -> list.map { entity -> entity.toUi() }}
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        Log.d(_tag, "ViewModel initialized.")
        initializeAppData()
    }

    private fun initializeAppData() {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d(_tag, "MainViewModel: Starting app data initialization.")
            try {
                val initialConfig = settingDatasource.networkConfigFlow.firstOrNull()
                if (initialConfig?.ip.isNullOrBlank() || initialConfig?.port == null) {
                    if (!_showConnectionConfigDialog.value) _showConnectionConfigDialog.value = true
                }
                layoutRepository.addDefaultLayoutsIfFirstLaunch()
                val currentLayouts = layoutRepository.allLayoutsFlow.first()
                Log.d(_tag, "MainViewModel: Current layouts count from DB after init: ${currentLayouts.size}")
            } catch (e: Exception) {
                Log.e(_tag, "Error during MainViewModel app data initialization", e)
            } finally {
                _isLoading.value = false
                Log.d(_tag, "MainViewModel: App data initialization complete.")
            }
        }
    }

    fun saveConnectionSettings(ip: String, port: Int) {
        viewModelScope.launch {
            settingDatasource.saveSettings(ip, port)
            _showConnectionConfigDialog.value = false
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch { settingDatasource.saveKeepScreenOn(enabled) }
    }

    fun showConnectionConfigDialog() { _showConnectionConfigDialog.value = true }

    fun hideConnectionConfigDialog(contextForToast: Context? = null) {
        viewModelScope.launch {
            if (networkConfigState.value?.ip != null && networkConfigState.value?.port != null) {
                _showConnectionConfigDialog.value = false
            } else {
                contextForToast?.let { Handler(Looper.getMainLooper()).post { Toast.makeText(it, "Please save connection settings first", Toast.LENGTH_SHORT).show() } }
            }
        }
    }

    fun selectLayout(index: Int) {
        viewModelScope.launch {
            val currentEnabledLayouts = enabledLayoutsState.first()
            if (index >= 0 && index < currentEnabledLayouts.size) {
                layoutRepository.saveSelectedLayoutIndex(index)
            } else if (currentEnabledLayouts.isNotEmpty()) {
                layoutRepository.saveSelectedLayoutIndex(0)
            }
        }
    }

    fun requestAddLayout() { _showAddLayoutDialog.value = true }

    fun confirmAddLayout(title: String, iconName: String) {
        viewModelScope.launch {
            layoutRepository.addLayout(title, LayoutType.FREE_FORM, iconName, emptyList())
            _showAddLayoutDialog.value = false
            val newLayoutId = layoutRepository.allLayoutsFlow.first().lastOrNull()?.id
            if (newLayoutId != null) {
                val updatedEnabledLayouts = enabledLayoutsState.first { layouts -> layouts.any {it.id == newLayoutId} }
                val newEnabledIndex = updatedEnabledLayouts.indexOfFirst { it.id == newLayoutId }
                if (newEnabledIndex != -1) selectLayout(newEnabledIndex)
            }
        }
    }

    fun cancelAddLayout() { _showAddLayoutDialog.value = false }

    fun saveFreeFormLayout(items: List<FreeFormItemState>) {
        viewModelScope.launch {
            val layoutId = selectedLayoutIdFlow.first()
            if (layoutId != null) {
                Log.i(_tag, "saveFreeFormLayout: Saving ${items.size} items for layout ID: $layoutId")
                if (items.isNotEmpty()) {
                    items.forEachIndexed { index, item ->
                        Log.d(_tag, "saveFreeFormLayout: Item $index to save: ID=${item.id}, Text='${item.text}', Pos=(${item.gridCol},${item.gridRow})")
                    }
                }
                layoutRepository.saveLayoutItems(layoutId, items)
            } else {
                Log.w(_tag, "saveFreeFormLayout: Cannot save, selectedLayoutId is null.")
            }
        }
    }

    override fun onCleared() { super.onCleared(); Log.d(_tag, "MainViewModel cleared.") }
}
