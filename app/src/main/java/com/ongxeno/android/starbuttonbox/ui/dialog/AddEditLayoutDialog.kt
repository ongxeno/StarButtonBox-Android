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
import androidx.compose.material.icons.filled.DashboardCustomize
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ongxeno.android.starbuttonbox.ui.screen.managelayout.ManageLayoutInfo
import com.ongxeno.android.starbuttonbox.utils.IconMapper

@Composable
fun AddEditLayoutDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (title: String, iconName: String, existingId: String?) -> Unit,
    layoutToEdit: ManageLayoutInfo? = null
) {
    val isEditMode = layoutToEdit != null
    var title by remember(layoutToEdit?.id) { mutableStateOf(layoutToEdit?.title ?: "") }
    var selectedIcon by remember(layoutToEdit?.id) {
        mutableStateOf(
            layoutToEdit?.iconName?.let { IconMapper.getIconVector(it) }
                ?: Icons.Filled.DashboardCustomize // Default icon
        )
    }
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

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Layout Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = !isTitleValid && title.isNotEmpty()
                )
                if (!isTitleValid && title.isNotEmpty()) {
                    Text(
                        "Title cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Select Icon:", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp), // Limit height of the grid
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
                                .padding(2.dp), // Inner padding after border
                            tint = if (selectedIcon == icon) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                            onConfirm(title.trim(), IconMapper.getIconName(selectedIcon), layoutToEdit?.id)
                        },
                        enabled = isTitleValid
                    ) {
                        Text(confirmButtonText)
                    }
                }
            }
        }
    }
}
