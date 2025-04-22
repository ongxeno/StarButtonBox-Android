package com.ongxeno.android.starbuttonbox.ui.layout

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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp // Import Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ongxeno.android.starbuttonbox.data.Command // Import your Command class
import com.ongxeno.android.starbuttonbox.ui.button.MomentaryButton // Import your Button composable
import java.util.UUID
import kotlin.math.roundToInt

// Define minimum and maximum button sizes
private val minButtonWidth = 60.dp
private val minButtonHeight = 30.dp
private val maxButtonWidth = 400.dp
private val maxButtonHeight = 200.dp
private val resizeHandleSize = 16.dp // Size of the resize handle

/**
 * Data class to hold the state of a draggable and resizable button.
 *
 * @param id A unique identifier for the button state instance.
 * @param text The text label displayed on the button.
 * @param command The Command object triggered when the button is pressed (in locked mode).
 * @param offset The current position (x, y) of the button relative to its container.
 * @param width The current width of the button.
 * @param height The current height of the button.
 */
data class DraggableButtonState(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val command: Command,
    var offset: Offset = Offset.Zero,
    var width: Dp = 120.dp, // Default width
    var height: Dp = 50.dp  // Default height
)

// --- Saver definitions ---

/**
 * Saver for a single DraggableButtonState, now including width and height.
 */
val DraggableButtonStateSaver = listSaver<DraggableButtonState, Any>(
    save = { buttonState ->
        // List of properties: id, text, command actionName, offsetX, offsetY, width(value), height(value)
        listOf(
            buttonState.id,
            buttonState.text,
            buttonState.command.actionName,
            buttonState.offset.x,
            buttonState.offset.y,
            buttonState.width.value, // Save Dp value as Float
            buttonState.height.value // Save Dp value as Float
        )
    },
    restore = { savedList ->
        val command = Command.fromActionName(savedList[2] as String)

        if (command != null) {
            DraggableButtonState(
                id = savedList[0] as String,
                text = savedList[1] as String,
                command = command,
                offset = Offset(savedList[3] as Float, savedList[4] as Float),
                width = Dp(savedList[5] as Float), // Restore Float to Dp
                height = Dp(savedList[6] as Float) // Restore Float to Dp
            )
        } else {
            println("Warning: Could not restore command with actionName: ${savedList[2]}")
            null
        }
    }
)

/**
 * Saver for a MutableList of DraggableButtonState objects, updated for width/height.
 */
val ButtonStateListSaver = Saver<MutableList<DraggableButtonState>, List<Any>>(
    save = { stateList ->
        // Map each state to its saveable representation including width/height values
        stateList.map { item ->
            listOf(
                item.id,
                item.text,
                item.command.actionName,
                item.offset.x,
                item.offset.y,
                item.width.value,
                item.height.value
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
                        id = data[0] as String,
                        text = data[1] as String,
                        command = command,
                        offset = Offset(data[3] as Float, data[4] as Float),
                        width = Dp(data[5] as Float), // Restore width
                        height = Dp(data[6] as Float) // Restore height
                    )
                } else {
                    println("Warning: Could not restore command with actionName: ${data[2]}")
                    null
                }
            } else {
                println("Warning: Invalid saved item data format: $itemData")
                null
            }
        }.toMutableStateList()
    }
)


/**
 * A composable layout that allows placing buttons freely on the screen.
 * Features a lock/unlock mode:
 * - Locked: Buttons are fixed and trigger their assigned commands on press.
 * - Unlocked: Buttons can be dragged to new positions and resized.
 * Button positions and sizes are saved and restored.
 *
 * @param initialButtons A list of pairs defining the initial buttons (Text label, Command object).
 * @param onCommand A lambda function called when a button is pressed in locked mode.
 * @param modifier Optional Modifier for the layout container.
 */
