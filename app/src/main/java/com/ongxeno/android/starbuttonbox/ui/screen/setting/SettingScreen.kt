package com.ongxeno.android.starbuttonbox.ui.screen.setting // Ensure correct package

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Info // For About icon
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.ui.dialog.ConnectionConfigDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingViewModel = hiltViewModel(),
    onNavigateToManageLayouts: () -> Unit,
    onNavigateToManageMacros: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val networkConfig by viewModel.networkConfigState.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOnState.collectAsStateWithLifecycle()
    val showConnectionDialog by viewModel.showConnectionConfigDialogState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- Connection Section ---
            SettingsSection(title = "Connection") {
                Text(
                    "Current Target: ${ipDisplay}:${portDisplay}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )
                Button(
                    onClick = { viewModel.showConnectionConfigDialog() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Text("Configure Connection")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- Display Section ---
            SettingsSection(title = "Display") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
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
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- Manage Data Section ---
            SettingsSection(title = "Manage Data") {
                SettingsListItem(
                    text = "Manage Layouts / Tabs",
                    onClick = onNavigateToManageLayouts
                )
                SettingsListItem(
                    text = "Manage Key Bindings (Macros)",
                    onClick = onNavigateToManageMacros
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // --- About Section ---
            SettingsSection(title = "Application") {
                SettingsListItem(
                    text = "About StarButtonBox",
                    icon = Icons.Filled.Info,
                    onClick = onNavigateToAbout
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showConnectionDialog) {
            ConnectionConfigDialog(
                viewModel = hiltViewModel(),
                onDismissRequest = { toastContext -> viewModel.hideConnectionConfigDialog(toastContext ?: context) },
                onSave = { ip, port -> viewModel.saveConnectionSettings(ip, port) },
                networkConfigFlow = viewModel.networkConfigState,
            )
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsListItem(
    text: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(text, style = MaterialTheme.typography.bodyLarge) },
        leadingContent = if (icon != null) {
            { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            null
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
