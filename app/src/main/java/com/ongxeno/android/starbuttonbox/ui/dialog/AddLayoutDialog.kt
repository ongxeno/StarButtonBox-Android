package com.ongxeno.android.starbuttonbox.ui.dialog

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

// Define a list of selectable icons
val availableIcons = listOf(
    Icons.Filled.DashboardCustomize,
    Icons.Filled.Widgets,
    Icons.Filled.Gamepad,
    Icons.Filled.Build,
    Icons.Filled.SettingsInputComponent,
    Icons.Filled.Adjust,
    Icons.Filled.Bolt,
    Icons.Filled.Camera,
    Icons.Filled.Flag,
    Icons.Filled.Flight,
    Icons.Filled.Headset,
    Icons.Filled.Key,
    Icons.Filled.Lightbulb,
    Icons.Filled.Lock,
    Icons.Filled.Map,
    Icons.Filled.Mic,
    Icons.Filled.Navigation,
    Icons.Filled.Power,
    Icons.Filled.Shield,
    Icons.Filled.Speed,
    Icons.Filled.Star,
    Icons.Filled.Tune,
    Icons.Filled.Videocam,
    Icons.Filled.Warning,
    Icons.Filled.WbSunny,
    Icons.Filled.HelpOutline // Default/Fallback
)

// Helper to get the string name of an icon (for saving)
fun getIconName(icon: ImageVector): String {
    return when (icon) {
        Icons.Filled.DashboardCustomize -> "DashboardCustomize"
        Icons.Filled.Widgets -> "Widgets"
        Icons.Filled.Gamepad -> "Gamepad"
        Icons.Filled.Build -> "Build"
        Icons.Filled.SettingsInputComponent -> "SettingsInputComponent"
        Icons.Filled.Adjust -> "Adjust"
        Icons.Filled.Bolt -> "Bolt"
        Icons.Filled.Camera -> "Camera"
        Icons.Filled.Flag -> "Flag"
        Icons.Filled.Flight -> "Flight"
        Icons.Filled.Headset -> "Headset"
        Icons.Filled.Key -> "Key"
        Icons.Filled.Lightbulb -> "Lightbulb"
        Icons.Filled.Lock -> "Lock"
        Icons.Filled.Map -> "Map"
        Icons.Filled.Mic -> "Mic"
        Icons.Filled.Navigation -> "Navigation"
        Icons.Filled.Power -> "Power"
        Icons.Filled.Shield -> "Shield"
        Icons.Filled.Speed -> "Speed"
        Icons.Filled.Star -> "Star"
        Icons.Filled.Tune -> "Tune"
        Icons.Filled.Videocam -> "Videocam"
        Icons.Filled.Warning -> "Warning"
        Icons.Filled.WbSunny -> "WbSunny"
        else -> "HelpOutline" // Default name
    }
}


/**
 * Dialog for adding a new FreeForm layout.
 *
 * @param onDismissRequest Lambda called when the dialog should be dismissed.
 * @param onConfirm Lambda called when the user confirms adding the layout,
 * providing the chosen title and icon name.
 */
@Composable
fun AddLayoutDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (title: String, iconName: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf(Icons.Filled.DashboardCustomize) } // Default icon
    val isTitleValid by remember(title) { derivedStateOf { title.isNotBlank() } }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Add New FreeForm Layout", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Layout Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isTitleValid && title.isNotEmpty() // Show error if blank after typing
                )
                if (!isTitleValid && title.isNotEmpty()) {
                    Text("Title cannot be empty", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }


                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Icon:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                // Icon Selection Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp), // Limit height to prevent overly large dialog
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableIcons) { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = getIconName(icon), // Use name for description
                            modifier = Modifier
                                .size(40.dp)
                                .padding(2.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (selectedIcon == icon) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = MaterialTheme.shapes.extraSmall
                                )
                                .clickable { selectedIcon = icon }
                                .padding(2.dp), // Inner padding after border
                            tint = if (selectedIcon == icon) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(title.trim(), getIconName(selectedIcon)) },
                        enabled = isTitleValid // Enable button only if title is valid
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}