package com.ongxeno.android.starbuttonbox.ui.dialog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

    // Filter available macros to only include those with an effective input action
    val actionableMacros = remember(availableMacros) {
        availableMacros.filter { it.effectiveInputAction != null }
    }

    val defaultTextSizeSp = 14f
    val defaultBackgroundColorHex = remember { ColorUtils.DefaultButtonBackground.toHexString() }
    val standardAndDefaultColors = remember { (ColorUtils.StandardColors + defaultBackgroundColorHex).toSet() }

    var buttonText by remember(initialItemState?.id) { mutableStateOf(initialItemState?.text ?: "") }

    // Adjust initial selectedMacroId if the original one is no longer in actionableMacros
    var selectedMacroId by remember(initialItemState?.id, actionableMacros) {
        val initialMacro = initialItemState?.macroId
        if (initialMacro != null && actionableMacros.any { it.id == initialMacro }) {
            mutableStateOf(initialMacro)
        } else {
            mutableStateOf(actionableMacros.firstOrNull()?.id ?: "")
        }
    }

    var selectedType by remember(initialItemState?.id) {
        mutableStateOf(initialItemState?.type ?: FreeFormItemType.MOMENTARY_BUTTON)
    }
    var commandDropdownExpanded by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    // isCommandSelected should be true if selectedMacroId is not empty AND it exists in actionableMacros
    var isCommandSelected by remember(selectedMacroId, actionableMacros) {
        mutableStateOf(selectedMacroId.isNotEmpty() && actionableMacros.any { it.id == selectedMacroId })
    }


    var selectedTextSizeSp by remember(initialItemState?.id) {
        mutableStateOf(initialItemState?.textSizeSp ?: defaultTextSizeSp)
    }
    var selectedBackgroundColorHex by remember(initialItemState?.id) {
        mutableStateOf(initialItemState?.backgroundColorHex ?: defaultBackgroundColorHex)
    }
    var isCustomColorSelected by remember(initialItemState?.backgroundColorHex) {
        mutableStateOf(
            initialItemState?.backgroundColorHex != null &&
                    !standardAndDefaultColors.contains(initialItemState.backgroundColorHex)
        )
    }
    var showColorPickerDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val dropdownContentMaxHeight = remember { (screenHeight * 0.4f).coerceAtMost(250.dp) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
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

                OutlinedTextField(
                    value = buttonText,
                    onValueChange = { buttonText = it },
                    label = { Text("Button Label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = commandDropdownExpanded,
                    onExpandedChange = { commandDropdownExpanded = !commandDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = actionableMacros.find { it.id == selectedMacroId }?.title ?: selectedMacroId.ifEmpty { "Select Command" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Command") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = commandDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        isError = !isCommandSelected && selectedMacroId.isNotEmpty() // Show error if selection is attempted but invalid
                    )
                    ExposedDropdownMenu(
                        expanded = commandDropdownExpanded,
                        onDismissRequest = { commandDropdownExpanded = false },
                        modifier = Modifier.heightIn(max = dropdownContentMaxHeight + 32.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().height(dropdownContentMaxHeight).verticalScroll(rememberScrollState())) {
                            if (actionableMacros.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No actionable macros available", style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)) },
                                    onClick = { commandDropdownExpanded = false },
                                    enabled = false
                                )
                            } else {
                                actionableMacros.forEach { macro ->
                                    DropdownMenuItem(
                                        text = { Text(macro.title, style = MaterialTheme.typography.bodyMedium) },
                                        onClick = {
                                            selectedMacroId = macro.id
                                            isCommandSelected = true // A valid command is now selected
                                            commandDropdownExpanded = false
                                        },
                                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                    )
                                }
                            }
                        }
                    }
                }
                if (!isCommandSelected && selectedMacroId.isNotEmpty() && actionableMacros.none{it.id == selectedMacroId}) {
                     Text("Previously selected command is no longer valid or has no action. Please select a new one.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                } else if (!isCommandSelected && actionableMacros.isNotEmpty()) {
                     Text("Please select a command.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                }


                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedType.name.replace("_", " ").lowercase().replaceFirstChar { it.titlecase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Button Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false },
                        modifier = Modifier.exposedDropdownSize(matchTextFieldWidth = true)
                    ) {
                        FreeFormItemType.entries.forEach { type ->
                             DropdownMenuItem(
                                text = { Text(type.name.replace("_", " ").lowercase().replaceFirstChar { it.titlecase() }) },
                                onClick = { selectedType = type; typeDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Text Size (${selectedTextSizeSp.toInt()} sp)",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Slider(
                    value = selectedTextSizeSp,
                    onValueChange = { selectedTextSizeSp = it },
                    valueRange = 10f..30f,
                    steps = 19,
                    onValueChangeFinished = { if (abs(selectedTextSizeSp - 14f) < 2f) selectedTextSizeSp = 14f }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text("Background Color", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColorSelectorBox(
                        color = remember { ColorUtils.parseHexColor(defaultBackgroundColorHex) ?: Color.Transparent },
                        hexColor = defaultBackgroundColorHex,
                        isSelected = selectedBackgroundColorHex == defaultBackgroundColorHex && !isCustomColorSelected,
                        onClick = { selectedBackgroundColorHex = defaultBackgroundColorHex; isCustomColorSelected = false }
                    )
                    ColorUtils.StandardColors.forEach { hex ->
                        val color = remember(hex) { ColorUtils.parseHexColor(hex) ?: Color.Transparent }
                        ColorSelectorBox(
                            color = color,
                            hexColor = hex,
                            isSelected = selectedBackgroundColorHex == hex && !isCustomColorSelected,
                            onClick = { selectedBackgroundColorHex = hex; isCustomColorSelected = false }
                        )
                    }
                    val customColor = remember(selectedBackgroundColorHex) { ColorUtils.parseHexColor(selectedBackgroundColorHex) ?: Color.Transparent }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isCustomColorSelected) customColor else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                BorderStroke(
                                    width = if (isCustomColorSelected) 3.dp else 1.dp,
                                    color = if (isCustomColorSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.5f)
                                ),
                                CircleShape
                            )
                            .clickable { showColorPickerDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.ColorLens, "Select Custom Color",
                            tint = if (isCustomColorSelected) ColorUtils.getContrastingTextColor(customColor) else MaterialTheme.colorScheme.onSurface
                        )
                        if (isCustomColorSelected) {
                            Icon(
                                Icons.Filled.Check, "Custom Color Selected",
                                modifier = Modifier.size(40.dp * 0.6f),
                                tint = ColorUtils.getContrastingTextColor(customColor)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (isEditMode) {
                        TextButton(
                            onClick = { initialItemState?.id?.let { itemId -> onDelete(itemId) } },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Delete") }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (isCommandSelected) { // Check if a valid command is selected
                                onSave(
                                    buttonText.ifBlank { actionableMacros.find { it.id == selectedMacroId }?.label ?: selectedMacroId },
                                    selectedMacroId,
                                    selectedType,
                                    selectedTextSizeSp.takeIf { it != defaultTextSizeSp },
                                    selectedBackgroundColorHex.takeIf { it != defaultBackgroundColorHex || isCustomColorSelected }
                                )
                            } // If not, the error message for command selection should guide the user
                        },
                        enabled = isCommandSelected || selectedMacroId.isEmpty() // Enable save if a valid command is selected OR if no command is intended (empty macroId)
                    ) { Text(if (isEditMode) "Save" else "Add") }
                }
            }
        }
    }

    if (showColorPickerDialog) {
        CustomColorPickerDialog(
            initialColor = remember(selectedBackgroundColorHex) { ColorUtils.parseHexColor(selectedBackgroundColorHex) ?: Color.White },
            onDismissRequest = { showColorPickerDialog = false },
            onColorSelected = { selectedColor ->
                selectedBackgroundColorHex = selectedColor.toHexString()
                isCustomColorSelected = true
                showColorPickerDialog = false
            }
        )
    }
}

@Composable
private fun ColorSelectorBox(
    color: Color,
    hexColor: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
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
            .clickable { onClick() },
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
