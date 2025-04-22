package com.ongxeno.android.starbuttonbox.ui.layout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable // Import rememberSaveable
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
import com.ongxeno.android.starbuttonbox.data.getCommandFromString
import com.ongxeno.android.starbuttonbox.datasource.LayoutDatasource // Needed for getDefaultItemsForLayout logic
import com.ongxeno.android.starbuttonbox.ui.button.MomentaryButton
import com.ongxeno.android.starbuttonbox.utils.LocalLayoutDatasource // Import CompositionLocal key
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

// --- Grid Configuration ---
private const val GRID_COLUMNS = 60 // Updated grid column count
private const val GRID_ROWS = 35    // Updated grid row count
private val gridLineColor = Color.Gray.copy(alpha = 0.4f)
private val gridStrokeWidth = 1.dp // Slightly thicker lines again
// Optional: Dashed line effect for grid
private val gridPathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
// private val gridPathEffect = null // Use null for solid lines

// --- Button Size Constraints ---
private val minButtonWidth = 30.dp
private val minButtonHeight = 20.dp
private val maxButtonWidth = 600.dp
private val maxButtonHeight = 400.dp
private val resizeHandleSize = 16.dp // Size of the resize handle


// --- Define Default Layouts Here ---
// This function determines the default items if none are saved for a layoutId
private fun getDefaultItemsForLayout(layoutId: String): List<FreeFormItemState> {

    return when (layoutId) {
        "freeform_1" -> listOfNotNull( // Use listOfNotNull to skip if a command failed to load
            FreeFormItemState.from(text = "FLT RDY", command = Command.GeneralCockpit.FlightReady),
            FreeFormItemState.from(text = "GEAR", command = Command.LandingAndDocking.ToggleLandingGear),
            FreeFormItemState.from(text = "ENGINES", command = Command.PowerManagement.TogglePowerEngines),
            FreeFormItemState.from(text = "QT MODE", command = Command.QuantumTravel.ToggleQuantumMode),
        )
        "freeform_2" -> listOfNotNull(
            FreeFormItemState.from(text = "SCAN", command = Command.Scanning.ToggleScanningMode),
            FreeFormItemState.from(text = "PING", command = Command.Scanning.ActivatePing),
            FreeFormItemState.from(text = "LOCK TGT", command = Command.Targeting.LockSelectedTarget),
            FreeFormItemState.from(text = "UNLOCK", command = Command.GeneralCockpit.UnlockDoors),
        )
        else -> emptyList() // Default for unknown IDs
    }
}


/**
 * A persistent, composable layout that allows placing buttons freely on the screen,
 * with grid snapping for both position and size. Loads/saves state via LayoutDatasource
 * obtained from CompositionLocal (LocalLayoutDatasource). Handles its own default state.
 *
 * @param layoutId A unique identifier for this specific layout instance (used for persistence).
 * @param onCommand A lambda function called when a button is pressed in locked mode.
 * @param modifier Optional Modifier for the layout container.
 */
