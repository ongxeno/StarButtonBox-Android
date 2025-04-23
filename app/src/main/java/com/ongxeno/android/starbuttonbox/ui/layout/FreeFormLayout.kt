package com.ongxeno.android.starbuttonbox.ui.layout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ongxeno.android.starbuttonbox.data.Command
import com.ongxeno.android.starbuttonbox.data.FreeFormItemState
import com.ongxeno.android.starbuttonbox.data.FreeFormItemType
import com.ongxeno.android.starbuttonbox.ui.button.MomentaryButton
import com.ongxeno.android.starbuttonbox.ui.dialog.AddEditButtonDialog
import com.ongxeno.android.starbuttonbox.utils.LocalLayoutDatasource
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

// --- Grid Configuration ---
private const val GRID_COLUMNS = 60
private const val GRID_ROWS = 35
private val gridLineColor = Color.Gray.copy(alpha = 0.4f)
private val gridStrokeWidth = 1.dp
private val gridPathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

// --- Button Size Constraints ---
private val minButtonWidth = 30.dp
private val minButtonHeight = 20.dp
private val maxButtonWidth = 600.dp
private val maxButtonHeight = 400.dp
private val resizeHandleSize = 16.dp
private val editButtonVisualSize = 24.dp // Desired VISUAL size (e.g., 16.dp)
private val editButtonIconSizeMultiplier = 0.5f // Icon size relative to VISUAL size
private val minTouchTargetSize = 48.dp // Minimum touch target size (Material Design guideline) // *** ADDED DEFINITION ***

