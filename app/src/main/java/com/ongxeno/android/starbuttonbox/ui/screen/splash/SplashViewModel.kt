package com.ongxeno.android.starbuttonbox.ui.screen.splash // Or a more general ui.splash package

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Import SplashViewModelInterface if it's in a different package
// import com.ongxeno.android.starbuttonbox.ui.SplashViewModelInterface // Example if in ui package
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
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
) : ViewModel(), SplashViewModelInterface { // Implement the interface (ensure it's imported)

    private val _tag = "SplashViewModel"

    private val _isLoadingFlow = MutableStateFlow(true)
    override val isLoading: StateFlow<Boolean> = _isLoadingFlow.asStateFlow()

    private val _isFirstLaunchFlow = MutableStateFlow(false)
    override val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunchFlow.asStateFlow()

    private val _isInitializationCompleteFlow = MutableStateFlow(false)
    override val isInitializationComplete: StateFlow<Boolean> = _isInitializationCompleteFlow.asStateFlow()

    // New StateFlow to indicate if setup flow should be initiated
    private val _navigateToSetupFlow = MutableStateFlow(false)
    override val navigateToSetupFlow: StateFlow<Boolean> = _navigateToSetupFlow.asStateFlow()

    init {
        // Log.d(_tag, "ViewModel initialized. Starting setup initiation.")
        initiateSetup()
    }

    private fun initiateSetup() {
        viewModelScope.launch {
            // Log.d(_tag, "initiateSetup coroutine started.")
            _isLoadingFlow.value = true
            _isInitializationCompleteFlow.value = false
            _navigateToSetupFlow.value = false // Default to false

            try {
                val firstLaunch = settingDatasource.isFirstLaunchFlow.first()
                _isFirstLaunchFlow.value = firstLaunch
                Log.i(_tag, "Is first launch: $firstLaunch")

                val networkConfig = settingDatasource.networkConfigFlow.first()
                // Log.d(_tag, "Initial network config read: $networkConfig")

                if (firstLaunch) {
                    Log.i(_tag, "First launch detected.")
                    // Populate default layouts if it's the first launch
                    val currentLayouts = layoutRepository.allLayoutsFlow.first()
                    if (currentLayouts.isEmpty()) {
                        layoutRepository.addDefaultLayoutsIfFirstLaunch()
                        Log.i(_tag, "Default layouts populated successfully.")
                    } else {
                        Log.w(_tag, "First launch detected, but layouts were NOT empty (${currentLayouts.size} found). Skipping default layout population.")
                    }

                    // Check if network config is missing to trigger setup flow
                    if (networkConfig?.ip.isNullOrBlank() || networkConfig?.port == null) {
                        Log.i(_tag, "Network config is missing on first launch. Navigating to setup.")
                        _navigateToSetupFlow.value = true
                    } else {
                        Log.i(_tag, "Network config found on first launch. Proceeding to main app.")
                    }
                } else {
                    // Log.d(_tag, "Not a first launch, skipping default layout population and setup flow decision based on firstLaunch.")
                }

                _isInitializationCompleteFlow.value = true
                Log.i(_tag, "Initialization complete.")

            } catch (e: Exception) {
                Log.e(_tag, "Error during splash screen setup", e)
                _isInitializationCompleteFlow.value = true // Allow app to proceed even on error
            } finally {
                _isLoadingFlow.value = false
            }
        }
    }

    override fun markFirstLaunchCompletedInViewModel() {
        viewModelScope.launch {
            // Log.d(_tag, "markFirstLaunchCompletedInViewModel called. Current isFirstLaunch state: ${_isFirstLaunchFlow.value}")
            if (_isFirstLaunchFlow.value) {
                settingDatasource.setFirstLaunchCompleted()
                Log.i(_tag, "First launch marked as completed in SettingDatasource.")
                _isFirstLaunchFlow.value = false
                // Log.d(_tag, "_isFirstLaunch state in ViewModel now false.")
            } else {
                // Log.d(_tag, "markFirstLaunchCompletedInViewModel: Not a first launch, or already marked.")
            }
        }
    }
}
