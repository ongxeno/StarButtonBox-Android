/*
 * File: StarButtonBox/app/src/main/java/com/ongxeno/android/starbuttonbox/ui/layout/FreeFormLayout.kt
 * Optimized drag performance using graphicsLayer for position.
 * Resize uses state update for visual feedback.
 * Matched resize handle style to edit button style.
 * Using default Material 3 ripple indication.
 */
package com.ongxeno.android.starbuttonbox.ui.layout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInFull // Icon for resize handle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer // Import graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.ongxeno.android.starbuttonbox.data.Command
import com.ongxeno.android.starbuttonbox.data.FreeFormItemState
import com.ongxeno.android.starbuttonbox.data.FreeFormItemType
import com.ongxeno.android.starbuttonbox.ui.button.MomentaryButton
import com.ongxeno.android.starbuttonbox.ui.dialog.AddEditButtonDialog
import com.ongxeno.android.starbuttonbox.utils.ColorUtils
// Import the extension function
import com.ongxeno.android.starbuttonbox.utils.ColorUtils.adjustLuminance
import com.ongxeno.android.starbuttonbox.utils.LocalLayoutDatasource
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

// --- Grid Configuration ---
private const val GRID_COLUMNS = 60
private const val GRID_ROWS = 35
private val gridLineColor = Color.Gray.copy(alpha = 0.2f)
private val gridStrokeWidth = 1.dp
private val gridPathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)

// --- Button Size Constraints (Now in Grid Units) ---
private const val minGridWidth = 2
private const val minGridHeight = 1
private const val maxGridWidth = GRID_COLUMNS
private const val maxGridHeight = GRID_ROWS

// --- Edit/Resize Handle Appearance ---
private val handleVisualSize = 24.dp
private val handleIconSizeMultiplier = 0.5f
private val minTouchTargetSize = 48.dp

// --- Default Layouts Definition (Using Grid Units) ---
private fun getDefaultItemsForLayout(layoutId: String): List<FreeFormItemState> {
    return when (layoutId) {
        "freeform_1" -> listOf(
            FreeFormItemState(text = "FLT RDY", commandString = Command.GeneralCockpit_FlightReady, gridCol = 1, gridRow = 1, gridWidth = 8, gridHeight = 4),
            FreeFormItemState(text = "GEAR", commandString = Command.LandingAndDocking_ToggleLandingGear, gridCol = 10, gridRow = 1, gridWidth = 8, gridHeight = 4),
            FreeFormItemState(text = "ENGINES", commandString = Command.PowerManagement_TogglePowerEngines, gridCol = 1, gridRow = 6, gridWidth = 8, gridHeight = 4),
            FreeFormItemState(text = "QT MODE", commandString = Command.QuantumTravel_ToggleQuantumMode, gridCol = 10, gridRow = 6, gridWidth = 10, gridHeight = 4)
        )
        "freeform_2" -> listOf(
            FreeFormItemState(text = "SCAN", commandString = Command.Scanning_ToggleScanningMode, gridCol = 45, gridRow = 30, gridWidth = 12, gridHeight = 4),
            FreeFormItemState(text = "PING", commandString = Command.Scanning_ActivatePing, gridCol = 45, gridRow = 25, gridWidth = 12, gridHeight = 4),
            FreeFormItemState(text = "LOCK TGT", commandString = Command.Targeting_LockSelectedTarget, gridCol = 1, gridRow = 30, gridWidth = 15, gridHeight = 4),
            FreeFormItemState(text = "UNLOCK", commandString = Command.Targeting_UnlockLockedTarget, gridCol = 18, gridRow = 30, gridWidth = 12, gridHeight = 4)
        )
        else -> emptyList()
    }
}


