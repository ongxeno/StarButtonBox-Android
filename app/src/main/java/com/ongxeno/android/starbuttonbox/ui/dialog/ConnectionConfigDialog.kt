package com.ongxeno.android.starbuttonbox.ui.dialog

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import com.ongxeno.android.starbuttonbox.ui.screen.setting.DiscoveryState
import com.ongxeno.android.starbuttonbox.ui.screen.setting.SettingViewModel
import kotlinx.coroutines.flow.StateFlow

private enum class ConfigMode(val label: String) {
    AUTOMATIC("Automatic"),
    MANUAL("Manual")
}

/**
 * A dialog composable for displaying and editing network connection settings (IP/Port).
 * Includes modes for Automatic Discovery and Manual Configuration, styled with M3 components.
 *
 * @param viewModel The SettingViewModel instance providing discovery state and actions.
 * @param onDismissRequest Lambda called when the dialog should be dismissed. Accepts optional Context.
 * @param onSave Lambda called when settings should be saved (passes new IP and Port).
 * @param networkConfigFlow StateFlow emitting the current NetworkConfig (nullable) from the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class) // Needed for SegmentedButtonRow
@Composable
fun ConnectionConfigDialog(
    viewModel: SettingViewModel, // Pass the ViewModel
    onDismissRequest: (context: Context?) -> Unit,
    onSave: (ip: String, port: Int) -> Unit,
    networkConfigFlow: StateFlow<NetworkConfig?>,
) {
    // --- Collect States from ViewModel ---
    val currentNetworkConfig by networkConfigFlow.collectAsStateWithLifecycle()
    val discoveryState by viewModel.discoveryState.collectAsStateWithLifecycle()

    // --- Local UI State ---
    var selectedMode by remember { mutableStateOf(ConfigMode.AUTOMATIC) } // Default to Automatic
    // Manual input state
    var manualIpAddress by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf("") }
    // State to hold the server selected from the automatic list
    var automaticallySelectedServer by remember { mutableStateOf<NetworkConfig?>(null) }

    // Initialize manual fields when dialog opens or config changes
    LaunchedEffect(currentNetworkConfig) {
        currentNetworkConfig?.let { config ->
            if (manualIpAddress != (config.ip ?: "")) {
                manualIpAddress = config.ip ?: ""
            }
            if (manualPort != (config.port?.toString() ?: "")) {
                manualPort = config.port?.toString() ?: ""
            }
            // Optional: Default to manual mode if already configured
            // if (config.ip != null && config.port != null && selectedMode == ConfigMode.AUTOMATIC) {
            //     selectedMode = ConfigMode.MANUAL
            // }
        }
    }

    // Determine if Save button should be enabled
    val isSaveEnabled by remember(selectedMode, manualIpAddress, manualPort, automaticallySelectedServer) {
        derivedStateOf {
            when (selectedMode) {
                ConfigMode.AUTOMATIC -> automaticallySelectedServer != null
                ConfigMode.MANUAL -> {
                    val portInt = manualPort.toIntOrNull()
                    // Basic IP format check (very lenient)
                    val ipRegex = """^\d{1,3}(\.\d{1,3}){3}$""".toRegex()
                    ipRegex.matches(manualIpAddress) && portInt != null && portInt in 1..65535
                }
            }
        }
    }

    val context = LocalContext.current
    val isInitialSetup = currentNetworkConfig?.ip == null || currentNetworkConfig?.port == null

    Dialog(
        onDismissRequest = { onDismissRequest(if (isInitialSetup && !isSaveEnabled) context else null) },
        properties = DialogProperties(
            dismissOnClickOutside = !isInitialSetup || isSaveEnabled,
            dismissOnBackPress = !isInitialSetup || isSaveEnabled
        ),
    ) {
        // Use Card for standard M3 dialog appearance
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.padding(16.dp) // Padding around the card
        ) {
            Column(
                modifier = Modifier
                    // Apply standard Material Design padding for dialog content
                    .padding(PaddingValues(all = 24.dp))
                    .verticalScroll(rememberScrollState()) // Make content scrollable if needed
            ) {
                // --- Dialog Title ---
                Text(
                    text = if (isInitialSetup) "Initial Connection Setup" else "Connection Settings",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(16.dp)) // Space after title

                // --- Mode Selection ---
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ConfigMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = selectedMode == mode,
                            onClick = {
                                selectedMode = mode
                                if (mode == ConfigMode.AUTOMATIC) {
                                    automaticallySelectedServer = null
                                    viewModel.startDiscovery()
                                } else {
                                    viewModel.stopDiscovery()
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ConfigMode.entries.size),
                            label = { Text(mode.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp)) // More space after mode selection

                // --- Content based on Mode ---
                Box(modifier = Modifier.heightIn(min = 180.dp)) { // Give content area min height
                    when (selectedMode) {
                        ConfigMode.AUTOMATIC -> {
                            AutomaticDiscoveryContent(
                                discoveryState = discoveryState,
                                selectedServer = automaticallySelectedServer,
                                onServerSelected = { server -> automaticallySelectedServer = server },
                                onRefreshClicked = { viewModel.startDiscovery() }
                            )
                        }
                        ConfigMode.MANUAL -> {
                            ManualConfigContent(
                                ipAddress = manualIpAddress,
                                port = manualPort,
                                onIpChange = { manualIpAddress = it },
                                onPortChange = { newValue ->
                                    if (newValue.all { it.isDigit() } && newValue.length <= 5) {
                                        manualPort = newValue
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp)) // Space before buttons

                // --- Action Buttons ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End // Align buttons to the end
                ) {
                    if (!isInitialSetup) {
                        TextButton(onClick = { onDismissRequest(null) }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp)) // Space between buttons
                    }
                    Button(
                        onClick = {
                            // Save logic depends on the selected mode
                            when (selectedMode) {
                                ConfigMode.AUTOMATIC -> {
                                    automaticallySelectedServer?.let {
                                        if (it.ip != null && it.port != null) {
                                            onSave(it.ip, it.port)
                                        } else {
                                            Toast.makeText(context, "Invalid server data", Toast.LENGTH_SHORT).show()
                                        }
                                    } ?: Toast.makeText(context, "Please select a server", Toast.LENGTH_SHORT).show()
                                }
                                ConfigMode.MANUAL -> {
                                    val portInt = manualPort.toIntOrNull()
                                    val ipRegex = """^\d{1,3}(\.\d{1,3}){3}$""".toRegex()
                                    if (!ipRegex.matches(manualIpAddress)) {
                                        Toast.makeText(context, "Invalid IP Address format", Toast.LENGTH_SHORT).show()
                                    } else if (portInt == null || portInt !in 1..65535) {
                                        Toast.makeText(context, "Invalid Port (1-65535)", Toast.LENGTH_SHORT).show()
                                    } else {
                                        onSave(manualIpAddress, portInt)
                                    }
                                }
                            }
                        },
                        enabled = isSaveEnabled
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}


// --- Composable for Manual Input Fields ---
@Composable
private fun ManualConfigContent(
    ipAddress: String,
    port: String,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit
) {
    Column {
        Text(
            "Manually enter target PC IP and Port:",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp) // Add padding below text
        )
        // Use standard spacing
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = ipAddress,
            onValueChange = onIpChange,
            label = { Text("Target IP Address") },
            placeholder = { Text("e.g., 192.168.1.100") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp)) // Consistent spacing
        OutlinedTextField(
            value = port,
            onValueChange = onPortChange,
            label = { Text("Target Port (1-65535)") },
            placeholder = { Text("e.g., 5005") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// --- Composable for Automatic Discovery UI ---
@Composable
private fun AutomaticDiscoveryContent(
    discoveryState: DiscoveryState,
    selectedServer: NetworkConfig?,
    onServerSelected: (NetworkConfig) -> Unit,
    onRefreshClicked: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) { // Allow column to take width
        // --- Status Row ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), // Add padding below status
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Display status text based on state
            val statusText = when (discoveryState) {
                DiscoveryState.Idle -> "Press Refresh to search"
                DiscoveryState.Searching -> "Searching for servers..."
                is DiscoveryState.Discovered -> if (discoveryState.servers.isEmpty()) "No servers found" else "Select a server:"
                is DiscoveryState.Error -> "Discovery Error"
            }
            Text(
                statusText,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(end = 8.dp) // Give text space
            )

            // Show progress indicator or refresh button
            if (discoveryState == DiscoveryState.Searching) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRefreshClicked) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh Discovery")
                }
            }
        }

        // --- Server List or Error Message ---
        when (discoveryState) {
            is DiscoveryState.Discovered -> {
                if (discoveryState.servers.isNotEmpty()) {
                    // Use LazyColumn within a Box with explicit height for scrollability
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 180.dp) // Define height constraints
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                    ) {
                        LazyColumn {
                            itemsIndexed(discoveryState.servers, key = { _, server -> "${server.ip}:${server.port}" }) { index, server ->
                                ServerListItem(
                                    server = server,
                                    isSelected = server == selectedServer,
                                    onClick = { onServerSelected(server) }
                                )
                                if (index < discoveryState.servers.lastIndex) {
                                    HorizontalDivider(thickness = Dp.Hairline) // Use Hairline divider
                                }
                            }
                        }
                    }
                }
                // "No servers found" text is handled by the statusText above
            }
            is DiscoveryState.Error -> {
                Text(
                    "Error: ${discoveryState.message}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }
            // Idle and Searching states don't need extra content here
            else -> {
                // Optional: Add a placeholder or empty text if needed when Idle/Searching
                // Text("...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// --- Composable for a single item in the discovered server list using ListItem ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerListItem(
    server: NetworkConfig,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text("${server.ip}:${server.port}") },
        leadingContent = {
            Icon(
                Icons.Filled.Lan, // Network/Server Icon
                contentDescription = "Server",
                tint = MaterialTheme.colorScheme.onSurfaceVariant // Use a less prominent color
            )
        },
        trailingContent = {
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary // Use primary color for check
                )
            }
        },
        modifier = Modifier.clickable(
            onClick = onClick,
            role = Role.RadioButton // Semantics for selection
        ),
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface // Use secondary container for selected bg
        )
    )
}
