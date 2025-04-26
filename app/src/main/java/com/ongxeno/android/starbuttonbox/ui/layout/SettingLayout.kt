package com.ongxeno.android.starbuttonbox.ui.setting

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack // Import ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.MainViewModel

/**
 * Composable function for the main settings screen.
 * Displays settings organized into sections.
 * Replaces the main content area when active.
 *
 * @param viewModel The MainViewModel instance to interact with settings state and actions.
 */
@Composable
fun SettingsLayout(viewModel: MainViewModel) {
    // Collect necessary state from the ViewModel
    val networkConfig by viewModel.networkConfigState.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOnState.collectAsStateWithLifecycle()

    // Prepare display strings for network config, handling null cases
    val ipDisplay = networkConfig?.ip ?: "Not Set"
    val portDisplay = networkConfig?.port?.toString() ?: "Not Set"

    // Main column for the settings layout
    Column(
        modifier = Modifier
            .fillMaxSize() // Take up the whole screen
            .padding(16.dp) // Add padding around the content
        // Apply window insets padding if needed, especially for status/navigation bars
        // .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.Horizontal))
    ) {
        // --- Top Bar section with Back Button and Title ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp) // Add bottom padding
        ) {
            // Back button to navigate away from settings
            IconButton(onClick = { viewModel.hideSettingsScreen() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Use auto-mirrored icon
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(8.dp)) // Space between icon and text
            // Title text for the settings screen
            Text("Settings", style = MaterialTheme.typography.headlineSmall)
        }

        // --- Connection Section ---
        SettingsSection(title = "Connection") {
            // Display the currently configured IP and Port
            Text(
                "Current Target: ${ipDisplay}:${portDisplay}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp) // Add padding below text
            )
            // Placeholder button for future mDNS discovery feature
            Button(
                onClick = { /* TODO: Implement mDNS Discovery Logic */ },
                enabled = false // Disabled for now
            ) {
                Text("Automatic PC Discovery (TODO)")
            }
            Spacer(modifier = Modifier.height(8.dp)) // Spacing between buttons
            // Button to open the manual connection configuration dialog
            Button(onClick = { viewModel.showConnectionConfigDialog() }) {
                Text("Configure Connection Manually")
            }
        }

        // Divider between sections
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // --- Display Section ---
        SettingsSection(title = "Display") {
            // Row for the "Keep Screen On" setting label and switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween // Pushes label and switch apart
            ) {
                Text("Keep Screen On", style = MaterialTheme.typography.bodyMedium)
                // Switch component bound to the ViewModel state
                Switch(
                    checked = keepScreenOn, // Current state from ViewModel
                    // Call ViewModel function when the switch state changes
                    onCheckedChange = { isChecked -> viewModel.setKeepScreenOn(isChecked) }
                )
            }
            // Helper text explaining the setting
            Text(
                text = "Prevents the screen from turning off automatically while the app is active.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, // Use theme color for secondary text
                modifier = Modifier.padding(top = 4.dp) // Add padding above helper text
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // --- Manage Layout Section ---
        SettingsSection(title = "Manage Layout") {
            // Placeholder button for future layout management feature
            Button(
                onClick = { /* TODO: Navigate to Layout Management Screen */ },
                enabled = false // Disabled for now
            ) {
                Text("Manage Layouts (TODO)")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // --- Key Binding Section ---
        SettingsSection(title = "Key Binding") {
            // Placeholder button for future key binding feature
            Button(
                onClick = { /* TODO: Navigate to Key Binding Screen */ },
                enabled = false // Disabled for now
            ) {
                Text("Configure Key Bindings (TODO)")
            }
        }
    }
}

/**
 * A helper composable to create a consistent visual structure for each settings section.
 *
 * @param title The title of the settings section.
 * @param content The composable content specific to this section.
 */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier, // Allow passing modifiers
    content: @Composable ColumnScope.() -> Unit // Content lambda within a ColumnScope
) {
    Column(modifier = modifier.fillMaxWidth()) { // Ensure section takes full width
        // Section title text
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium, // Use appropriate theme typography
            fontWeight = FontWeight.Bold, // Make title bold
            modifier = Modifier.padding(bottom = 8.dp) // Add padding below title
        )
        // Render the specific content provided for this section
        content()
    }
}
