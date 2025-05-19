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
import kotlinx.coroutines.flow.collect
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
    private val MAX_CACHED_LAYOUTS = 3

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val connectionStatus: StateFlow<ConnectionStatus> = connectionManager.connectionStatus
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionStatus.NO_CONFIG)

    val latestResponseTimeMs: StateFlow<Long?> = connectionManager.latestResponseTimeMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val networkConfigState: StateFlow<NetworkConfig?> = settingDatasource.networkConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _showAddLayoutDialog = MutableStateFlow(false)
    val showAddLayoutDialogState: StateFlow<Boolean> = _showAddLayoutDialog.asStateFlow()

    private val _showConnectionConfigDialog = MutableStateFlow(false)
    val showConnectionConfigDialogState: StateFlow<Boolean> = _showConnectionConfigDialog.asStateFlow()

    val keepScreenOnState: StateFlow<Boolean> = settingDatasource.keepScreenOnFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val selectedLayoutIndexState: StateFlow<Int> = layoutRepository.selectedLayoutIndexFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val enabledLayoutsState: StateFlow<List<LayoutInfo>> =
        layoutRepository.enabledLayoutsFlow
            .map { layoutEntities ->
                layoutEntities.map { entity -> entity.toLayoutInfo() }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMacrosState: StateFlow<List<Macro>> = macroRepository.getAllMacros()
        .map { list -> list.map { entity -> entity.toUi() }}
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val recentlyUsedLayoutIds = ArrayDeque<String>(MAX_CACHED_LAYOUTS)

    private val _cachedLayoutsToRender = MutableStateFlow<List<LayoutInfo>>(emptyList())
    val cachedLayoutsToRenderState: StateFlow<List<LayoutInfo>> = _cachedLayoutsToRender.asStateFlow()

    val selectedLayoutIdFlow: Flow<String?> = combine(
        selectedLayoutIndexState, enabledLayoutsState
    ) { index, currentEnabledLayouts -> currentEnabledLayouts.getOrNull(index)?.id }
        .distinctUntilChanged()

    val currentFreeFormItemsState: StateFlow<List<FreeFormItemState>> = selectedLayoutIdFlow
        .flatMapLatest { layoutId ->
            if (layoutId != null) {
                val layoutInfo = _cachedLayoutsToRender.value.find { it.id == layoutId }
                if (layoutInfo?.type == LayoutType.FREE_FORM) {
                    layoutRepository.getLayoutItemsFlow(layoutId)
                } else {
                    flowOf(emptyList())
                }
            } else {
                flowOf(emptyList())
            }
        }
        .catch { e -> Log.e(_tag, "Error in currentFreeFormItemsState flow", e); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        Log.d(_tag, "ViewModel initialized.")
        initializeAppData()

        viewModelScope.launch {
            combine(enabledLayoutsState, selectedLayoutIndexState) { layouts, currentIndex ->
                if (layouts.isNotEmpty()) {
                    val selectedLayout = layouts.getOrNull(currentIndex)

                    if (selectedLayout != null) {
                        if (recentlyUsedLayoutIds.contains(selectedLayout.id)) {
                            recentlyUsedLayoutIds.remove(selectedLayout.id)
                        }
                        recentlyUsedLayoutIds.addFirst(selectedLayout.id)
                        while (recentlyUsedLayoutIds.size > MAX_CACHED_LAYOUTS) {
                            recentlyUsedLayoutIds.removeLast()
                        }
                        _cachedLayoutsToRender.value = layouts.filter { layoutInfo ->
                            recentlyUsedLayoutIds.contains(layoutInfo.id)
                        }.distinctBy { it.id }
                    } else if (layouts.isNotEmpty() && recentlyUsedLayoutIds.isEmpty()) {
                        val firstLayout = layouts.first()
                        recentlyUsedLayoutIds.addFirst(firstLayout.id)
                        _cachedLayoutsToRender.value = listOf(firstLayout)
                    }
                } else {
                    recentlyUsedLayoutIds.clear()
                    _cachedLayoutsToRender.value = emptyList()
                }
            }.collect()
        }
    }

    private fun initializeAppData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val initialConfig = settingDatasource.networkConfigFlow.firstOrNull()
                if (initialConfig?.ip.isNullOrBlank() || initialConfig?.port == null) {
                    if (!_showConnectionConfigDialog.value) _showConnectionConfigDialog.value = true
                }
                layoutRepository.addDefaultLayoutsIfFirstLaunch()
                val currentEnabled = enabledLayoutsState.first()
                val currentIndex = selectedLayoutIndexState.first()
                if (currentEnabled.isNotEmpty() && (currentIndex < 0 || currentIndex >= currentEnabled.size)) {
                    layoutRepository.saveSelectedLayoutIndex(0)
                }
            } catch (e: Exception) {
                Log.e(_tag, "Error during MainViewModel app data initialization", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveConnectionSettings(ip: String, port: Int) {
        viewModelScope.launch {
            settingDatasource.saveSettings(ip, port)
            _showConnectionConfigDialog.value = false
        }
    }

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
                val selectedLayout = currentEnabledLayouts[index]
                if (recentlyUsedLayoutIds.contains(selectedLayout.id)) {
                    recentlyUsedLayoutIds.remove(selectedLayout.id)
                }
                recentlyUsedLayoutIds.addFirst(selectedLayout.id)
                while (recentlyUsedLayoutIds.size > MAX_CACHED_LAYOUTS) {
                    recentlyUsedLayoutIds.removeLast()
                }
                _cachedLayoutsToRender.value = currentEnabledLayouts.filter { layoutInfo ->
                    recentlyUsedLayoutIds.contains(layoutInfo.id)
                }.distinctBy { it.id }
                layoutRepository.saveSelectedLayoutIndex(index)
            } else if (currentEnabledLayouts.isNotEmpty() && index != selectedLayoutIndexState.value) {
                selectLayout(0)
            } else if (currentEnabledLayouts.isEmpty()){
                layoutRepository.saveSelectedLayoutIndex(0)
                _cachedLayoutsToRender.value = emptyList()
                recentlyUsedLayoutIds.clear()
            }
        }
    }

    fun requestAddLayout() { _showAddLayoutDialog.value = true }

    fun confirmAddLayout(title: String, iconName: String) {
        viewModelScope.launch {
            layoutRepository.addLayout(title, LayoutType.FREE_FORM, iconName, emptyList())
            _showAddLayoutDialog.value = false
            val allLayouts = layoutRepository.allLayoutsFlow.first()
            val newLayout = allLayouts.maxByOrNull { it.orderIndex }
            if (newLayout != null) {
                val newLayoutInfo = newLayout.toLayoutInfo()
                val enabled = enabledLayoutsState.first()
                val newIndexInEnabled = enabled.indexOfFirst { it.id == newLayoutInfo.id }
                if (newIndexInEnabled != -1) {
                    selectLayout(newIndexInEnabled)
                } else if (enabled.isNotEmpty()){
                    selectLayout(enabled.size -1)
                }
            }
        }
    }

    fun cancelAddLayout() { _showAddLayoutDialog.value = false }

    fun saveFreeFormLayout(layoutId: String, items: List<FreeFormItemState>) {
        viewModelScope.launch {
            Log.i(_tag, "saveFreeFormLayout: Saving ${items.size} items for layout ID: $layoutId")
            layoutRepository.saveLayoutItems(layoutId, items)
        }
    }

    override fun onCleared() { super.onCleared(); Log.d(_tag, "MainViewModel cleared.") }
}
