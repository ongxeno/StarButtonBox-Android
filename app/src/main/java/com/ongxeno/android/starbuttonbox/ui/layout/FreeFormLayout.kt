/*
 * File: StarButtonBox/app/src/main/java/com/ongxeno/android/starbuttonbox/ui/layout/FreeFormLayout.kt
 * Changed save logic: Layout is now saved only when transitioning from unlocked to locked state.
 * Drag/Resize operations update local state for visual feedback but don't save immediately.
 */
package com.ongxeno.android.starbuttonbox.ui.layout

import android.content.Context
import android.util.Log
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.material.ripple.rememberRipple
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.MainViewModel
import com.ongxeno.android.starbuttonbox.data.Command
import com.ongxeno.android.starbuttonbox.data.FreeFormItemState
import com.ongxeno.android.starbuttonbox.data.FreeFormItemType
import com.ongxeno.android.starbuttonbox.ui.button.MomentaryButton
import com.ongxeno.android.starbuttonbox.ui.dialog.AddEditButtonDialog
import com.ongxeno.android.starbuttonbox.utils.ColorUtils
import com.ongxeno.android.starbuttonbox.utils.ColorUtils.adjustLuminance
import kotlin.math.max
import kotlin.math.roundToInt

// --- Grid Configuration ---
private const val GRID_COLUMNS = 60
private const val GRID_ROWS = 35
private val gridLineColor = Color.Gray.copy(alpha = 0.2f)
private val gridStrokeWidth = 1.dp
private val gridPathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)

// --- Button Size Constraints ---
private const val minGridWidth = 2
private const val minGridHeight = 1
private const val maxGridWidth = GRID_COLUMNS
private const val maxGridHeight = GRID_ROWS

// --- Edit/Resize Handle Appearance ---
private val handleVisualSize = 24.dp
private val handleIconSizeMultiplier = 0.5f
private const val minTouchTargetSizeDp = 48

// --- Logging Tag ---
private const val TAG_DRAG = "FreeFormDrag"

