package com.ongxeno.android.starbuttonbox.ui.dialog

import androidx.compose.foundation.background // Import background
import androidx.compose.foundation.clickable // Import clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState // Import rememberScrollState
import androidx.compose.foundation.verticalScroll // Import verticalScroll
// Removed LazyColumn imports
// import androidx.compose.foundation.lazy.LazyColumn
// import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons // Import Icons
// Removed ArrowDropDown import, ExposedDropdownMenuDefaults provides one
// import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration // Import LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ongxeno.android.starbuttonbox.data.Command // Import Command object
import com.ongxeno.android.starbuttonbox.data.FreeFormItemState
import com.ongxeno.android.starbuttonbox.data.FreeFormItemType
import com.ongxeno.android.starbuttonbox.data.Command.getAllCommandStrings // Import the helper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditButtonDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onSave: (text: String, commandString: String, type: FreeFormItemType) -> Unit,
    initialItemState: FreeFormItemState?
) {
    if (!showDialog) return

    val isEditMode = initialItemState != null
    val allCommandIdentifiers = remember { Command.getAllCommandStrings() }

    var buttonText by remember(initialItemState?.id) { mutableStateOf(initialItemState?.text ?: "") }
    var selectedCommandString by remember(initialItemState?.id) {
        mutableStateOf(initialItemState?.commandString ?: allCommandIdentifiers.firstOrNull() ?: "")
    }
    var selectedType by remember(initialItemState?.id) {
        mutableStateOf(initialItemState?.type ?: FreeFormItemType.MOMENTARY_BUTTON)
    }
    var commandDropdownExpanded by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var isCommandSelected by remember(selectedCommandString) { mutableStateOf(selectedCommandString.isNotEmpty()) }

    // Calculate a reasonable fixed height based on screen height for the dropdown content
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val dropdownContentMaxHeight = remember { (screenHeight * 0.4f).coerceAtMost(250.dp) } // 40% of screen or 250dp max

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            // Removed Modifier.width(IntrinsicSize.Min)
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isEditMode) "Edit Button" else "Add Button",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Button Text
                OutlinedTextField(
                    value = buttonText,
                    onValueChange = { buttonText = it },
                    label = { Text("Button Label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                // --- Command Selection ---
                ExposedDropdownMenuBox(
                    expanded = commandDropdownExpanded,
                    onExpandedChange = { commandDropdownExpanded = !commandDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCommandString.ifEmpty { "Select Command" },
                        onValueChange = {}, // Read-only
                        readOnly = true, // Make TextField read-only
                        label = { Text("Command") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = commandDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor() // Important for anchoring the dropdown
                            .fillMaxWidth(),
                        isError = !isCommandSelected
                    )
                    ExposedDropdownMenu(
                        expanded = commandDropdownExpanded,
                        onDismissRequest = { commandDropdownExpanded = false },
                        // Let the menu size itself, constrain the inner Column
                        modifier = Modifier.heightIn(max = dropdownContentMaxHeight + 32.dp) // Allow padding
                    ) {
                        // Use a regular Column with verticalScroll AND fixed height
                        Column(
                            modifier = Modifier
                                .fillMaxWidth() // Fill available width from menu
                                .height(dropdownContentMaxHeight) // Apply fixed height HERE
                                .verticalScroll(rememberScrollState()) // Make it scrollable
                        ) {
                            allCommandIdentifiers.forEach { commandId ->
                                DropdownMenuItem(
                                    text = { Text(commandId, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        selectedCommandString = commandId
                                        isCommandSelected = true
                                        commandDropdownExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }
                } // End Command Selection Box

                if (!isCommandSelected) {
                    Text("Please select a command", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))

                // --- Button Type Selection (Keep ExposedDropdownMenuBox) ---
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedType.name, onValueChange = {}, readOnly = true,
                        label = { Text("Button Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false },
                        modifier = Modifier.exposedDropdownSize(matchTextFieldWidth = true) // Keep for type dropdown
                    ) {
                        DropdownMenuItem(
                            text = { Text(FreeFormItemType.MOMENTARY_BUTTON.name) },
                            onClick = { selectedType = FreeFormItemType.MOMENTARY_BUTTON; typeDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // --- Action Buttons ---
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (selectedCommandString.isNotEmpty()) {
                                onSave(buttonText.ifBlank { selectedCommandString }, selectedCommandString, selectedType)
                            } else {
                                isCommandSelected = false
                                println("Save attempted without selecting a command.")
                            }
                        },
                        enabled = selectedCommandString.isNotEmpty()
                    ) {
                        Text(if (isEditMode) "Save" else "Add")
                    }
                }
            }
        }
    }
}
