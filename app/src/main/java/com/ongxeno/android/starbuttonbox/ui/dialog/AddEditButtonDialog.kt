/*
 * File: StarButtonBox/app/src/main/java/com/ongxeno/android/starbuttonbox/ui/dialog/AddEditButtonDialog.kt
 * Updates the dialog to include controls for text size and background color selection.
 */
package com.ongxeno.android.starbuttonbox.ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check // Icon for selected color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp // Import sp for text size
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ongxeno.android.starbuttonbox.data.Command
import com.ongxeno.android.starbuttonbox.data.FreeFormItemState
import com.ongxeno.android.starbuttonbox.data.FreeFormItemType
// Import the new ColorUtils object
import com.ongxeno.android.starbuttonbox.utils.ColorUtils
import com.ongxeno.android.starbuttonbox.utils.ColorUtils.toHexString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditButtonDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    // Updated onSave lambda to include new parameters
    onSave: (
        text: String,
        commandString: String,
        type: FreeFormItemType,
        textSizeSp: Float?, // Nullable Float for text size
        backgroundColorHex: String? // Nullable String for background color hex
    ) -> Unit,
    initialItemState: FreeFormItemState? // The state being edited, or null if adding
) {
    if (!showDialog) return // Don't compose if the dialog shouldn't be shown

    val isEditMode = initialItemState != null
    val allCommandIdentifiers = remember { Command.getAllCommandStrings() } // Get all available command strings

    // --- Default values ---
    // Define default values used when creating a *new* button or if state is somehow invalid
    val defaultTextSizeSp = 14f // Default text size in sp
    val defaultBackgroundColorHex = remember { ColorUtils.DefaultButtonBackground.toHexString() } // Get hex from ColorUtils default

    // --- State Variables ---
    // Use the initial state if editing, otherwise use defaults or empty strings
    var buttonText by remember(initialItemState?.id) { mutableStateOf(initialItemState?.text ?: "") }
    var selectedCommandString by remember(initialItemState?.id) {
        mutableStateOf(initialItemState?.commandString ?: allCommandIdentifiers.firstOrNull() ?: "")
    }
    var selectedType by remember(initialItemState?.id) {
        mutableStateOf(initialItemState?.type ?: FreeFormItemType.MOMENTARY_BUTTON)
    }
    var commandDropdownExpanded by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    // Track if a valid command is selected for validation feedback
    var isCommandSelected by remember(selectedCommandString) { mutableStateOf(selectedCommandString.isNotEmpty()) }

    // --- New State Variables for Customization ---
    // Initialize with existing value if editing, otherwise use the defined defaults
    var selectedTextSizeSp by remember(initialItemState?.id) {
        mutableStateOf(initialItemState?.textSizeSp ?: defaultTextSizeSp)
    }
    var selectedBackgroundColorHex by remember(initialItemState?.id) {
        mutableStateOf(initialItemState?.backgroundColorHex ?: defaultBackgroundColorHex)
    }

    // --- Dropdown Height Calculation (Unchanged) ---
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val dropdownContentMaxHeight = remember { (screenHeight * 0.4f).coerceAtMost(250.dp) }

    // --- Dialog Composable ---
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false) // Standard dialog properties
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            // Use a Column with verticalScroll to handle potentially long content
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                // Dialog Title
                Text(
                    text = if (isEditMode) "Edit Button" else "Add Button",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Button Text Input (Unchanged)
                OutlinedTextField(
                    value = buttonText,
                    onValueChange = { buttonText = it },
                    label = { Text("Button Label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Command Selection Dropdown (Unchanged)
                ExposedDropdownMenuBox(
                    expanded = commandDropdownExpanded,
                    onExpandedChange = { commandDropdownExpanded = !commandDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCommandString.ifEmpty { "Select Command" },
                        onValueChange = {}, readOnly = true,
                        label = { Text("Command") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = commandDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        isError = !isCommandSelected // Show error state if no command selected
                    )
                    ExposedDropdownMenu(
                        expanded = commandDropdownExpanded,
                        onDismissRequest = { commandDropdownExpanded = false },
                        modifier = Modifier.heightIn(max = dropdownContentMaxHeight + 32.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(dropdownContentMaxHeight)
                                .verticalScroll(rememberScrollState())
                        ) {
                            allCommandIdentifiers.forEach { commandId ->
                                DropdownMenuItem(
                                    text = { Text(commandId, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        selectedCommandString = commandId
                                        isCommandSelected = true // Mark as selected
                                        commandDropdownExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                )
                            }
                        }
                    }
                }
                // Validation message for command selection
                if (!isCommandSelected) {
                    Text("Please select a command", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Button Type Selection Dropdown (Unchanged)
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
                        modifier = Modifier.exposedDropdownSize(matchTextFieldWidth = true)
                    ) {
                        // Add more types here if they are introduced in FreeFormItemType
                        DropdownMenuItem(
                            text = { Text(FreeFormItemType.MOMENTARY_BUTTON.name) },
                            onClick = { selectedType = FreeFormItemType.MOMENTARY_BUTTON; typeDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp)) // Add spacing

                // --- Text Size Selection Slider ---
                Text(
                    text = "Text Size (${selectedTextSizeSp.toInt()} sp)", // Display current value
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Slider(
                    value = selectedTextSizeSp, // Current state
                    onValueChange = { selectedTextSizeSp = it }, // Update state on change
                    valueRange = 10f..30f, // Define min and max text size (e.g., 10sp to 30sp)
                    steps = 19 // Number of steps between min and max (e.g., (30-10)/1 = 20 intervals -> 19 steps)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // --- Background Color Selection ---
                Text("Background Color", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                // Use a Row to display color options horizontally
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), // Add some padding
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), // Space out colors
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Box for resetting to the default color
                    ColorSelectorBox(
                        color = remember { ColorUtils.parseHexColor(defaultBackgroundColorHex) ?: Color.Transparent }, // Parse default hex
                        hexColor = defaultBackgroundColorHex, // Pass the default hex string
                        isSelected = selectedBackgroundColorHex == defaultBackgroundColorHex, // Check if it's selected
                        onClick = { selectedBackgroundColorHex = defaultBackgroundColorHex } // Set state to default hex
                    )

                    // Iterate through the standard colors defined in ColorUtils
                    ColorUtils.StandardColors.forEach { hex ->
                        // Parse the hex string to a Compose Color, remembering the result
                        val color = remember(hex) { ColorUtils.parseHexColor(hex) ?: Color.Transparent }
                        ColorSelectorBox(
                            color = color,
                            hexColor = hex, // Pass the current hex string
                            isSelected = selectedBackgroundColorHex == hex, // Check if it's selected
                            onClick = { selectedBackgroundColorHex = hex } // Set state to this hex string
                        )
                    }
                    // Placeholder for a future color picker button
                    // IconButton(onClick = { /* TODO: Open color picker */ }) { /* Icon */ }
                }
                Spacer(modifier = Modifier.height(24.dp))


                // --- Action Buttons (Cancel/Save) ---
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // Only save if a command is selected
                            if (selectedCommandString.isNotEmpty()) {
                                // Call the updated onSave lambda with all values
                                onSave(
                                    buttonText.ifBlank { selectedCommandString }, // Use command string as default text if blank
                                    selectedCommandString,
                                    selectedType,
                                    // Pass null for size/color if the selected value is the same as the default
                                    // This avoids saving unnecessary default values in the state
                                    selectedTextSizeSp.takeIf { it != defaultTextSizeSp },
                                    selectedBackgroundColorHex.takeIf { it != defaultBackgroundColorHex }
                                )
                            } else {
                                // Trigger validation feedback if command not selected
                                isCommandSelected = false
                                println("Save attempted without selecting a command.")
                            }
                        },
                        // Disable Save button if no command is selected
                        enabled = selectedCommandString.isNotEmpty()
                    ) {
                        Text(if (isEditMode) "Save" else "Add")
                    }
                }
            }
        }
    }
}

/**
 * A helper composable to display a single color selection circle.
 *
 * @param color The Compose Color to display.
 * @param hexColor The hex string representation of the color (used for onClick).
 * @param isSelected Whether this color is the currently selected one.
 * @param onClick Lambda function called with the hexColor string when the box is clicked.
 * @param modifier Modifier for the Box.
 * @param size The size of the color circle.
 * @param selectedBorderColor The color of the border when this item is selected.
 */
@Composable
private fun ColorSelectorBox(
    color: Color,
    hexColor: String,
    isSelected: Boolean,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp, // Size of the color circle
    selectedBorderColor: Color = MaterialTheme.colorScheme.primary // Border color when selected
) {
    Box(
        modifier = modifier
            .size(size) // Apply size
            .clip(CircleShape) // Make it circular
            .background(color) // Set the background to the passed color
            .border( // Add a border, thicker if selected
                BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp, // Thicker border if selected
                    color = if (isSelected) selectedBorderColor else Color.Gray.copy(alpha = 0.5f) // Use primary or gray
                ),
                CircleShape // Apply border to the circle shape
            )
            .clickable { onClick(hexColor) }, // Call onClick with the hex string
        contentAlignment = Alignment.Center // Center the checkmark icon
    ) {
        // Show a checkmark icon if this color is selected
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected Color", // Accessibility description
                // Use contrasting color for the checkmark to ensure visibility
                tint = ColorUtils.getContrastingTextColor(color),
                modifier = Modifier.size(size * 0.6f) // Make icon slightly smaller than the box
            )
        }
    }
}
