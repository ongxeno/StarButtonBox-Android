package com.ongxeno.android.starbuttonbox.ui.layout

import androidx.compose.foundation.Canvas // Import Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size // Import size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver // Import Saver
import androidx.compose.runtime.saveable.listSaver // Keep listSaver for DraggableButtonStateSaver if preferred
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect // For dashed lines
import androidx.compose.ui.graphics.drawscope.Stroke // For stroke style
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp // Import Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ongxeno.android.starbuttonbox.data.Command // Import your Command class
import com.ongxeno.android.starbuttonbox.ui.button.MomentaryButton // Import your Button composable
import java.util.UUID
import kotlin.math.max // Import max
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
// Min size should ideally be at least 1 grid cell equivalent if possible
private val minButtonWidth = 30.dp
private val minButtonHeight = 20.dp
private val maxButtonWidth = 600.dp
private val maxButtonHeight = 400.dp
private val resizeHandleSize = 16.dp // Size of the resize handle

/**
 * Data class to hold the state of a draggable and resizable button.
 */
data class DraggableButtonState(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val command: Command,
    var offset: Offset = Offset.Zero,
    var width: Dp = 120.dp, // Default width
    var height: Dp = 50.dp  // Default height
)

// --- Saver definitions (Unchanged) ---
val DraggableButtonStateSaver = listSaver<DraggableButtonState, Any>(
    save = { buttonState ->
        listOf(
            buttonState.id, buttonState.text, buttonState.command.actionName,
            buttonState.offset.x, buttonState.offset.y,
            buttonState.width.value, buttonState.height.value
        )
    },
    restore = { savedList ->
        val command = Command.fromActionName(savedList[2] as String)
        if (command != null) {
            DraggableButtonState(
                id = savedList[0] as String, text = savedList[1] as String, command = command,
                offset = Offset(savedList[3] as Float, savedList[4] as Float),
                width = Dp(savedList[5] as Float), height = Dp(savedList[6] as Float)
            )
        } else { println("Warning: Could not restore command with actionName: ${savedList[2]}"); null }
    }
)
val ButtonStateListSaver = Saver<MutableList<DraggableButtonState>, List<Any>>(
    save = { stateList ->
        stateList.map { item ->
            listOf(
                item.id, item.text, item.command.actionName,
                item.offset.x, item.offset.y, item.width.value, item.height.value
            )
        }
    },
    restore = { savedList ->
        savedList.mapNotNull { itemData ->
            if (itemData is List<*>) {
                @Suppress("UNCHECKED_CAST")
                val data = itemData as List<Any>
                val command = Command.fromActionName(data[2] as String)
                if (command != null) {
                    DraggableButtonState(
                        id = data[0] as String, text = data[1] as String, command = command,
                        offset = Offset(data[3] as Float, data[4] as Float),
                        width = Dp(data[5] as Float), height = Dp(data[6] as Float)
                    )
                } else { println("Warning: Could not restore command with actionName: ${data[2]}"); null }
            } else { println("Warning: Invalid saved item data format: $itemData"); null }
        }.toMutableStateList()
    }
)


/**
 * A composable layout that allows placing buttons freely on the screen, with grid snapping
 * for both position and size.
 * Features a lock/unlock mode:
 * - Locked: Buttons are fixed and trigger commands.
 * - Unlocked: A grid is shown, buttons can be dragged (snapping on release) and resized (snapping on release).
 *
 * @param onCommand A lambda function called when a button is pressed in locked mode.
 * @param modifier Optional Modifier for the layout container.
 * @param initialButtons An optional list of pairs defining the initial buttons (Text label, Command object).
 * Defaults to a predefined list of 4 buttons if not provided.
 */