@Composable
fun FreeFormLayout(
    layoutId: String, // Required ID for persistence
    onCommand: (Command) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Access LayoutDatasource via CompositionLocal
    val layoutDatasource = LocalLayoutDatasource.current

    var isLocked by rememberSaveable(layoutId) { mutableStateOf(true) } // Key saveable by layoutId
    var isLoading by remember(layoutId) { mutableStateOf(true) } // Loading state

    // Remember the list as a mutable state list for efficient updates during drag/resize
    val mutableItems = remember { mutableStateListOf<FreeFormItemState>() }

    // Effect to load initial state and observe changes from the datasource
    LaunchedEffect(layoutId, layoutDatasource) {
        isLoading = true // Start loading
        layoutDatasource.getLayoutFlow(layoutId).collect { loadedItems ->
            val itemsToUse = if (loadedItems.isEmpty()) {
                // Load default layout if nothing is saved for this ID yet
                println("Layout '$layoutId' not found in DataStore, loading default.")
                getDefaultItemsForLayout(layoutId)
            } else {
                println("Layout '$layoutId' loaded from DataStore.")
                loadedItems
            }
            // Update the mutable list for UI interaction
            // Ensure thread safety if collect runs on a different dispatcher, though DataStore flows usually emit on Main
            // withContext(Dispatchers.Main) { // Optional: Ensure update happens on Main thread
            mutableItems.clear()
            mutableItems.addAll(itemsToUse)
            isLoading = false // Finish loading
            // }
        }
    }

    val scope = rememberCoroutineScope()
    // Function to save the current state of mutableItems
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

            // Show loading indicator until state is loaded
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                // --- Draw Grid Lines (if unlocked) ---
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

                // --- Render Buttons from mutableItems ---
                mutableItems.forEachIndexed { index, itemState ->
                    // Reconstruct Command object - handle potential null if actionName is invalid
                    val command = remember(itemState.commandString) {
                        getCommandFromString(itemState.commandString)
                    }

                    if (command == null) {
                        // Optionally render a placeholder or skip if command is invalid
                        println("Warning: Could not find command for actionName: ${itemState.commandString}")
                        // Consider adding error UI or logging
                        return@forEachIndexed // Skip rendering this item
                    }

                    val itemWidth = remember(itemState.widthDp) { Dp(itemState.widthDp) }
                    val itemHeight = remember(itemState.heightDp) { Dp(itemState.heightDp) }
                    val itemOffsetX = itemState.offsetX
                    val itemOffsetY = itemState.offsetY

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(itemOffsetX.roundToInt(), itemOffsetY.roundToInt()) }
                            .size(itemWidth, itemHeight)
                            .pointerInput(isLocked, cellWidthPx, cellHeightPx, itemState.id) { // Key includes item ID
                                if (!isLocked) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            // Snap Position Logic
                                            val currentState = mutableItems[index] // Read latest state
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

                                            // Update local mutable state
                                            mutableItems[index] = currentState.copy(offsetX = finalX, offsetY = finalY)
                                            // Save the entire layout state
                                            saveCurrentLayout(mutableItems.toList())
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        // Update offset during drag
                                        val currentState = mutableItems[index] // Read latest state
                                        val currentLocalWidthPx = with(density) { Dp(currentState.widthDp).toPx() }
                                        val currentLocalHeightPx = with(density) { Dp(currentState.heightDp).toPx() }
                                        val newOffset = Offset(currentState.offsetX, currentState.offsetY) + dragAmount
                                        val coercedX = newOffset.x.coerceIn(0f, maxWidthPx - currentLocalWidthPx)
                                        val coercedY = newOffset.y.coerceIn(0f, maxHeightPx - currentLocalHeightPx)

                                        // Update local mutable state directly for smooth dragging
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
                                    onPress = { if (isLocked) onCommand(command) }
                                    // Add other necessary parameters
                                )
                            }
                            // Add cases for other types later
                        }


                        // Resize Handle
                        if (!isLocked) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = resizeHandleSize / 2, y = resizeHandleSize / 2)
                                    .size(resizeHandleSize)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .pointerInput(Unit, cellWidthPx, cellHeightPx, itemState.id) { // Key includes item ID
                                        detectDragGestures(
                                            onDragEnd = {
                                                // Snap Size Logic
                                                val currentState = mutableItems[index] // Read latest state
                                                val currentLocalWidthPx = with(density) { Dp(currentState.widthDp).toPx() }
                                                val currentLocalHeightPx = with(density) { Dp(currentState.heightDp).toPx() }

                                                val targetWidthCells = max(1, if (cellWidthPx > 0) (currentLocalWidthPx / cellWidthPx).roundToInt() else 1)
                                                val targetHeightCells = max(1, if (cellHeightPx > 0) (currentLocalHeightPx / cellHeightPx).roundToInt() else 1)
                                                val snappedWidthPx = targetWidthCells * cellWidthPx
                                                val snappedHeightPx = targetHeightCells * cellHeightPx
                                                val snappedWidthDp = with(density) { snappedWidthPx.toDp() }.coerceIn(minButtonWidth, maxButtonWidth)
                                                val snappedHeightDp = with(density) { snappedHeightPx.toDp() }.coerceIn(minButtonHeight, maxButtonHeight)

                                                // Update local mutable state
                                                mutableItems[index] = currentState.copy(
                                                    widthDp = snappedWidthDp.value,
                                                    heightDp = snappedHeightDp.value
                                                )
                                                // Save the entire layout state
                                                saveCurrentLayout(mutableItems.toList())
                                            }
                                        ) { change, dragAmount ->
                                            change.consume()
                                            // Update size during drag
                                            val currentState = mutableItems[index] // Read latest state
                                            val currentLocalWidthPx = with(density) { Dp(currentState.widthDp).toPx() }
                                            val currentLocalHeightPx = with(density) { Dp(currentState.heightDp).toPx() }
                                            val newWidthPx = currentLocalWidthPx + dragAmount.x
                                            val newHeightPx = currentLocalHeightPx + dragAmount.y
                                            val newWidthDp = with(density) { newWidthPx.toDp() }.coerceIn(minButtonWidth, maxButtonWidth)
                                            val newHeightDp = with(density) { newHeightPx.toDp() }.coerceIn(minButtonHeight, maxButtonHeight)

                                            // Update local mutable state directly
                                            mutableItems[index] = currentState.copy(
                                                widthDp = newWidthDp.value,
                                                heightDp = newHeightDp.value
                                            )
                                        }
                                    }
                            )
                        }
                    }
                }
            }

            // --- Lock/Unlock Toggle Button (Unchanged) ---
            IconButton(
                onClick = { isLocked = !isLocked },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.shapes.medium),
                colors = IconButtonDefaults.iconButtonColors(contentColor = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
            ) {
                Icon(imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen, contentDescription = if (isLocked) "Layout Locked" else "Layout Unlocked")
            }
        }
    }
}

// Example Usage (Unchanged, still valid)
@Composable
fun ExampleFreeFormLayoutUsage(
    layoutId: String, // Pass the specific ID
    onCommand: (Command) -> Unit
) {
    // LayoutDatasource is accessed internally via LocalLayoutDatasource.current
    FreeFormLayout(
        layoutId = layoutId,
        onCommand = onCommand,
        modifier = Modifier.background(Color.Black.copy(alpha = 0.9f))
    )
}