@Composable
fun FreeFormLayout(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    // Collect the authoritative state from the ViewModel
    val itemsStateFromViewModel by viewModel.currentFreeFormItemsState.collectAsStateWithLifecycle()

    // Local mutable state for editing, initialized from ViewModel state
    // This state is modified during drag/resize/add/delete while unlocked
    val editableItemsState = remember { mutableStateListOf<FreeFormItemState>() }

    // Flag to track if local state differs from ViewModel state
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    // Effect to synchronize local editable state when ViewModel state changes
    // OR when the layout becomes locked (to discard local edits if not saved)
    LaunchedEffect(itemsStateFromViewModel) {
        Log.d(TAG_DRAG,"Syncing local state from ViewModel. Has Unsaved: $hasUnsavedChanges")
        // Only overwrite local state if there are no unsaved changes OR if becoming locked
        // (This prevents overwriting user edits while unlocked)
        // Correction: Always sync when ViewModel changes to reflect external updates.
        // We will handle saving/discarding based on the lock button click.
        if (editableItemsState != itemsStateFromViewModel) {
            Log.d(TAG_DRAG,"ViewModel state differs. Updating local state.")
            editableItemsState.clear()
            editableItemsState.addAll(itemsStateFromViewModel)
            hasUnsavedChanges = false // Reset flag after syncing
        }
    }


    var isLocked by rememberSaveable { mutableStateOf(true) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingItemState by remember { mutableStateOf<FreeFormItemState?>(null) }

    // State for temporary VISUAL offset during drag (applied via graphicsLayer)
    var currentDragVisualOffset by remember { mutableStateOf(Offset.Zero) }
    // State for temporary VISUAL resize delta (applied via size modifier)
    var currentResizeVisualDelta by remember { mutableStateOf(Offset.Zero) }
    // ID of the item currently being interacted with (drag OR resize)
    var interactingItemId by remember { mutableStateOf<String?>(null) }
    // Removed awaitingStateUpdateAfterInteraction flag

    val context: Context = LocalContext.current.applicationContext
    val density = LocalDensity.current
    val minTouchTargetSizePx = with(density) { minTouchTargetSizeDp.dp.toPx() }

    // Removed LaunchedEffect(itemsState) for resetting offsets

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val constraints = this.constraints
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()
        val cellWidthPx = if (GRID_COLUMNS > 0 && maxWidthPx > 0) maxWidthPx / GRID_COLUMNS else maxWidthPx
        val cellHeightPx = if (GRID_ROWS > 0 && maxHeightPx > 0) maxHeightPx / GRID_ROWS else maxHeightPx
        val gridStrokeWidthPx = with(density) { gridStrokeWidth.toPx() }
        val isGridValid = cellWidthPx > 0 && cellHeightPx > 0

        Box(modifier = Modifier.fillMaxSize()) {

            // --- Draw Grid Lines ---
            if (!isLocked && isGridValid) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // ... grid drawing ...
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
            // *** Render based on the LOCAL editableItemsState ***
            editableItemsState.forEachIndexed { index, itemState -> // Use index for modification
                val commandIdentifier = itemState.commandString
                val itemId = itemState.id

                // Calculate BASE Absolute Position in Pixels from LOCAL state
                val itemBaseAbsoluteX = itemState.gridCol * cellWidthPx
                val itemBaseAbsoluteY = itemState.gridRow * cellHeightPx
                val itemBaseAbsoluteWidth = itemState.gridWidth * cellWidthPx
                val itemBaseAbsoluteHeight = itemState.gridHeight * cellHeightPx

                // Apply temporary resize delta for visual feedback
                val currentResizeDeltaW = if (itemId == interactingItemId && currentResizeVisualDelta != Offset.Zero) currentResizeVisualDelta.x else 0f
                val currentResizeDeltaH = if (itemId == interactingItemId && currentResizeVisualDelta != Offset.Zero) currentResizeVisualDelta.y else 0f

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
                    ButtonColors(
                        containerColor = enabledContainerColor,
                        contentColor = enabledContentColor,
                        disabledContainerColor = enabledContainerColor,
                        disabledContentColor = enabledContentColor
                    )
                }
                val textSize = remember(itemState.textSizeSp) {
                    itemState.textSizeSp?.sp ?: TextUnit.Unspecified
                }


                // --- Item Container Box ---
                Box(
                    modifier = Modifier
                        // *** Removed Modifier.offset ***
                        .size(itemWidthDp, itemHeightDp) // Size includes visual resize delta
                        .graphicsLayer { // Apply TOTAL translation (base + visual drag)
                            val visualOffsetX = if (itemId == interactingItemId) currentDragVisualOffset.x else 0f
                            val visualOffsetY = if (itemId == interactingItemId) currentDragVisualOffset.y else 0f
                            // Combine base position and visual offset
                            translationX = itemBaseAbsoluteX + visualOffsetX
                            translationY = itemBaseAbsoluteY + visualOffsetY
                            // Log.d(TAG_DRAG, "--> Item $itemId: Applying TOTAL graphicsLayer offset: x=${translationX.roundToInt()}, y=${translationY.roundToInt()} (Base: ${itemBaseAbsoluteX.roundToInt()}/${itemBaseAbsoluteY.roundToInt()}, Visual: ${visualOffsetX.roundToInt()}/${visualOffsetY.roundToInt()}, Interacting: ${itemId == interactingItemId})")
                        }
                        .pointerInput(isLocked, isGridValid, itemId, editableItemsState.size) { // Key on local state size
                            if (!isLocked && isGridValid) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        Log.d(TAG_DRAG, "Item $itemId: onDragStart at $offset")
                                        interactingItemId = itemId
                                        currentDragVisualOffset = Offset.Zero
                                        currentResizeVisualDelta = Offset.Zero
                                    },
                                    onDragEnd = {
                                        Log.d(TAG_DRAG, "Item $itemId: onDragEnd. Final visual offset relative to start: $currentDragVisualOffset")
                                        // Find the item in the *local* mutable state
                                        val currentLocalState = editableItemsState.getOrNull(index) ?: run {
                                            Log.e(TAG_DRAG,"Item $itemId: Cannot find state in local list onDragEnd!")
                                            interactingItemId = null
                                            currentDragVisualOffset = Offset.Zero
                                            return@detectDragGestures
                                        }

                                        // Calculate final position based on OLD base + visual drag offset
                                        val finalDraggedX = (currentLocalState.gridCol * cellWidthPx) + currentDragVisualOffset.x
                                        val finalDraggedY = (currentLocalState.gridRow * cellHeightPx) + currentDragVisualOffset.y
                                        // Snap to grid
                                        val targetCol = (finalDraggedX / cellWidthPx).roundToInt()
                                        val targetRow = (finalDraggedY / cellHeightPx).roundToInt()
                                        // Clamp within grid boundaries
                                        val clampedCol = targetCol.coerceIn(0, GRID_COLUMNS - currentLocalState.gridWidth)
                                        val clampedRow = targetRow.coerceIn(0, GRID_ROWS - currentLocalState.gridHeight)

                                        Log.d(TAG_DRAG, "Item $itemId: Drag ended. Target Col/Row: $clampedCol/$clampedRow. Current Col/Row: ${currentLocalState.gridCol}/${currentLocalState.gridRow}")

                                        // *** Modify LOCAL state directly ***
                                        val needsUpdate = currentLocalState.gridCol != clampedCol || currentLocalState.gridRow != clampedRow
                                        if (needsUpdate) {
                                            editableItemsState[index] = currentLocalState.copy(
                                                gridCol = clampedCol,
                                                gridRow = clampedRow
                                            )
                                            hasUnsavedChanges = true // Mark changes
                                            Log.d(TAG_DRAG, "Item $itemId: Updated LOCAL position. hasUnsavedChanges = true")
                                        } else {
                                            Log.d(TAG_DRAG, "Item $itemId: No position change detected.")
                                        }

                                        // *** Reset visual state IMMEDIATELY ***
                                        currentDragVisualOffset = Offset.Zero
                                        interactingItemId = null
                                        // *** DO NOT SAVE HERE ***
                                    },
                                    onDragCancel = {
                                        Log.d(TAG_DRAG, "Item $itemId: onDragCancel")
                                        interactingItemId = null
                                        currentDragVisualOffset = Offset.Zero
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        currentDragVisualOffset += dragAmount
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
                                    enabled = isLocked, // Button enabled only when layout is locked
                                    onPress = { if (isLocked) viewModel.sendCommand(commandIdentifier, context) },
                                    colors = buttonColors,
                                    textSize = textSize,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            // Add other item types here if needed
                        }

                        // --- Resize Handle ---
                        if (!isLocked) {
                            val handleTouchOffsetXDp = with(density) { (minTouchTargetSizePx / 2).toDp() }
                            val handleTouchOffsetYDp = with(density) { (minTouchTargetSizePx / 2).toDp() }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = handleTouchOffsetXDp, y = handleTouchOffsetYDp)
                                    .size(minTouchTargetSizeDp.dp)
                                    .clip(CircleShape)
                                    .pointerInput(Unit, isGridValid, itemId, editableItemsState.size) { // Separate pointerInput for resize
                                        if (isGridValid) {
                                            detectDragGestures(
                                                onDragStart = { offset ->
                                                    Log.d(TAG_DRAG, "Item $itemId: onResizeStart at $offset")
                                                    interactingItemId = itemId
                                                    currentDragVisualOffset = Offset.Zero
                                                    currentResizeVisualDelta = Offset.Zero
                                                },
                                                onDragEnd = {
                                                    Log.d(TAG_DRAG, "Item $itemId: onResizeEnd. Final resize delta: $currentResizeVisualDelta")
                                                    // Find the item in the *local* mutable state
                                                    val currentLocalState = editableItemsState.getOrNull(index) ?: run {
                                                        Log.e(TAG_DRAG,"Item $itemId: Cannot find state in local list onResizeEnd!")
                                                        interactingItemId = null
                                                        currentResizeVisualDelta = Offset.Zero
                                                        return@detectDragGestures
                                                    }

                                                    // Calculate final size based on base + visual resize delta
                                                    val finalResizedWidthPx = (currentLocalState.gridWidth * cellWidthPx) + currentResizeVisualDelta.x
                                                    val finalResizedHeightPx = (currentLocalState.gridHeight * cellHeightPx) + currentResizeVisualDelta.y
                                                    // Snap to grid cells
                                                    val targetWidthCells = max(minGridWidth, (finalResizedWidthPx / cellWidthPx).roundToInt())
                                                    val targetHeightCells = max(minGridHeight, (finalResizedHeightPx / cellHeightPx).roundToInt())
                                                    // Clamp within grid boundaries
                                                    val clampedWidthCells = targetWidthCells.coerceIn(minGridWidth, GRID_COLUMNS - currentLocalState.gridCol)
                                                    val clampedHeightCells = targetHeightCells.coerceIn(minGridHeight, GRID_ROWS - currentLocalState.gridRow)

                                                    Log.d(TAG_DRAG, "Item $itemId: Resize ended. Target W/H Cells: $clampedWidthCells/$clampedHeightCells. Current W/H: ${currentLocalState.gridWidth}/${currentLocalState.gridHeight}")

                                                    // *** Modify LOCAL state directly ***
                                                    val needsUpdate = currentLocalState.gridWidth != clampedWidthCells || currentLocalState.gridHeight != clampedHeightCells
                                                    if(needsUpdate) {
                                                        editableItemsState[index] = currentLocalState.copy(
                                                            gridWidth = clampedWidthCells,
                                                            gridHeight = clampedHeightCells
                                                        )
                                                        hasUnsavedChanges = true // Mark changes
                                                        Log.d(TAG_DRAG, "Item $itemId: Updated LOCAL size. hasUnsavedChanges = true")
                                                    } else {
                                                        Log.d(TAG_DRAG, "Item $itemId: No size change detected.")
                                                    }

                                                    // *** Reset visual state IMMEDIATELY ***
                                                    currentResizeVisualDelta = Offset.Zero
                                                    interactingItemId = null
                                                    // *** DO NOT SAVE HERE ***
                                                },
                                                onDragCancel = {
                                                    Log.d(TAG_DRAG, "Item $itemId: onResizeCancel")
                                                    interactingItemId = null
                                                    currentResizeVisualDelta = Offset.Zero
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    currentResizeVisualDelta += dragAmount
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
                                .offset(x = with(density){(-minTouchTargetSizePx / 2).toDp()}, y = with(density){(-minTouchTargetSizePx / 2).toDp()})
                                .size(minTouchTargetSizeDp.dp)
                                .clip(CircleShape)
                                .clickable(
                                    onClick = {
                                        editingItemState = itemState // Use item from local state
                                        showAddEditDialog = true
                                    },
                                )
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
                                    Icons.Filled.Edit,
                                    contentDescription = "Edit Button",
                                    modifier = Modifier.size(handleIconSizeMultiplier * handleVisualSize), // Corrected calculation
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    } // End Edit Button

                } // End Item Container Box
            } // End forEach

            // --- Top Right Control Buttons ---
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLocked) {
                    IconButton(
                        onClick = {
                            editingItemState = null // Clear editing state for Add mode
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
                    onClick = {
                        val currentlyLocked = isLocked
                        if (!currentlyLocked) { // Transitioning from Unlocked -> Locked
                            if (hasUnsavedChanges) {
                                Log.d(TAG_DRAG, "Locking layout and saving changes.")
                                viewModel.saveFreeFormLayout(editableItemsState.toList()) // Save the current local state
                                hasUnsavedChanges = false // Reset flag after saving
                            } else {
                                Log.d(TAG_DRAG, "Locking layout, no changes to save.")
                                // Optional: If you want to discard local edits when locking without changes,
                                // you could reload from ViewModel here, but current setup syncs via LaunchedEffect.
                            }
                        } else { // Transitioning from Locked -> Unlocked
                            Log.d(TAG_DRAG, "Unlocking layout.")
                            // Ensure local state matches viewmodel state when unlocking
                            if (editableItemsState != itemsStateFromViewModel) {
                                Log.d(TAG_DRAG,"Syncing local state from ViewModel on unlock.")
                                editableItemsState.clear()
                                editableItemsState.addAll(itemsStateFromViewModel)
                                hasUnsavedChanges = false
                            }
                        }
                        isLocked = !currentlyLocked // Toggle the lock state
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.shapes.medium),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = if (isLocked) "Layout Locked" else "Layout Unlocked (Tap to Lock & Save)"
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
        onSave = { text, commandString, type, textSizeSp, backgroundColorHex ->
            val currentEditId = editingItemState?.id
            if (currentEditId != null) {
                // Edit Mode: Update item in LOCAL list
                val index = editableItemsState.indexOfFirst { it.id == currentEditId }
                if (index != -1) {
                    val updatedItem = editableItemsState[index].copy(
                        text = text,
                        commandString = commandString,
                        type = type,
                        textSizeSp = textSizeSp,
                        backgroundColorHex = backgroundColorHex
                    )
                    editableItemsState[index] = updatedItem
                    hasUnsavedChanges = true // Mark changes
                    Log.d(TAG_DRAG, "Updated item $currentEditId locally. hasUnsavedChanges = true")
                }
            } else {
                // Add Mode: Add item to LOCAL list
                val newItem = FreeFormItemState(
                    text = text,
                    commandString = commandString,
                    type = type,
                    textSizeSp = textSizeSp,
                    backgroundColorHex = backgroundColorHex
                )
                editableItemsState.add(newItem)
                hasUnsavedChanges = true // Mark changes
                Log.d(TAG_DRAG, "Added new item locally. hasUnsavedChanges = true")
            }
            showAddEditDialog = false
            // *** DO NOT SAVE TO VIEWMODEL HERE ***
        },
        onDelete = { itemId ->
            // Delete item from LOCAL list
            val removed = editableItemsState.removeIf { it.id == itemId }
            if (removed) {
                hasUnsavedChanges = true // Mark changes
                Log.d(TAG_DRAG, "Deleted item $itemId locally. hasUnsavedChanges = true")
            }
            showAddEditDialog = false
            // *** DO NOT SAVE TO VIEWMODEL HERE ***
        }
    )
}
