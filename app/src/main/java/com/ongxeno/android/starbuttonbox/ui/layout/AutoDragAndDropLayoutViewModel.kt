package com.ongxeno.android.starbuttonbox.ui.layout

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ongxeno.android.starbuttonbox.datasource.ConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the AutoDragAndDropLayout.
 * Handles user interactions for setting source/destination positions and starting/stopping the auto drag loop.
 *
 * @param connectionManager The manager responsible for network communication with the PC server.
 */
@HiltViewModel
class AutoDragAndDropLayoutViewModel @Inject constructor(
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _tag = "AutoDragDropVM"

    // State to track if the auto drag loop is currently active (or supposed to be active).
    // This state is primarily driven by user interaction on the Start/Stop button.
    // The actual loop execution is on the PC server.
    private val _isLoopingActive = MutableStateFlow(false)
    val isLoopingActive: StateFlow<Boolean> = _isLoopingActive.asStateFlow()

    /**
     * Called when the "Set Source Position" button is clicked.
     * Sends a command to the PC server to capture the current mouse position as the source.
     */
    fun onSetSourceClicked() {
        Log.d(_tag, "Set Source Position button clicked.")
        viewModelScope.launch {
            val success = connectionManager.sendCaptureMousePositionCommand("SRC")
            if (!success) {
                Log.w(_tag, "Failed to queue 'Set Source' command. Check connection.")
                // Optionally, provide user feedback here (e.g., via a Toast or another StateFlow)
            }
        }
    }

    /**
     * Called when the "Set Destination Position" button is clicked.
     * Sends a command to the PC server to capture the current mouse position as the destination.
     */
    fun onSetDestinationClicked() {
        Log.d(_tag, "Set Destination Position button clicked.")
        viewModelScope.launch {
            val success = connectionManager.sendCaptureMousePositionCommand("DES")
            if (!success) {
                Log.w(_tag, "Failed to queue 'Set Destination' command. Check connection.")
            }
        }
    }

    /**
     * Called when the "Start/Stop Auto Drag" button is clicked.
     * Toggles the looping state and sends the corresponding command ("START" or "STOP")
     * to the PC server.
     */
    fun onStartStopClicked() {
        val newLoopState = !_isLoopingActive.value
        _isLoopingActive.value = newLoopState // Update the local UI state immediately

        val commandAction = if (newLoopState) "START" else "STOP"
        Log.d(_tag, "Start/Stop button clicked. New loop state: $newLoopState, Sending command: $commandAction")

        viewModelScope.launch {
            val success = connectionManager.sendAutoDragLoopCommand(commandAction)
            if (!success) {
                Log.w(_tag, "Failed to queue '$commandAction' command. Check connection.")
                // If sending fails, revert the UI state as the command didn't go through
                _isLoopingActive.value = !newLoopState
            }
        }
    }
}