@Composable
fun FreeFormLayout(
    // Required parameter first
    onCommand: (Command) -> Unit,
    // Optional parameters follow
    modifier: Modifier = Modifier,
    // Default list is now inlined here
    initialButtons: List<Pair<String, Command>> = listOf(
        "FLT RDY" to Command.GeneralCockpit.FlightReady,
        "GEAR" to Command.LandingAndDocking.ToggleLandingGear,
        "ENGINES" to Command.PowerManagement.TogglePowerEngines,
        "QT MODE" to Command.QuantumTravel.ToggleQuantumMode
    )
) {
    var isLocked by rememberSaveable { mutableStateOf(true) }

    // Initialize state directly using initialButtons (it will always have a value)
    val buttonStates = rememberSaveable(inputs = arrayOf(initialButtons), saver = ButtonStateListSaver) {
        initialButtons.map { (text, command) ->
            DraggableButtonState(text = text, command = command)
        }.toMutableStateList()
    }

    // Apply the modifier to the outermost BoxWithConstraints
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val constraints = this.constraints
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()

        // Calculate grid cell dimensions based on updated counts
        val cellWidthPx = if (GRID_COLUMNS > 0) maxWidthPx / GRID_COLUMNS else maxWidthPx
        val cellHeightPx = if (GRID_ROWS > 0) maxHeightPx / GRID_ROWS else maxHeightPx
        val gridStrokeWidthPx = with(density) { gridStrokeWidth.toPx() }

        Box(modifier = Modifier.fillMaxSize()) { // Inner Box doesn't need the modifier again

            // --- Draw Grid Lines (if unlocked) ---
            if (!isLocked && GRID_COLUMNS > 0 && GRID_ROWS > 0) { // Only draw if grid exists
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw vertical lines
                    for (i in 1 until GRID_COLUMNS) {
                        val x = i * cellWidthPx
                        drawLine(
                            color = gridLineColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = gridStrokeWidthPx,
                            pathEffect = gridPathEffect // Apply path effect
                        )
                    }
                    // Draw horizontal lines
                    for (i in 1 until GRID_ROWS) {
                        val y = i * cellHeightPx
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = gridStrokeWidthPx,
                            pathEffect = gridPathEffect // Apply path effect
                        )
                    }
                }
            }

            // --- Render Buttons ---
            buttonStates.forEachIndexed { index, buttonState ->
                val currentWidthPx = with(density) { buttonState.width.toPx() }
                val currentHeightPx = with(density) { buttonState.height.toPx() }

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                buttonState.offset.x.roundToInt(),
                                buttonState.offset.y.roundToInt()
                            )
                        }
                        .size(buttonState.width, buttonState.height)
                        .pointerInput(isLocked, cellWidthPx, cellHeightPx) { // Key includes cell size
                            if (!isLocked) {
                                detectDragGestures(
                                    onDragEnd = {
                                        // --- Snap Position Logic ---
                                        val currentState = buttonStates[index]
                                        val currentWidthPxLocal = with(density) { currentState.width.toPx() }
                                        val currentHeightPxLocal = with(density) { currentState.height.toPx() }
                                        val targetCol = if (cellWidthPx > 0) (currentState.offset.x / cellWidthPx).roundToInt() else 0
                                        val targetRow = if (cellHeightPx > 0) (currentState.offset.y / cellHeightPx).roundToInt() else 0
                                        val coercedCol = targetCol.coerceIn(0, GRID_COLUMNS - 1)
                                        val coercedRow = targetRow.coerceIn(0, GRID_ROWS - 1)
                                        val snappedX = coercedCol * cellWidthPx
                                        val snappedY = coercedRow * cellHeightPx
                                        val finalX = snappedX.coerceIn(0f, maxWidthPx - currentWidthPxLocal)
                                        val finalY = snappedY.coerceIn(0f, maxHeightPx - currentHeightPxLocal)
                                        buttonStates[index] = currentState.copy(offset = Offset(finalX, finalY))
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    // Update offset state during drag (no snapping yet)
                                    val currentState = buttonStates[index]
                                    val currentWidthPxLocal = with(density) { currentState.width.toPx() }
                                    val currentHeightPxLocal = with(density) { currentState.height.toPx() }
                                    val newOffset = currentState.offset + dragAmount
                                    val coercedX = newOffset.x.coerceIn(0f, maxWidthPx - currentWidthPxLocal)
                                    val coercedY = newOffset.y.coerceIn(0f, maxHeightPx - currentHeightPxLocal)
                                    buttonStates[index] = currentState.copy(offset = Offset(coercedX, coercedY))
                                }
                            }
                        }
                ) {
                    MomentaryButton(
                        modifier = Modifier.fillMaxSize(),
                        text = buttonState.text,
                        enabled = isLocked,
                        onPress = { if (isLocked) onCommand(buttonState.command) }
                    )

                    // Resize Handle
                    if (!isLocked) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = resizeHandleSize / 2, y = resizeHandleSize / 2)
                                .size(resizeHandleSize)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .pointerInput(Unit, cellWidthPx, cellHeightPx) { // Add cell sizes to key
                                    detectDragGestures(
                                        onDragEnd = {
                                            // --- Snap Size Logic ---
                                            val currentState = buttonStates[index]
                                            val finalWidthPx = with(density) { currentState.width.toPx() }
                                            val finalHeightPx = with(density) { currentState.height.toPx() }

                                            // Calculate target cells ensuring at least 1 cell
                                            val targetWidthCells = max(1, if (cellWidthPx > 0) (finalWidthPx / cellWidthPx).roundToInt() else 1)
                                            val targetHeightCells = max(1, if (cellHeightPx > 0) (finalHeightPx / cellHeightPx).roundToInt() else 1)

                                            // Calculate snapped size in pixels
                                            val snappedWidthPx = targetWidthCells * cellWidthPx
                                            val snappedHeightPx = targetHeightCells * cellHeightPx

                                            // Convert back to Dp and apply constraints
                                            val snappedWidthDp = with(density) { snappedWidthPx.toDp() }
                                                .coerceIn(minButtonWidth, maxButtonWidth)
                                            val snappedHeightDp = with(density) { snappedHeightPx.toDp() }
                                                .coerceIn(minButtonHeight, maxButtonHeight)

                                            // Update state with snapped size
                                            buttonStates[index] = currentState.copy(
                                                width = snappedWidthDp,
                                                height = snappedHeightDp
                                            )
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        // Update size state during drag (no snapping yet)
                                        val currentState = buttonStates[index]
                                        val currentWidthPxLocal = with(density) { currentState.width.toPx() }
                                        val currentHeightPxLocal = with(density) { currentState.height.toPx() }
                                        val newWidthPx = currentWidthPxLocal + dragAmount.x
                                        val newHeightPx = currentHeightPxLocal + dragAmount.y
                                        val newWidthDp = with(density) { newWidthPx.toDp() }
                                            .coerceIn(minButtonWidth, maxButtonWidth)
                                        val newHeightDp = with(density) { newHeightPx.toDp() }
                                            .coerceIn(minButtonHeight, maxButtonHeight)
                                        buttonStates[index] = currentState.copy(
                                            width = newWidthDp, height = newHeightDp
                                        )
                                    }
                                }
                        )
                    }
                }
            }

            // --- Lock/Unlock Toggle Button (Unchanged) ---
            IconButton(
                onClick = { isLocked = !isLocked },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        shape = MaterialTheme.shapes.medium
                    ),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = if (isLocked) "Layout Locked (Tap to Unlock)" else "Layout Unlocked (Tap to Lock)"
                )
            }
        }
    }
}


