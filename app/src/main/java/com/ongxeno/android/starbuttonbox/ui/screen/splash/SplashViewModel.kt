package com.ongxeno.android.starbuttonbox.ui.screen.splash // Or a more general ui.splash package

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ongxeno.android.starbuttonbox.datasource.LayoutRepository
import com.ongxeno.android.starbuttonbox.datasource.SettingDatasource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val settingDatasource: SettingDatasource,
    private val layoutRepository: LayoutRepository
) : ViewModel(), SplashViewModelInterface {

    private val _isLoadingFlow = MutableStateFlow(true)
    override val isLoading: StateFlow<Boolean> = _isLoadingFlow.asStateFlow()

    private val _isFirstLaunchFlow = MutableStateFlow(false)
    override val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunchFlow.asStateFlow()

    private val _isInitializationCompleteFlow = MutableStateFlow(false)
    override val isInitializationComplete: StateFlow<Boolean> = _isInitializationCompleteFlow.asStateFlow()

    init {
        initiateSetup()
    }

    private fun initiateSetup() {
        viewModelScope.launch {
            _isLoadingFlow.value = true
            _isInitializationCompleteFlow.value = false

            try {
                // Check if it's the first launch
                val firstLaunch = settingDatasource.isFirstLaunchFlow.first()
                _isFirstLaunchFlow.value = firstLaunch

                // Ensure network config is read/cached
                val networkConfig = settingDatasource.networkConfigFlow.first()

                if (firstLaunch) {
                    val currentLayouts = layoutRepository.allLayoutDefinitionsFlow.first()
                    if (currentLayouts.isEmpty()) {
                        layoutRepository.addDefaultLayouts()
                    }
                }

                // All critical async setup tasks are done
                _isInitializationCompleteFlow.value = true

            } catch (_: Exception) {
                _isInitializationCompleteFlow.value = true
            } finally {
                _isLoadingFlow.value = false
            }
        }
    }

    /**
     * Called by the SplashScreen UI after initialization is complete and
     * just before navigating away, if it was determined to be a first launch.
     */
    override fun markFirstLaunchCompletedInViewModel() { // Override
        viewModelScope.launch {
            if (_isFirstLaunchFlow.value) {
                settingDatasource.setFirstLaunchCompleted()
                _isFirstLaunchFlow.value = false
            }
        }
    }
}