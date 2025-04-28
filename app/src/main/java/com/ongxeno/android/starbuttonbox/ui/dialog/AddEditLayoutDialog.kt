package com.ongxeno.android.starbuttonbox.ui.dialog

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ongxeno.android.starbuttonbox.ui.model.LayoutInfo
import com.ongxeno.android.starbuttonbox.utils.IconMapper

/**
 * Dialog for adding OR editing a FreeForm layout.
 *
 * @param onDismissRequest Lambda called when the dialog should be dismissed.
 * @param onConfirm Lambda called when the user confirms adding/saving the layout.
 * Provides the chosen title, icon name string, and the existing layout ID (null if adding).
 * @param layoutToEdit Optional LayoutInfo object. If provided, dialog enters 'Edit' mode.
 */
@Composable
fun AddEditLayoutDialog( // Renamed Composable
    onDismissRequest: () -> Unit,
    onConfirm: (title: String, iconName: String, existingId: String?) -> Unit, // Added existingId
    layoutToEdit: LayoutInfo? = null // Optional parameter for editing
) {
    val isEditMode = layoutToEdit != null
    var title by remember { mutableStateOf(layoutToEdit?.title ?: "") } // Initialize with existing title if editing
    var selectedIcon by remember { mutableStateOf(layoutToEdit?.icon ?: Icons.Filled.DashboardCustomize) } // Initialize with existing icon
    val isTitleValid by remember(title) { derivedStateOf { title.isNotBlank() } }

    val dialogTitle = if (isEditMode) "Edit Layout" else "Add New FreeForm Layout"
    val confirmButtonText = if (isEditMode) "Save" else "Add"

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
                Text(dialogTitle, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Layout Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isTitleValid && title.isNotEmpty()
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
                        .heightIn(max = 200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(IconMapper.availableIcons) { icon ->
                        Icon(
                            imageVector = icon,
                            contentDescription = IconMapper.getIconName(icon),
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { selectedIcon = icon }
                                .padding(4.dp)
                                .border(
                                    width = 2.dp,
                                    color = if (selectedIcon == icon) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(2.dp),
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
                        onClick = {
                            // Pass back the title, icon name, and the existing ID (null if adding)
                            onConfirm(title.trim(), IconMapper.getIconName(selectedIcon), layoutToEdit?.id)
                        },
                        enabled = isTitleValid
                    ) {
                        Text(confirmButtonText) // Use dynamic button text
                    }
                }
            }
        }
    }
}