// --- Helper function needed in Command data class ---
// (Keep the implementation from the previous version in Command.kt)
/*
companion object {
    fun fromActionName(actionName: String): Command? { ... }
    fun getAllCommands(): List<Command> { ... }
}
*/

// --- Example Usage ---
// Shows calling with and without providing initialButtons
@Composable
fun ExampleFreeFormLayoutUsage(onCommand: (Command) -> Unit) {

    // Example 1: Using the default buttons (no initialButtons argument needed)
    // FreeFormLayout(onCommand = onCommand)

    // Example 2: Providing a custom list of buttons
    val myCustomButtons = remember {
        listOf(
            "FLT RDY" to Command.GeneralCockpit.FlightReady,
            "ENGINES" to Command.PowerManagement.TogglePowerEngines,
            "SHIELDS" to Command.PowerManagement.TogglePowerShields,
            "WEAPONS" to Command.PowerManagement.TogglePowerWeapons,
            "GEAR" to Command.LandingAndDocking.ToggleLandingGear,
            "QT MODE" to Command.QuantumTravel.ToggleQuantumMode,
            "QT JUMP" to Command.QuantumTravel.ActivateQuantumTravel,
            "SCAN" to Command.Scanning.ToggleScanningMode,
            "PING" to Command.Scanning.ActivatePing,
            "LOCK TGT" to Command.Targeting.LockSelectedTarget,
            "UNLOCK" to Command.Targeting.UnlockLockedTarget,
            "EJECT" to Command.GeneralCockpit.Eject
        )
    }
    FreeFormLayout(
        onCommand = onCommand, // Required param first
        modifier = Modifier.background(Color.Black.copy(alpha = 0.9f)), // Modifier next
        initialButtons = myCustomButtons // Other optional params last
    )
}
