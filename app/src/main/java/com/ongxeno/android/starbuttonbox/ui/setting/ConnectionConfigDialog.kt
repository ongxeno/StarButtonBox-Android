package com.ongxeno.android.starbuttonbox.ui.setting

import android.content.Context // Import context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import kotlinx.coroutines.flow.StateFlow

/**
 * A dialog composable specifically for displaying and editing network connection settings (IP/Port).
 * Replaces the general SettingsDialog for this specific purpose.
 *
 * @param onDismissRequest Lambda called when the dialog should be dismissed.
 * Accepts an optional Context, which is provided only if dismissal
 * is attempted during initial setup without saving (to show a Toast).
 * @param onSave Lambda called when settings should be saved (passes new IP and Port).
 * This typically calls a ViewModel function like `saveConnectionSettings`.
 * @param networkConfigFlow StateFlow emitting the current NetworkConfig (nullable) from the ViewModel.
 */
@Composable
fun ConnectionConfigDialog(
    onDismissRequest: (context: Context?) -> Unit, // Now accepts optional context
    onSave: (ip: String, port: Int) -> Unit,
    networkConfigFlow: StateFlow<NetworkConfig?>,
) {
    // Collect the current network config state from the flow provided by the ViewModel
    val currentNetworkConfig by networkConfigFlow.collectAsStateWithLifecycle()
    // Extract current IP and Port for checking if it's initial setup
    val currentIp = currentNetworkConfig?.ip
    val currentPort = currentNetworkConfig?.port

    // Local state for the text fields within the dialog
    var ipAddressState by remember { mutableStateOf("") }
    var portState by remember { mutableStateOf("") }

    // Use LaunchedEffect to initialize/update TextField state when the networkConfigFlow changes
    // This ensures the fields reflect the current saved settings when the dialog opens
    LaunchedEffect(currentNetworkConfig) {
        currentNetworkConfig?.let { config ->
            // Update local state only if it actually differs from the flow's value
            // This prevents unnecessary recompositions and cursor jumps in the TextFields
            if (ipAddressState != (config.ip ?: "")) {
                ipAddressState = config.ip ?: ""
            }
            if (portState != (config.port?.toString() ?: "")) {
                portState = config.port?.toString() ?: ""
            }
        }
    }

    // Get the current context (needed for Toasts)
    val context = LocalContext.current
    // Determine if this is the initial setup (IP or Port is null/missing)
    val isInitialSetup = currentIp == null || currentPort == null

    AlertDialog(
        // Called when the user clicks outside or presses back
        // Pass context only if it's initial setup, so the ViewModel can show a "Save first" Toast
        onDismissRequest = { onDismissRequest(if(isInitialSetup) context else null) },
        properties = DialogProperties(
            // Prevent dismissing by clicking outside or pressing back during initial setup
            dismissOnClickOutside = !isInitialSetup,
            dismissOnBackPress = !isInitialSetup
        ),
        // Set the dialog title based on whether it's initial setup or regular editing
        title = { Text(if (isInitialSetup) "Initial Connection Setup" else "Connection Settings") },
        text = {
            // Column layout for the dialog content
            Column {
                Text("Configure target PC IP and Port:")
                Spacer(modifier = Modifier.height(16.dp)) // Spacing

                // Input field for IP Address
                OutlinedTextField(
                    value = ipAddressState,
                    onValueChange = { ipAddressState = it }, // Update local state on change
                    label = { Text("Target IP Address") },
                    placeholder = { Text("e.g., 192.168.1.100") },
                    singleLine = true // Ensure single line input
                )
                Spacer(modifier = Modifier.height(8.dp)) // Spacing

                // Input field for Port
                OutlinedTextField(
                    value = portState,
                    onValueChange = { newValue ->
                        // Basic validation: allow only digits, max 5 characters
                        if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                            portState = newValue // Update local state if valid
                        }
                    },
                    label = { Text("Target Port (1-65535)") },
                    placeholder = { Text("e.g., 5005") },
                    singleLine = true, // Ensure single line input
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number // Use number keyboard
                    )
                )
            }
        },
        // Confirm button (Save)
        confirmButton = {
            Button(
                onClick = {
                    // Validate input before calling the onSave lambda
                    val portInt = portState.toIntOrNull() // Safely convert port string to Int
                    if (ipAddressState.isBlank()) {
                        // Show error if IP is empty
                        Toast.makeText(context, "IP Address cannot be empty", Toast.LENGTH_SHORT).show()
                    } else if (portInt == null || portInt !in 1..65535) {
                        // Show error if port is invalid
                        Toast.makeText(context, "Invalid Port (1-65535)", Toast.LENGTH_SHORT).show()
                    } else {
                        // If valid, call the provided onSave lambda (triggers ViewModel function)
                        onSave(ipAddressState, portInt)
                    }
                }
            ) {
                Text("Save")
            }
        },
        // Dismiss button (Cancel)
        dismissButton = {
            // Show the Cancel button *only* if it's NOT the initial setup
            if (!isInitialSetup) {
                TextButton(onClick = { onDismissRequest(null) }) { // No context needed for normal cancel
                    Text("Cancel")
                }
            }
            // Otherwise (initial setup), no Cancel button is shown, forcing Save or Back (if allowed)
        }
    )
}
