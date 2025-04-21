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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * A dialog composable for displaying and editing app settings.
 * Assumes the caller handles conditional display.
 * Reads current settings from Flows and saves using a suspend function.
 * Handles potentially null initial values from the flows.
 *
 * @param onDismissRequest Lambda called when the dialog should be dismissed.
 * @param onSave Suspend lambda called when settings should be saved (passes new IP and Port).
 * @param ipAddressFlow Flow emitting the current target IP address (nullable).
 * @param portFlow Flow emitting the current target port number (nullable).
 */
@Composable
fun SettingsDialog(
    onDismissRequest: () -> Unit,
    onSave: suspend (ip: String, port: Int) -> Unit,
    networkConfigFlow: Flow<NetworkConfig?>,
) {
    // Collect current values from Flows, initial value is null
    val currentNetworkConfig by networkConfigFlow.collectAsStateWithLifecycle(initialValue = null)
    val currentIp by remember(currentNetworkConfig) { mutableStateOf(currentNetworkConfig?.ip) }
    val currentPort by remember(currentNetworkConfig) { mutableStateOf(currentNetworkConfig?.port) }

    // Internal state for the text fields
    var ipAddressState by remember { mutableStateOf("") }
    var portState by remember { mutableStateOf("") }

    // Use LaunchedEffect to initialize/update TextField state only when
    // the collected value changes *and* is not null (or after initial null).
    // This prevents resetting user input if the flow re-emits null temporarily.
    LaunchedEffect(currentNetworkConfig) {
        currentNetworkConfig?.let { (currentIp, currentPort) ->
            ipAddressState = currentIp ?: ""
            portState = currentPort?.toString() ?: ""
        }
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = currentIp != null && currentPort != null,
            dismissOnBackPress = currentIp != null && currentPort != null
        ),
        title = { Text(if (currentIp == null || currentPort == null) "Initial Setup Required" else "Settings") },
        text = {
            Column {
                Text("Configure target PC details:")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = ipAddressState,
                    onValueChange = { ipAddressState = it },
                    label = { Text("Target IP Address") },
                    placeholder = { Text("e.g., 192.168.1.100") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = portState,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                            portState = newValue
                        }
                    },
                    label = { Text("Target Port (1-65535)") },
                    placeholder = { Text("e.g., 5005") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val portInt = portState.toIntOrNull()
                    if (ipAddressState.isBlank()) {
                        Toast.makeText(context, "IP Address cannot be empty", Toast.LENGTH_SHORT).show()
                    } else if (portInt == null || portInt !in 1..65535) {
                        Toast.makeText(context, "Invalid Port (1-65535)", Toast.LENGTH_SHORT).show()
                    } else {
                        scope.launch {
                            onSave(ipAddressState, portInt)
                        }
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            if (currentIp != null && currentPort != null) {
                TextButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            }
        }
    )
}
