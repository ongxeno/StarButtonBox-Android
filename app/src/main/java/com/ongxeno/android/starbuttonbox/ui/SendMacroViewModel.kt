package com.ongxeno.android.starbuttonbox.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ongxeno.android.starbuttonbox.datasource.ConnectionManager
import com.ongxeno.android.starbuttonbox.datasource.MacroRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class SendMacroViewModel @Inject constructor(
    private val macroRepository: MacroRepository,
    private val connectionManager: ConnectionManager, // Inject ConnectionManager
    private val json: Json // Inject Json for serializing InputAction
) : ViewModel() {

    private val _tag = "SendMacroViewModel"

    // NetworkConfigState is now managed and observed by ConnectionManager
    // UdpSender and its job are no longer needed here.

    init {
        Log.d(_tag, "ViewModel initialized, ConnectionManager: $connectionManager")
    }

    fun sendMacro(macroId: String?) {
        if (macroId == null) {
            Log.w(_tag, "SendMacro: No Macro ID provided.")
            return
        }

        viewModelScope.launch(Dispatchers.IO) { // Perform DB operation off the main thread
            val macro = macroRepository.getMacroById(macroId)
            if (macro == null) {
                Log.w(_tag, "SendMacro: Macro with ID '$macroId' not found.")
                return@launch
            }

            val inputAction = macro.effectiveInputAction
            if (inputAction == null) {
                Log.w(_tag, "SendMacro: Macro '${macro.title}' (ID: $macroId) has no effective input action defined.")
                return@launch
            }

            try {
                // Serialize the InputAction to JSON string
                val inputActionJson = json.encodeToString(inputAction)

                // Delegate sending to ConnectionManager
                // The ConnectionManager will handle wrapping this into a UdpPacket
                // and managing ACKs.
                Log.i(_tag, "Requesting to send action for macro: ${macro.title} (ID: $macroId)")
                connectionManager.sendMacroCommand(inputActionJson, macro.title)

            } catch (e: Exception) {
                Log.e(_tag, "Error serializing InputAction for macro '${macro.title}': $inputAction", e)
            }
        }
    }

    // onCleared will be handled by Hilt for ConnectionManager if it's a @Singleton.
    // If ConnectionManager's lifecycle needs to be tied more directly to this ViewModel
    // (e.g., if ConnectionManager was not a Singleton), you'd call connectionManager.onCleared() here.
    // Since ConnectionManager is a @Singleton, its onCleared will be called when the app scope ends.
    override fun onCleared() {
        super.onCleared()
        Log.d(_tag, "SendMacroViewModel cleared.")
        // If ConnectionManager was scoped to this ViewModel:
        // connectionManager.onCleared()
    }
}