@Composable
fun FreeFormLayout(
    modifier: Modifier = Modifier,
    initialButtons: List<Pair<String, Command>> = listOf(
        "FLT RDY" to Command.GeneralCockpit.FlightReady,
        "ENGINES" to Command.PowerManagement.TogglePowerEngines,
        "SHIELDS" to Command.PowerManagement.TogglePowerShields,
        "WEAPONS" to Command.PowerManagement.TogglePowerWeapons
    ),
    onCommand: (Command) -> Unit
) {
    var isLocked by rememberSaveable { mutableStateOf(true) }
    val buttonStates = rememberSaveable(saver = ButtonStateListSaver) {
        initialButtons.map { (text, command) ->
            DraggableButtonState(text = text, command = command)
        }.toMutableStateList()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val constraints = this.constraints

        Box(modifier = Modifier.fillMaxSize()) {

            buttonStates.forEachIndexed { index, buttonState ->
                // Read size in Px for bounds checking (needed before the Box)
                val currentWidthPx = with(density) { buttonState.width.toPx() }
                val currentHeightPx = with(density) { buttonState.height.toPx() }

                // Outer Box acts as the container for the button and its resize handle
                Box(
                    modifier = Modifier
                        .offset {
                            // Coerce offset based on current size and container constraints
                            val coercedX = buttonState.offset.x.coerceIn(0f, constraints.maxWidth - currentWidthPx)
                            val coercedY = buttonState.offset.y.coerceIn(0f, constraints.maxHeight - currentHeightPx)
                            IntOffset(coercedX.roundToInt(), coercedY.roundToInt())
                        }
                        .size(buttonState.width, buttonState.height) // Apply size to the outer container
                        .pointerInput(isLocked) { // Dragging the whole button
                            if (!isLocked) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    // Update offset state based on previous offset
                                    val currentState = buttonStates[index] // Read current state
                                    buttonStates[index] = currentState.copy(
                                        offset = currentState.offset + dragAmount
                                    )
                                }
                            }
                        }
                ) {
                    // --- Render the actual button ---
                    MomentaryButton(
                        modifier = Modifier.fillMaxSize(), // Button fills its container
                        text = buttonState.text,
                        enabled = isLocked,
                        onPress = { if (isLocked) onCommand(buttonState.command) }
                    )

                    // --- Resize Handle (Bottom-Right) ---
                    if (!isLocked) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = resizeHandleSize / 2, y = resizeHandleSize / 2) // Offset slightly for better grab
                                .size(resizeHandleSize)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .pointerInput(Unit) { // Resizing gesture
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()

                                        // *** FIX: Read current state *inside* the gesture lambda ***
                                        val currentState = buttonStates[index]
                                        val currentWidthPxLocal = with(density) { currentState.width.toPx() }
                                        val currentHeightPxLocal = with(density) { currentState.height.toPx() }

                                        // Calculate new size in Px based on the *latest* state
                                        val newWidthPx = currentWidthPxLocal + dragAmount.x
                                        val newHeightPx = currentHeightPxLocal + dragAmount.y

                                        // Convert new size to Dp and apply constraints
                                        val newWidthDp = with(density) { newWidthPx.toDp() }
                                            .coerceIn(minButtonWidth, maxButtonWidth)
                                        val newHeightDp = with(density) { newHeightPx.toDp() }
                                            .coerceIn(minButtonHeight, maxButtonHeight)

                                        // Update size state
                                        buttonStates[index] = currentState.copy(
                                            width = newWidthDp,
                                            height = newHeightDp
                                        )
                                    }
                                }
                        )
                    }
                }
            }

            // --- Lock/Unlock Toggle Button ---
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
// (Keep the implementation from the previous version)
@Composable
fun ExampleFreeFormLayoutUsage(onCommand: (Command) -> Unit) {
    val myInitialButtons = remember {
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
        initialButtons = myInitialButtons,
        onCommand = onCommand,
        modifier = Modifier.background(Color.Black.copy(alpha = 0.9f))
    )
}
