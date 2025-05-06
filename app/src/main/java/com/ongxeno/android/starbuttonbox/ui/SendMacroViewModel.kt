package com.ongxeno.android.starbuttonbox.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import com.ongxeno.android.starbuttonbox.datasource.MacroRepository
import com.ongxeno.android.starbuttonbox.datasource.SettingDatasource
import com.ongxeno.android.starbuttonbox.datasource.UdpSender
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SendMacroViewModel @Inject constructor(
    settingDatasource: SettingDatasource,
    private val macroRepository: MacroRepository,
) : ViewModel() {

    private val _tag = "SendMacroViewModel"

    // --- Screen Visibility State ---

    val networkConfigState: StateFlow<NetworkConfig?> = settingDatasource.networkConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- Internal State ---
    private var udpSender: UdpSender? = null
    private var udpSenderJob: Job? = null

    init {
        observeNetworkConfig()
    }

    private fun observeNetworkConfig() {
        udpSenderJob?.cancel()
        udpSenderJob = viewModelScope.launch {
            networkConfigState.collect { config ->
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
        }
    }

    fun sendMacro(macroId: String?) {
        val currentSender = udpSender
        if (currentSender == null) {
            Log.w(_tag, "SendCommand failed: UdpSender not available (Settings likely missing).")
        } else if (macroId == null) {
            Log.w(_tag, "SendCommand: No Macro ID provided.")
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                val macro = macroRepository.getMacroById(macroId)
                if (macro == null) {
                    Log.w(_tag, "SendCommand: Macro with ID '$macroId' not found.")
                    return@launch
                }

                val action = macro.effectiveInputAction
                if (action == null) {
                    Log.w(
                        _tag,
                        "SendCommand: Macro '${macro.title}' (ID: $macroId) has no effective input action defined."
                    )
                    return@launch
                }

                Log.i(_tag, "Sending action: ${macro.title} via $currentSender")
                currentSender.sendAction(action, macro.title) // Use the sender instance
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        udpSenderJob?.cancel()
        udpSender?.close()
        Log.d(_tag, "ViewModel cleared.")
    }

}