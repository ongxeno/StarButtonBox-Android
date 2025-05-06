package com.ongxeno.android.starbuttonbox.ui.screen.setting // Ensure correct package

// Import SettingsSection from its location
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.ui.dialog.ConnectionConfigDialog

/**
 * Composable function for the main settings screen.
 * Uses Scaffold and TopAppBar for consistent structure.
 *
 * @param viewModel The MainViewModel instance.
 */
@OptIn(ExperimentalMaterial3Api::class) // Added OptIn for TopAppBar
@Composable
fun SettingsScreen(
    viewModel: SettingViewModel = hiltViewModel(),
    onNavigateToManageLayouts: () -> Unit,
    onNavigateToManageMacros: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // Collect necessary state from the ViewModel
    val networkConfig by viewModel.networkConfigState.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOnState.collectAsStateWithLifecycle()
    val showConnectionDialog by viewModel.showConnectionConfigDialogState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Prepare display strings for network config
    val ipDisplay = networkConfig?.ip ?: "Not Set"
    val portDisplay = networkConfig?.port?.toString() ?: "Not Set"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
                // No actions needed for this screen's top bar
            )
        }
    ) { paddingValues ->
        // Main column for the settings content, applying padding from Scaffold
        // Make the column scrollable in case content overflows on smaller screens
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Apply padding from Scaffold
                .verticalScroll(rememberScrollState()) // Make content scrollable
                .padding(16.dp) // Add padding around the content inside the scrollable area
        ) {
            // --- Connection Section ---
            SettingsSection(title = "Connection") {
                Text(
                    "Current Target: ${ipDisplay}:${portDisplay}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Button(
                    onClick = { /* TODO: Implement mDNS Discovery Logic */ },
                    enabled = false
                ) {
                    Text("Automatic PC Discovery (TODO)")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { viewModel.showConnectionConfigDialog() }) {
                    Text("Configure Connection Manually")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- Display Section ---
            SettingsSection(title = "Display") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Keep Screen On", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = keepScreenOn,
                        onCheckedChange = { isChecked -> viewModel.setKeepScreenOn(isChecked) }
                    )
                }
                Text(
                    text = "Prevents the screen from turning off automatically while the app is active.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- Manage Layout Section ---
            SettingsSection(title = "Manage Layout") {
                Button(
                    onClick = onNavigateToManageLayouts,
                    enabled = true
                ) {
                    Text("Manage Layouts / Tabs")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- Key Binding Section ---
            SettingsSection(title = "Key Binding") {
                Button(
                    onClick = onNavigateToManageMacros,
                    enabled = false
                ) {
                    Text("Configure Key Bindings (TODO)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp)) // Add some space at the bottom
        }

        if (showConnectionDialog) {
            ConnectionConfigDialog(
                onDismissRequest = { viewModel.hideConnectionConfigDialog(context) },
                onSave = { ip, port -> viewModel.saveConnectionSettings(ip, port) },
                networkConfigFlow = viewModel.networkConfigState,
            )
        }
    }
}

// Note: SettingsSection remains defined here for now.
// Consider moving it to a common 'components' package if used elsewhere.
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}
