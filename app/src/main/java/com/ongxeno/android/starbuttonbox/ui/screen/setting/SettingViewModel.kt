package com.ongxeno.android.starbuttonbox.ui.screen.setting // Or your preferred package

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import com.ongxeno.android.starbuttonbox.datasource.SettingDatasource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val settingDatasource: SettingDatasource
) : ViewModel() {

    private val _tag = "SettingViewModel"

    // --- State for Connection Config Dialog ---
    private val _showConnectionConfigDialog = MutableStateFlow(false)
    val showConnectionConfigDialogState: StateFlow<Boolean> = _showConnectionConfigDialog.asStateFlow()

    // --- Expose necessary settings state for the UI ---
    val networkConfigState: StateFlow<NetworkConfig?> = settingDatasource.networkConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val keepScreenOnState: StateFlow<Boolean> = settingDatasource.keepScreenOnFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        Log.d(_tag, "ViewModel initialized")
    }

    // --- Event Handlers specific to SettingScreen ---

    /** Saves network connection settings and hides the config dialog. */
    fun saveConnectionSettings(ip: String, port: Int) {
        viewModelScope.launch {
            settingDatasource.saveSettings(ip, port)
            _showConnectionConfigDialog.value = false // Hide dialog after saving
            Log.i(_tag, "Connection settings saved via SettingViewModel.")
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

    // Add other logic specific to settings if needed (e.g., mDNS discovery state)

    override fun onCleared() {
        super.onCleared()
        Log.d(_tag, "ViewModel cleared.")
    }
}