@Composable
fun FreeFormLayout(
    layoutId: String,
    onCommand: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val layoutDatasource = LocalLayoutDatasource.current
    var isLocked by rememberSaveable(layoutId) { mutableStateOf(true) }
    var isLoading by remember(layoutId) { mutableStateOf(true) }
    val mutableItems = remember { mutableStateListOf<FreeFormItemState>() }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingItemState by remember { mutableStateOf<FreeFormItemState?>(null) }

    // State for temporary resize visual feedback
    var resizeDelta by remember { mutableStateOf(Offset.Zero) }
    // ID of the item currently being interacted with (drag or resize)
    var interactingItemId by remember { mutableStateOf<String?>(null) }
    // State to hold the *cumulative* drag offset during a gesture for the specific interacting item
    var currentDragOffset by remember { mutableStateOf(Offset.Zero) }


    // --- Load Layout Data ---
    LaunchedEffect(layoutId, layoutDatasource) {
        isLoading = true
        layoutDatasource.getLayoutFlow(layoutId).collect { loadedItems ->
            val itemsToUse = if (loadedItems.isEmpty()) {
                getDefaultItemsForLayout(layoutId)
            } else {
                // Validate loaded grid values
                loadedItems.map {
                    it.copy(
                        gridWidth = it.gridWidth.coerceAtLeast(1),
                        gridHeight = it.gridHeight.coerceAtLeast(1),
                        gridCol = it.gridCol.coerceAtLeast(0),
                        gridRow = it.gridRow.coerceAtLeast(0)
                    )
                }
            }
            mutableItems.clear()
            mutableItems.addAll(itemsToUse)
            isLoading = false
        }
    }

    // --- Save Layout Data ---
    val scope = rememberCoroutineScope()
    val saveCurrentLayout: (List<FreeFormItemState>) -> Unit = { itemsToSave ->
        scope.launch {
            layoutDatasource.saveLayout(layoutId, itemsToSave.toList())
        }
    }

    // --- Main Layout UI ---
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val constraints = this.constraints
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()
        // Calculate cell dimensions
        val cellWidthPx = if (GRID_COLUMNS > 0 && maxWidthPx > 0) maxWidthPx / GRID_COLUMNS else maxWidthPx
        val cellHeightPx = if (GRID_ROWS > 0 && maxHeightPx > 0) maxHeightPx / GRID_ROWS else maxHeightPx
        val gridStrokeWidthPx = with(density) { gridStrokeWidth.toPx() }

        // Check if grid is valid
        val isGridValid = cellWidthPx > 0 && cellHeightPx > 0

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                // --- Draw Grid Lines ---
                if (!isLocked && isGridValid) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        for (i in 1 until GRID_COLUMNS) {
                            val x = i * cellWidthPx
                            drawLine(gridLineColor, Offset(x, 0f), Offset(x, size.height), gridStrokeWidthPx, pathEffect = gridPathEffect)
                        }
                        for (i in 1 until GRID_ROWS) {
                            val y = i * cellHeightPx
                            drawLine(gridLineColor, Offset(0f, y), Offset(size.width, y), gridStrokeWidthPx, pathEffect = gridPathEffect)
                        }
                    }
                }

                // --- Render Layout Items ---
                mutableItems.forEachIndexed { index, itemState ->
                    val commandIdentifier = itemState.commandString

                    // Calculate BASE Absolute Position and Size
                    val itemBaseAbsoluteX = itemState.gridCol * cellWidthPx
                    val itemBaseAbsoluteY = itemState.gridRow * cellHeightPx
                    val itemBaseAbsoluteWidth = itemState.gridWidth * cellWidthPx
                    val itemBaseAbsoluteHeight = itemState.gridHeight * cellHeightPx

                    // Apply temporary resize delta for visual feedback
                    val currentResizeDeltaW = if (itemState.id == interactingItemId) resizeDelta.x else 0f
                    val currentResizeDeltaH = if (itemState.id == interactingItemId) resizeDelta.y else 0f

                    val finalRenderWidthPx = itemBaseAbsoluteWidth + currentResizeDeltaW
                    val finalRenderHeightPx = itemBaseAbsoluteHeight + currentResizeDeltaH

                    val itemWidthDp = with(density) { finalRenderWidthPx.toDp() }
                    val itemHeightDp = with(density) { finalRenderHeightPx.toDp() }


                    // Resolve Styling
                    val defaultM3ButtonColors = ButtonDefaults.buttonColors()
                    val buttonColors = remember(itemState.backgroundColorHex) {
                        val enabledContainerColor = ColorUtils.parseHexColor(itemState.backgroundColorHex)
                            ?: defaultM3ButtonColors.containerColor
                        val enabledContentColor = ColorUtils.getContrastingTextColor(enabledContainerColor)
                        val disabledContainer = enabledContainerColor
                        val disabledContent = enabledContentColor
                        ButtonColors(
                            containerColor = enabledContainerColor,
                            contentColor = enabledContentColor,
                            disabledContainerColor = disabledContainer,
                            disabledContentColor = disabledContent
                        )
                    }
                    val textSize = remember(itemState.textSizeSp) {
                        itemState.textSizeSp?.sp ?: TextUnit.Unspecified
                    }

                    // --- Item Container Box ---
                    // Positioned at the BASE absolute position
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(itemBaseAbsoluteX.roundToInt(), itemBaseAbsoluteY.roundToInt()) }
                            .size(itemWidthDp, itemHeightDp)
                            // Apply visual drag translation using graphicsLayer
                            .graphicsLayer {
                                if (itemState.id == interactingItemId) {
                                    translationX = currentDragOffset.x
                                    translationY = currentDragOffset.y
                                } else {
                                    translationX = 0f
                                    translationY = 0f
                                }
                            }
                            // Drag gesture detection moved here
                            .pointerInput(isLocked, isGridValid, itemState.id) {
                                if (!isLocked && isGridValid) {
                                    detectDragGestures(
                                        onDragStart = {
                                            interactingItemId = itemState.id
                                            currentDragOffset = Offset.Zero
                                        },
                                        onDragEnd = { // Snap Position Logic
                                            val currentState = mutableItems[index]
                                            val finalDraggedX = (currentState.gridCol * cellWidthPx) + currentDragOffset.x
                                            val finalDraggedY = (currentState.gridRow * cellHeightPx) + currentDragOffset.y
                                            val targetCol = (finalDraggedX / cellWidthPx).roundToInt()
                                            val targetRow = (finalDraggedY / cellHeightPx).roundToInt()
                                            val clampedCol = targetCol.coerceIn(0, GRID_COLUMNS - currentState.gridWidth)
                                            val clampedRow = targetRow.coerceIn(0, GRID_ROWS - currentState.gridHeight)
                                            if (currentState.gridCol != clampedCol || currentState.gridRow != clampedRow) {
                                                mutableItems[index] = currentState.copy(
                                                    gridCol = clampedCol,
                                                    gridRow = clampedRow
                                                )
                                                saveCurrentLayout(mutableItems.toList())
                                            }
                                            currentDragOffset = Offset.Zero
                                            interactingItemId = null
                                        },
                                        onDragCancel = {
                                            currentDragOffset = Offset.Zero
                                            interactingItemId = null
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            currentDragOffset += dragAmount
                                        }
                                    )
                                }
                            }
                    ) {
                        // --- Item Content Box ---
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // --- Render the actual component ---
                            when (itemState.type) {
                                FreeFormItemType.MOMENTARY_BUTTON -> {
                                    MomentaryButton(
                                        modifier = Modifier.fillMaxSize(),
                                        text = itemState.text,
                                        enabled = isLocked,
                                        onPress = { if (isLocked) onCommand(commandIdentifier) },
                                        colors = buttonColors,
                                        textSize = textSize,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }

                            // --- Resize Handle ---
                            if (!isLocked) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = (minTouchTargetSize / 2), y = (minTouchTargetSize / 2))
                                        .size(minTouchTargetSize)
                                        .clip(CircleShape)
                                        .clickable(
                                            onClick = {},
                                            enabled = !isLocked
                                        )
                                        .pointerInput(Unit, isGridValid, itemState.id) { // Resize Gesture
                                            if (isGridValid) {
                                                detectDragGestures(
                                                    onDragStart = {
                                                        interactingItemId = itemState.id
                                                        resizeDelta = Offset.Zero
                                                    },
                                                    onDragEnd = { // Snap Size Logic
                                                        val currentState = mutableItems[index]
                                                        val finalDraggedWidthPx = (currentState.gridWidth * cellWidthPx) + resizeDelta.x
                                                        val finalDraggedHeightPx = (currentState.gridHeight * cellHeightPx) + resizeDelta.y
                                                        val targetWidthCells = max(minGridWidth, (finalDraggedWidthPx / cellWidthPx).roundToInt())
                                                        val targetHeightCells = max(minGridHeight, (finalDraggedHeightPx / cellHeightPx).roundToInt())
                                                        val clampedWidthCells = targetWidthCells.coerceIn(minGridWidth, GRID_COLUMNS - currentState.gridCol)
                                                        val clampedHeightCells = targetHeightCells.coerceIn(minGridHeight, GRID_ROWS - currentState.gridRow)
                                                        if (currentState.gridWidth != clampedWidthCells || currentState.gridHeight != clampedHeightCells) {
                                                            mutableItems[index] = currentState.copy(
                                                                gridWidth = clampedWidthCells,
                                                                gridHeight = clampedHeightCells
                                                            )
                                                            saveCurrentLayout(mutableItems.toList())
                                                        }
                                                        resizeDelta = Offset.Zero
                                                        interactingItemId = null
                                                    },
                                                    onDragCancel = {
                                                        resizeDelta = Offset.Zero
                                                        interactingItemId = null
                                                    },
                                                    onDrag = { change, dragAmount ->
                                                        change.consume()
                                                        resizeDelta += dragAmount
                                                    }
                                                )
                                            }
                                        }
                                ) {
                                    // Inner Box provides the visual appearance
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .size(handleVisualSize)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f))
                                            .border(1.dp, MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=0.5f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.OpenInFull,
                                            contentDescription = "Resize Button",
                                            modifier = Modifier.size(handleVisualSize * handleIconSizeMultiplier),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        } // End Item Content Box

                        // --- Edit Button ---
                        if (!isLocked) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = (-minTouchTargetSize / 2), y = (-minTouchTargetSize / 2))
                                    .size(minTouchTargetSize)
                                    .clip(CircleShape)
                                    .clickable(
                                        onClick = {
                                            editingItemState = itemState
                                            showAddEditDialog = true
                                        }
                                    )
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(handleVisualSize)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f))
                                        .border(1.dp, MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=0.5f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Edit Button",
                                        modifier = Modifier.size(handleVisualSize * handleIconSizeMultiplier),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        } // End Edit Button
                    } // End Item Container Box
                } // End forEachIndexed

            } // End else (isLoading check)

            // --- Top Right Control Buttons ---
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLocked) {
                    IconButton(
                        onClick = {
                            editingItemState = null
                            showAddEditDialog = true
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f), MaterialTheme.shapes.medium)
                    ) {
                        Icon(Icons.Filled.Add, "Add New Button", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                IconButton(
                    onClick = { isLocked = !isLocked },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.shapes.medium),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = if (isLocked) "Layout Locked (Tap to Edit)" else "Layout Unlocked (Tap to Lock)"
                    )
                }
            } // End Top Right Controls Row

        } // End Main Content Box (inside BoxWithConstraints)
    } // End BoxWithConstraints

    // --- Add/Edit Dialog Instance ---
    AddEditButtonDialog(
        showDialog = showAddEditDialog,
        onDismiss = { showAddEditDialog = false },
        initialItemState = editingItemState,
        // Saving only updates non-positional/size properties
        onSave = { text, commandString, type, textSizeSp, backgroundColorHex ->
            val currentEditId = editingItemState?.id
            if (currentEditId != null) {
                // Edit Mode
                val indexToUpdate = mutableItems.indexOfFirst { it.id == currentEditId }
                if (indexToUpdate != -1) {
                    mutableItems[indexToUpdate] = mutableItems[indexToUpdate].copy(
                        text = text,
                        commandString = commandString,
                        type = type,
                        textSizeSp = textSizeSp,
                        backgroundColorHex = backgroundColorHex
                    )
                    saveCurrentLayout(mutableItems.toList())
                } else { /* Error */ }
            } else {
                // Add Mode - Create new item with default grid values
                val newItem = FreeFormItemState(
                    text = text,
                    commandString = commandString,
                    type = type,
                    textSizeSp = textSizeSp,
                    backgroundColorHex = backgroundColorHex
                    // Default gridCol/Row/Width/Height are set in data class constructor
                )
                mutableItems.add(newItem)
                saveCurrentLayout(mutableItems.toList())
            }
            showAddEditDialog = false
        },
        onDelete = { itemId ->
            val indexToRemove = mutableItems.indexOfFirst { it.id == itemId }
            if (indexToRemove != -1) {
                mutableItems.removeAt(indexToRemove)
                saveCurrentLayout(mutableItems.toList())
            }
            showAddEditDialog = false // Dismiss dialog after delete
        }
    )
}

// Example Usage
@Composable
fun ExampleFreeFormLayoutUsage(
    layoutId: String,
    onCommand: (String) -> Unit
) {
    FreeFormLayout(
        layoutId = layoutId,
        onCommand = onCommand,
        modifier = Modifier.background(Color.Black.copy(alpha = 0.9f))
    )
}