// --- Define Default Layouts Here ---
private fun getDefaultItemsForLayout(layoutId: String): List<FreeFormItemState> {
    // Create default states using the commandString directly
    return when (layoutId) {
        "freeform_1" -> listOf(
            FreeFormItemState(text = "FLT RDY", commandString = Command.GeneralCockpit_FlightReady),
            FreeFormItemState(text = "GEAR", commandString = Command.LandingAndDocking_ToggleLandingGear),
            FreeFormItemState(text = "ENGINES", commandString = Command.PowerManagement_TogglePowerEngines),
            FreeFormItemState(text = "QT MODE", commandString = Command.QuantumTravel_ToggleQuantumMode)
        )
        "freeform_2" -> listOf(
            FreeFormItemState(text = "SCAN", commandString = Command.Scanning_ToggleScanningMode),
            FreeFormItemState(text = "PING", commandString = Command.Scanning_ActivatePing),
            FreeFormItemState(text = "LOCK TGT", commandString = Command.Targeting_LockSelectedTarget),
            FreeFormItemState(text = "UNLOCK", commandString = Command.Targeting_UnlockLockedTarget)
        )
        else -> emptyList() // Default for unknown IDs
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

    // State for the Add/Edit Dialog
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingItemState by remember { mutableStateOf<FreeFormItemState?>(null) }

    LaunchedEffect(layoutId, layoutDatasource) {
        isLoading = true
        layoutDatasource.getLayoutFlow(layoutId).collect { loadedItems ->
            val itemsToUse = if (loadedItems.isEmpty()) {
                println("Layout '$layoutId' not found in DataStore, loading default.")
                getDefaultItemsForLayout(layoutId)
            } else {
                println("Layout '$layoutId' loaded from DataStore.")
                loadedItems
            }
            mutableItems.clear()
            mutableItems.addAll(itemsToUse)
            isLoading = false
        }
    }

    val scope = rememberCoroutineScope()
    val saveCurrentLayout: (List<FreeFormItemState>) -> Unit = { itemsToSave ->
        scope.launch {
            layoutDatasource.saveLayout(layoutId, itemsToSave)
        }
    }

    // --- Main Layout ---
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val constraints = this.constraints
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()
        val cellWidthPx = if (GRID_COLUMNS > 0) maxWidthPx / GRID_COLUMNS else maxWidthPx
        val cellHeightPx = if (GRID_ROWS > 0) maxHeightPx / GRID_ROWS else maxHeightPx
        val gridStrokeWidthPx = with(density) { gridStrokeWidth.toPx() }

        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                // --- Draw Grid Lines ---
                if (!isLocked && GRID_COLUMNS > 0 && GRID_ROWS > 0) {
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

                // --- Render Items ---
                mutableItems.forEachIndexed { index, itemState ->
                    val commandIdentifier = itemState.commandString
                    val itemWidth = remember(itemState.widthDp) { Dp(itemState.widthDp) }
                    val itemHeight = remember(itemState.heightDp) { Dp(itemState.heightDp) }
                    val itemOffsetX = itemState.offsetX
                    val itemOffsetY = itemState.offsetY

                    Box( // Container for item + edit button
                        modifier = Modifier
                            .offset { IntOffset(itemOffsetX.roundToInt(), itemOffsetY.roundToInt()) }
                            .size(itemWidth, itemHeight)
                    ) {
                        // --- Item Content Box (handles drag, resize, rendering) ---
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(isLocked, cellWidthPx, cellHeightPx, itemState.id) {
                                    if (!isLocked) {
                                        detectDragGestures(
                                            onDragEnd = {
                                                // Snap Position Logic (unchanged)
                                                val currentState = mutableItems[index]
                                                val currentLocalWidthPx = with(density) { Dp(currentState.widthDp).toPx() }
                                                val currentLocalHeightPx = with(density) { Dp(currentState.heightDp).toPx() }
                                                val targetCol = if (cellWidthPx > 0) (currentState.offsetX / cellWidthPx).roundToInt() else 0
                                                val targetRow = if (cellHeightPx > 0) (currentState.offsetY / cellHeightPx).roundToInt() else 0
                                                val coercedCol = targetCol.coerceIn(0, GRID_COLUMNS - 1)
                                                val coercedRow = targetRow.coerceIn(0, GRID_ROWS - 1)
                                                val snappedX = coercedCol * cellWidthPx
                                                val snappedY = coercedRow * cellHeightPx
                                                val finalX = snappedX.coerceIn(0f, maxWidthPx - currentLocalWidthPx)
                                                val finalY = snappedY.coerceIn(0f, maxHeightPx - currentLocalHeightPx)
                                                mutableItems[index] = currentState.copy(offsetX = finalX, offsetY = finalY)
                                                saveCurrentLayout(mutableItems.toList())
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            // Update offset during drag (unchanged)
                                            val currentState = mutableItems[index]
                                            val currentLocalWidthPx = with(density) { Dp(currentState.widthDp).toPx() }
                                            val currentLocalHeightPx = with(density) { Dp(currentState.heightDp).toPx() }
                                            val newOffset = Offset(currentState.offsetX, currentState.offsetY) + dragAmount
                                            val coercedX = newOffset.x.coerceIn(0f, maxWidthPx - currentLocalWidthPx)
                                            val coercedY = newOffset.y.coerceIn(0f, maxHeightPx - currentLocalHeightPx)
                                            mutableItems[index] = currentState.copy(offsetX = coercedX, offsetY = coercedY)
                                        }
                                    }
                                }
                        ) {
                            // --- Render the actual component based on type ---
                            when (itemState.type) {
                                FreeFormItemType.MOMENTARY_BUTTON -> {
                                    MomentaryButton(
                                        modifier = Modifier.fillMaxSize(),
                                        text = itemState.text,
                                        enabled = isLocked,
                                        onPress = { if (isLocked) onCommand(commandIdentifier) }
                                    )
                                }
                            }

                            // --- Resize Handle ---
                            if (!isLocked) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .offset(x = resizeHandleSize / 2, y = resizeHandleSize / 2)
                                        .size(resizeHandleSize)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .pointerInput(Unit, cellWidthPx, cellHeightPx, itemState.id) {
                                            detectDragGestures(
                                                onDragEnd = {
                                                    // Snap Size Logic (unchanged)
                                                    val currentState = mutableItems[index]
                                                    val currentLocalWidthPx = with(density) { Dp(currentState.widthDp).toPx() }
                                                    val currentLocalHeightPx = with(density) { Dp(currentState.heightDp).toPx() }
                                                    val targetWidthCells = max(1, if (cellWidthPx > 0) (currentLocalWidthPx / cellWidthPx).roundToInt() else 1)
                                                    val targetHeightCells = max(1, if (cellHeightPx > 0) (currentLocalHeightPx / cellHeightPx).roundToInt() else 1)
                                                    val snappedWidthPx = targetWidthCells * cellWidthPx
                                                    val snappedHeightPx = targetHeightCells * cellHeightPx
                                                    val snappedWidthDp = with(density) { snappedWidthPx.toDp() }.coerceIn(minButtonWidth, maxButtonWidth)
                                                    val snappedHeightDp = with(density) { snappedHeightPx.toDp() }.coerceIn(minButtonHeight, maxButtonHeight)
                                                    mutableItems[index] = currentState.copy(widthDp = snappedWidthDp.value, heightDp = snappedHeightDp.value)
                                                    saveCurrentLayout(mutableItems.toList())
                                                }
                                            ) { change, dragAmount ->
                                                change.consume()
                                                // Update size during drag (unchanged)
                                                val currentState = mutableItems[index]
                                                val currentLocalWidthPx = with(density) { Dp(currentState.widthDp).toPx() }
                                                val currentLocalHeightPx = with(density) { Dp(currentState.heightDp).toPx() }
                                                val newWidthPx = currentLocalWidthPx + dragAmount.x
                                                val newHeightPx = currentLocalHeightPx + dragAmount.y
                                                val newWidthDp = with(density) { newWidthPx.toDp() }.coerceIn(minButtonWidth, maxButtonWidth)
                                                val newHeightDp = with(density) { newHeightPx.toDp() }.coerceIn(minButtonHeight, maxButtonHeight)
                                                mutableItems[index] = currentState.copy(widthDp = newWidthDp.value, heightDp = newHeightDp.value)
                                            }
                                        }
                                )
                            }
                        } // End Item Content Box

                        // --- Edit Button (Top Left Corner - Option 3) ---
                        if (!isLocked) {
                            // IconButton provides the larger touch target and handles clicks
                            IconButton(
                                onClick = {
                                    editingItemState = itemState
                                    showAddEditDialog = true
                                },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(x = (-minTouchTargetSize / 2), y = (-minTouchTargetSize / 2)) // Offset based on visual size
                                    // Ensure IconButton has minimum touch target size (or default)
                                    .sizeIn(minWidth = minTouchTargetSize, minHeight = minTouchTargetSize), // Use the defined constant
                                // Make IconButton background transparent
                                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Transparent)
                            ) {
                                // Inner Box provides the small visual appearance
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(editButtonVisualSize) // Set the desired visual size
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f))
                                        .border(1.dp, MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha=0.5f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "Edit Button", // Content description on IconButton is better for accessibility
                                        modifier = Modifier.size(editButtonVisualSize * editButtonIconSizeMultiplier), // Size icon relative to visual size
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        } // End Edit Button
                    } // End Container Box
                } // End forEachIndexed
            } // End else (isLoading)

            // --- Top Right Control Buttons (Lock/Unlock and Add) ---
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add Button
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
                        Icon(Icons.Filled.Add, "Add Button", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                // Lock/Unlock Button
                IconButton(
                    onClick = { isLocked = !isLocked },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.shapes.medium),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen, contentDescription = if (isLocked) "Layout Locked" else "Layout Unlocked")
                }
            } // End Top Right Controls Row
        } // End Main Content Box
    } // End BoxWithConstraints

    // --- Add/Edit Dialog ---
    AddEditButtonDialog(
        showDialog = showAddEditDialog,
        onDismiss = { showAddEditDialog = false },
        initialItemState = editingItemState,
        onSave = { text, commandString, type ->
            val currentEditId = editingItemState?.id
            if (currentEditId != null) {
                // Edit Mode
                val indexToUpdate = mutableItems.indexOfFirst { it.id == currentEditId }
                if (indexToUpdate != -1) {
                    mutableItems[indexToUpdate] = mutableItems[indexToUpdate].copy(
                        text = text,
                        commandString = commandString,
                        type = type
                    )
                    saveCurrentLayout(mutableItems.toList())
                } else {
                    println("Error: Could not find item with ID $currentEditId to update.")
                }
            } else {
                // Add Mode
                val newItem = FreeFormItemState(
                    text = text,
                    commandString = commandString,
                    type = type,
                    offsetX = 0f, offsetY = 0f, widthDp = 120f, heightDp = 50f
                )
                mutableItems.add(newItem)
                saveCurrentLayout(mutableItems.toList())
            }
            showAddEditDialog = false
        }
    )
}

// Example Usage (Unchanged)
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
