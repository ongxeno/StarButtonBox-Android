package com.ongxeno.android.starbuttonbox.ui.setting

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
import androidx.compose.runtime.setValue // Keep setValue for local state
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import kotlinx.coroutines.flow.StateFlow // Changed from Flow to StateFlow for consistency
// Removed unused imports: rememberCoroutineScope, launch

/**
 * A dialog composable for displaying and editing app settings.
 * Receives state and event handlers, typically from a ViewModel.
 *
 * @param onDismissRequest Lambda called when the dialog should be dismissed.
 * @param onSave Lambda called when settings should be saved (passes new IP and Port).
 * @param networkConfigFlow StateFlow emitting the current NetworkConfig (nullable).
 */
@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit,
    onSave: (ip: String, port: Int) -> Unit, // Changed to simple lambda
    networkConfigFlow: StateFlow<NetworkConfig?>, // Expects StateFlow from ViewModel
) {
    // Collect current values from the StateFlow
    val currentNetworkConfig by networkConfigFlow.collectAsStateWithLifecycle()
    // Use remember with currentNetworkConfig as key to update derived state
    val currentIp by remember(currentNetworkConfig) { mutableStateOf(currentNetworkConfig?.ip) }
    val currentPort by remember(currentNetworkConfig) { mutableStateOf(currentNetworkConfig?.port) }

    // Internal state for the text fields remains local to the dialog
    var ipAddressState by remember { mutableStateOf("") }
    var portState by remember { mutableStateOf("") }

    // Use LaunchedEffect to initialize/update TextField state from the flow
    LaunchedEffect(currentNetworkConfig) {
        currentNetworkConfig?.let { config ->
            // Update local state only if it differs, preventing cursor jumps
            if (ipAddressState != (config.ip ?: "")) {
                ipAddressState = config.ip ?: ""
            }
            if (portState != (config.port?.toString() ?: "")) {
                portState = config.port?.toString() ?: ""
            }
        }
    }

    // Get context for Toast messages
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            // Allow dismiss only if settings are already saved/valid
            dismissOnClickOutside = currentIp != null && currentPort != null,
            dismissOnBackPress = currentIp != null && currentPort != null
        ),
        title = { Text(if (currentIp == null || currentPort == null) "Initial Setup Required" else "Settings") },
        text = {
            Column {
                Text("Configure target PC details:")
                Spacer(modifier = Modifier.height(16.dp))
                // Input field for IP Address
                OutlinedTextField(
                    value = ipAddressState,
                    onValueChange = { ipAddressState = it },
                    label = { Text("Target IP Address") },
                    placeholder = { Text("e.g., 192.168.1.100") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Input field for Port
                OutlinedTextField(
                    value = portState,
                    onValueChange = { newValue ->
                        // Basic validation: only digits, max 5 chars
                        if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                            portState = newValue
                        }
                    },
                    label = { Text("Target Port (1-65535)") },
                    placeholder = { Text("e.g., 5005") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number // Set keyboard type to number
                    )
                )
            }
        },
        confirmButton = {
            // Save Button
            Button(
                onClick = {
                    // Validate input before calling onSave
                    val portInt = portState.toIntOrNull()
                    if (ipAddressState.isBlank()) {
                        Toast.makeText(context, "IP Address cannot be empty", Toast.LENGTH_SHORT)
                            .show()
                    } else if (portInt == null || portInt !in 1..65535) {
                        Toast.makeText(context, "Invalid Port (1-65535)", Toast.LENGTH_SHORT).show()
                    } else {
                        // Call the provided onSave lambda (which triggers ViewModel function)
                        onSave(ipAddressState, portInt)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            // Show Cancel button only if settings were previously saved/valid
            if (currentIp != null && currentPort != null) {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
            // Else: No cancel button if initial setup is required
        }
    )
}
