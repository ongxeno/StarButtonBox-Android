/*
 * File: StarButtonBox/app/src/main/java/com/ongxeno/android/starbuttonbox/ui/dialog/AddEditButtonDialog.kt
 * Added Delete button, dismissible properties, Color Picker dialog integration,
 * and visual feedback on the color picker button.
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens // Icon for color picker
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
// Import the custom color picker dialog
import com.ongxeno.android.starbuttonbox.data.FreeFormItemState
import com.ongxeno.android.starbuttonbox.data.FreeFormItemType
import com.ongxeno.android.starbuttonbox.data.Macro
import com.ongxeno.android.starbuttonbox.utils.ColorUtils
import com.ongxeno.android.starbuttonbox.utils.ColorUtils.toHexString
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditButtonDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        text: String,
        macroId: String,
        type: FreeFormItemType,
        textSizeSp: Float?,
        backgroundColorHex: String?
    ) -> Unit,
    initialItemState: FreeFormItemState?,
    availableMacros: List<Macro>,
    onDelete: (itemId: String) -> Unit
) {
    if (!showDialog) return

    val isEditMode = initialItemState != null

    val defaultTextSizeSp = 14f
    val defaultBackgroundColorHex = remember { ColorUtils.DefaultButtonBackground.toHexString() }
    // Combine default and standard colors for checking if a color is custom
    val standardAndDefaultColors = remember { (ColorUtils.StandardColors + defaultBackgroundColorHex).toSet() }

    var buttonText by remember(initialItemState?.id) { mutableStateOf(initialItemState?.text ?: "") }
    var selectedMacroId by remember(initialItemState?.id) {
        mutableStateOf(initialItemState?.macroId ?: availableMacros.firstOrNull()?.id ?: "")
    }
    var selectedType by remember(initialItemState?.id) {
        mutableStateOf(initialItemState?.type ?: FreeFormItemType.MOMENTARY_BUTTON)
    }
    var commandDropdownExpanded by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var isCommandSelected by remember(selectedMacroId) { mutableStateOf(selectedMacroId.isNotEmpty()) }

    var selectedTextSizeSp by remember(initialItemState?.id) {
        mutableStateOf(initialItemState?.textSizeSp ?: defaultTextSizeSp)
    }
    var selectedBackgroundColorHex by remember(initialItemState?.id) {
        mutableStateOf(initialItemState?.backgroundColorHex ?: defaultBackgroundColorHex)
    }

    // State to track if the current selection is from the custom picker
    var isCustomColorSelected by remember(initialItemState?.backgroundColorHex) {
        mutableStateOf(
            initialItemState?.backgroundColorHex != null &&
                    !standardAndDefaultColors.contains(initialItemState.backgroundColorHex)
        )
    }

    // State to control the visibility of the color picker dialog
    var showColorPickerDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val dropdownContentMaxHeight = remember { (screenHeight * 0.4f).coerceAtMost(250.dp) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(
                    text = if (isEditMode) "Edit Button" else "Add Button",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // --- Form Fields (Omitted for brevity) ---
                OutlinedTextField( /* Button Text */ value = buttonText, onValueChange = { buttonText = it }, label = { Text("Button Label") }, modifier = Modifier.fillMaxWidth(), singleLine = true )
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox( /* Command Selection */ expanded = commandDropdownExpanded, onExpandedChange = { commandDropdownExpanded = !commandDropdownExpanded }, modifier = Modifier.fillMaxWidth() ) {
                    OutlinedTextField( value = selectedMacroId.ifEmpty { "Select Command" }, onValueChange = {}, readOnly = true, label = { Text("Command") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = commandDropdownExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(), isError = !isCommandSelected )
                    ExposedDropdownMenu( expanded = commandDropdownExpanded, onDismissRequest = { commandDropdownExpanded = false }, modifier = Modifier.heightIn(max = dropdownContentMaxHeight + 32.dp) ) {
                        Column( modifier = Modifier .fillMaxWidth() .height(dropdownContentMaxHeight) .verticalScroll(rememberScrollState()) ) {
                            availableMacros.forEach { macro -> DropdownMenuItem( text = { Text(macro.title, style = MaterialTheme.typography.bodyMedium) }, onClick = { selectedMacroId = macro.id; isCommandSelected = true; commandDropdownExpanded = false }, contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding, ) }
                        }
                    }
                }
                if (!isCommandSelected) { Text("Please select a command", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp)) }
                Spacer(modifier = Modifier.height(16.dp))
                ExposedDropdownMenuBox( /* Button Type */ expanded = typeDropdownExpanded, onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }, modifier = Modifier.fillMaxWidth() ) {
                    OutlinedTextField( value = selectedType.name, onValueChange = {}, readOnly = true, label = { Text("Button Type") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth() )
                    ExposedDropdownMenu( expanded = typeDropdownExpanded, onDismissRequest = { typeDropdownExpanded = false }, modifier = Modifier.exposedDropdownSize(matchTextFieldWidth = true) ) {
                        DropdownMenuItem( text = { Text(FreeFormItemType.MOMENTARY_BUTTON.name) }, onClick = { selectedType = FreeFormItemType.MOMENTARY_BUTTON; typeDropdownExpanded = false }, modifier = Modifier.fillMaxWidth() )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text( /* Text Size */ text = "Text Size (${selectedTextSizeSp.toInt()} sp)", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 4.dp) )
                Slider( /* Text Size Slider */ value = selectedTextSizeSp, onValueChange = { selectedTextSizeSp = it }, valueRange = 10f..30f, steps = 19, onValueChangeFinished = { if (abs(selectedTextSizeSp - 14f) < 2f) selectedTextSizeSp = 14f } ) // Snap example
                Spacer(modifier = Modifier.height(16.dp))

                // --- Background Color Selection ---
                Text("Background Color", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Default Color Box
                    ColorSelectorBox(
                        color = remember { ColorUtils.parseHexColor(defaultBackgroundColorHex) ?: Color.Transparent },
                        hexColor = defaultBackgroundColorHex,
                        isSelected = selectedBackgroundColorHex == defaultBackgroundColorHex && !isCustomColorSelected, // Only selected if not custom
                        onClick = {
                            selectedBackgroundColorHex = defaultBackgroundColorHex
                            isCustomColorSelected = false // Selecting default/standard deselects custom
                        }
                    )
                    // Standard Colors
                    ColorUtils.StandardColors.forEach { hex ->
                        val color = remember(hex) { ColorUtils.parseHexColor(hex) ?: Color.Transparent }
                        ColorSelectorBox(
                            color = color,
                            hexColor = hex,
                            isSelected = selectedBackgroundColorHex == hex && !isCustomColorSelected, // Only selected if not custom
                            onClick = {
                                selectedBackgroundColorHex = hex
                                isCustomColorSelected = false // Selecting default/standard deselects custom
                            }
                        )
                    }
                    val customColor = remember(selectedBackgroundColorHex) {
                        ColorUtils.parseHexColor(selectedBackgroundColorHex) ?: Color.Transparent
                    }
                    // Use a Box similar to ColorSelectorBox
                    Box(
                        modifier = Modifier
                            .size(40.dp) // Match size
                            .clip(CircleShape)
                            // Show selected custom color as background if it's the active selection
                            .background(if (isCustomColorSelected) customColor else MaterialTheme.colorScheme.surfaceVariant) // Show color or default bg
                            .border(
                                BorderStroke(
                                    width = if (isCustomColorSelected) 3.dp else 1.dp,
                                    color = if (isCustomColorSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                                ),
                                CircleShape
                            )
                            .clickable { showColorPickerDialog = true }, // Open picker on click
                        contentAlignment = Alignment.Center
                    ) {
                        // Base ColorLens Icon
                        Icon(
                            Icons.Filled.ColorLens,
                            contentDescription = "Select Custom Color",
                            // Tint depends on whether custom color is selected
                            tint = if (isCustomColorSelected) ColorUtils.getContrastingTextColor(customColor) else MaterialTheme.colorScheme.onSurface
                        )
                        // Overlay Check mark if custom color is selected
                        if (isCustomColorSelected) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = "Custom Color Selected",
                                modifier = Modifier.size(40.dp * 0.6f), // Smaller check mark
                                // Ensure check mark contrasts with the selected background
                                tint = ColorUtils.getContrastingTextColor(customColor)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))


                // --- Action Buttons Row ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete Button
                    if (isEditMode) {
                        TextButton(
                            onClick = {
                                initialItemState?.id?.let { itemId -> onDelete(itemId) }
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Delete") }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    // Cancel Button
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Save/Add Button
                    Button(
                        onClick = {
                            if (selectedMacroId.isNotEmpty()) {
                                onSave(
                                    buttonText.ifBlank { availableMacros.firstOrNull { it.id == selectedMacroId }?.label ?: selectedMacroId },
                                    selectedMacroId,
                                    selectedType,
                                    selectedTextSizeSp.takeIf { it != defaultTextSizeSp },
                                    // Pass hex unless it's the default AND custom wasn't specifically selected
                                    selectedBackgroundColorHex.takeIf { it != defaultBackgroundColorHex || isCustomColorSelected }
                                )
                            } else { isCommandSelected = false }
                        },
                        enabled = selectedMacroId.isNotEmpty()
                    ) { Text(if (isEditMode) "Save" else "Add") }
                } // End Action Buttons Row
            } // End Main Column
        } // End Surface
    } // End Main Dialog

    // --- Custom Color Picker Dialog ---
    if (showColorPickerDialog) {
        CustomColorPickerDialog(
            initialColor = remember(selectedBackgroundColorHex) {
                ColorUtils.parseHexColor(selectedBackgroundColorHex) ?: Color.White
            },
            onDismissRequest = { showColorPickerDialog = false },
            onColorSelected = { selectedColor ->
                val newHex = selectedColor.toHexString()
                selectedBackgroundColorHex = newHex
                // Mark that the selection came from the custom picker
                isCustomColorSelected = true
                showColorPickerDialog = false
            }
        )
    }
}

// --- Helper Composable for Color Selection Box (Unchanged) ---
@Composable
private fun ColorSelectorBox(
    color: Color,
    hexColor: String,
    isSelected: Boolean,
    onClick: () -> Unit, // Changed param type
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    selectedBorderColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
            .border(
                BorderStroke(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) selectedBorderColor else Color.Gray.copy(alpha = 0.5f)
                ),
                CircleShape
            )
            .clickable { onClick() }, // Call lambda directly
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected Color",
                tint = ColorUtils.getContrastingTextColor(color),
                modifier = Modifier.size(size * 0.6f)
            )
        }
    }
}
