package com.ongxeno.android.starbuttonbox.ui.screen.setup

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info // For instruction icon
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import com.ongxeno.android.starbuttonbox.navigation.AppScreenRoute
import com.ongxeno.android.starbuttonbox.ui.theme.OnDarkPrimary
import com.ongxeno.android.starbuttonbox.ui.theme.OrangeDarkPrimary
// Assuming SettingDatasource has a companion object with DEFAULT_PORT
import com.ongxeno.android.starbuttonbox.datasource.SettingDatasource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupStartScreen(
    navController: NavController,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    // val context = LocalContext.current // Context is used in NetworkConfigUI

    LaunchedEffect(currentStep) {
        if (currentStep == SetupStep.SETUP_COMPLETE) {
            navController.navigate(AppScreenRoute.Main.route) {
                popUpTo(AppScreenRoute.Splash.route) { inclusive = true }
                popUpTo(AppScreenRoute.SetupStartScreen.route) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentStep) {
                            SetupStep.SERVER_QUERY -> "PC Server Status"
                            SetupStep.SERVER_INSTRUCTIONS -> "PC Server Setup Guide"
                            SetupStep.NETWORK_CONFIG -> "Network Configuration"
                            SetupStep.SETUP_COMPLETE -> "Completing Setup..."
                        }
                    )
                }
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentStep,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            transitionSpec = {
                (slideInHorizontally { height -> height } + fadeIn()).togetherWith(
                    slideOutHorizontally { height -> -height } + fadeOut())
            }, label = "SetupStepAnimation"
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                when (step) {
                    SetupStep.SERVER_QUERY -> ServerQueryUI(viewModel)
                    SetupStep.SERVER_INSTRUCTIONS -> ServerInstructionsUI(viewModel)
                    SetupStep.NETWORK_CONFIG -> NetworkConfigUI(viewModel)
                    SetupStep.SETUP_COMPLETE -> {
                        CircularProgressIndicator()
                        Text("Finalizing...", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerQueryUI(viewModel: SetupViewModel) {
    Text(
        "Is the StarButtonBox PC server software already installed and running on your PC?",
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 24.dp)
    )
    Button(
        onClick = { viewModel.answerServerQuery(isServerAlreadySetup = true) },
        modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 8.dp)
    ) {
        Text("Yes, it's set up and running")
    }
    Button(
        onClick = { viewModel.answerServerQuery(isServerAlreadySetup = false) },
        modifier = Modifier.fillMaxWidth(0.8f).padding(vertical = 8.dp)
    ) {
        Text("No, I need to set it up")
    }
}

@Composable
private fun ServerInstructionsUI(viewModel: SetupViewModel) {
    val ktorServerUrl by viewModel.ktorServerUrl.collectAsStateWithLifecycle()
    val isKtorRunning by viewModel.isKtorServerRunning.collectAsStateWithLifecycle()

    Text(
        "PC Server Setup Instructions",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 16.dp)
    )
    Text(
        "To use StarButtonBox, you need to run a small server application on your PC.",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    if (!isKtorRunning || ktorServerUrl == "Starting server...") {
        CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
        Text(ktorServerUrl ?: "Initializing local server...", style = MaterialTheme.typography.bodySmall)
    } else if (ktorServerUrl?.startsWith("Error:") == true) {
        Text(ktorServerUrl!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    } else if (ktorServerUrl != null) {
        Text(
            "1. On your PC, open a web browser and go to the following address:",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            ktorServerUrl!!,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Text(
            "2. Follow the instructions on that page to download and run StarButtonBoxServer_Installer_v1.0.exe.",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "3. Ensure your PC and this Android device are on the same Wi-Fi network.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "4. After installation, launch \"StarButtonBox Server\" and click \"Start Server\" in its control panel.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "5. If prompted by Windows Firewall, allow access for Private networks.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

    }

    Spacer(modifier = Modifier.height(32.dp))
    Button(
        onClick = { viewModel.proceedToNetworkConfig() },
        modifier = Modifier.fillMaxWidth(0.8f),
        enabled = isKtorRunning && ktorServerUrl?.startsWith("http") == true
    ) {
        Text("Continue to Network Setup")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkConfigUI(viewModel: SetupViewModel) {
    val discoveryState by viewModel.discoveryState.collectAsStateWithLifecycle()
    var manualIpAddress by remember { mutableStateOf("") }
    var manualPort by remember { mutableStateOf(SettingDatasource.TARGET_PORT_DEFAULT.toString()) } // Use default
    var automaticallySelectedServer by remember { mutableStateOf<NetworkConfig?>(null) }
    var selectedMode by remember { mutableStateOf(ConfigMode.AUTOMATIC) }

    val context = LocalContext.current

    LaunchedEffect(selectedMode) {
        if (selectedMode == ConfigMode.AUTOMATIC) {
            viewModel.startNsdDiscovery()
        } else {
            viewModel.stopNsdDiscovery()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopNsdDiscovery()
        }
    }

    Text(
        "Connect to PC Server",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        ConfigMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selectedMode == mode,
                onClick = { selectedMode = mode },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ConfigMode.entries.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = OrangeDarkPrimary,
                    activeContentColor = OnDarkPrimary,
                ),
                label = { Text(mode.label) }
            )
        }
    }

    // General Instruction for Network Config
    Text(
        text = "Ensure the StarButtonBox Server is running on your PC and on the same Wi-Fi network.",
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    AnimatedContent(targetState = selectedMode, label = "ConfigModeAnimation") { mode ->
        Column(modifier = Modifier.fillMaxWidth()) {
            when (mode) {
                ConfigMode.AUTOMATIC -> {
                    AutomaticDiscoveryContentInternal(
                        discoveryState = discoveryState,
                        selectedServer = automaticallySelectedServer,
                        onServerSelected = { server -> automaticallySelectedServer = server },
                        onRefreshClicked = { viewModel.startNsdDiscovery() }
                    )
                }
                ConfigMode.MANUAL -> {
                    ManualConfigContentInternal(
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
    }

    Spacer(modifier = Modifier.height(24.dp))
    Button(
        onClick = {
            val ipToSave: String?
            val portToSave: Int?

            if (selectedMode == ConfigMode.AUTOMATIC) {
                ipToSave = automaticallySelectedServer?.ip
                portToSave = automaticallySelectedServer?.port
            } else {
                ipToSave = manualIpAddress
                portToSave = manualPort.toIntOrNull()
            }

            if (ipToSave.isNullOrBlank() || portToSave == null || portToSave !in 1..65535) {
                android.widget.Toast.makeText(context, "Invalid IP or Port.", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                viewModel.saveNetworkConfigurationAndFinish(ipToSave, portToSave)
            }
        },
        modifier = Modifier.fillMaxWidth(0.8f),
        enabled = (selectedMode == ConfigMode.MANUAL && manualIpAddress.isNotBlank() && manualPort.toIntOrNull() != null) ||
                (selectedMode == ConfigMode.AUTOMATIC && automaticallySelectedServer != null)
    ) {
        Text("Save and Finish Setup")
    }
}

private enum class ConfigMode(val label: String) {
    AUTOMATIC("Automatic Discovery"),
    MANUAL("Manual Entry")
}

@Composable
private fun ManualConfigContentInternal(
    ipAddress: String,
    port: String,
    onIpChange: (String) -> Unit,
    onPortChange: (String) -> Unit
) {
    // Instruction for Manual Mode
    InstructionText(
        text = "If automatic discovery fails or you prefer manual entry, find the IP Address and Port displayed in the StarButtonBox Server application window on your PC (usually under 'Server Status')."
    )
    OutlinedTextField(
        value = ipAddress,
        onValueChange = onIpChange,
        label = { Text("PC Server IP Address") },
        placeholder = { Text("e.g., 192.168.1.100") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
    OutlinedTextField(
        value = port,
        onValueChange = onPortChange,
        label = { Text("PC Server Port (e.g., 5055)") }, // Updated default example
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AutomaticDiscoveryContentInternal(
    discoveryState: SetupDiscoveryState,
    selectedServer: NetworkConfig?,
    onServerSelected: (NetworkConfig) -> Unit,
    onRefreshClicked: () -> Unit
) {
    // Instruction for Automatic Mode
    InstructionText(
        text = "Searching for StarButtonBox servers on your local network. Select your PC server from the list below. If no servers appear, ensure the server is running on your PC, mDNS is enabled in its settings, and try refreshing. If issues persist, switch to 'Manual Entry'."
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val statusText = when (discoveryState) {
            SetupDiscoveryState.Idle -> "Tap refresh to search"
            SetupDiscoveryState.Searching -> "Searching..."
            is SetupDiscoveryState.Discovered -> if (discoveryState.servers.isEmpty()) "No servers found." else "Select a server:"
            is SetupDiscoveryState.Error -> "Error: ${discoveryState.message}"
        }
        Text(statusText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        if (discoveryState is SetupDiscoveryState.Searching) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            IconButton(onClick = onRefreshClicked) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh Discovery")
            }
        }
    }

    if (discoveryState is SetupDiscoveryState.Discovered && discoveryState.servers.isNotEmpty()) {
        Column(modifier = Modifier.heightIn(max = 160.dp).verticalScroll(rememberScrollState())) { // Reduced max height slightly
            discoveryState.servers.forEach { server ->
                ServerListItemInternal(
                    server = server,
                    isSelected = server == selectedServer,
                    onClick = { onServerSelected(server) }
                )
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServerListItemInternal(
    server: NetworkConfig,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text("${server.ip}:${server.port}") },
        leadingContent = { Icon(Icons.Filled.Lan, contentDescription = "Server") },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
        )
    )
}

@Composable
private fun InstructionText(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = "Info",
            modifier = Modifier.size(20.dp).padding(end = 8.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

