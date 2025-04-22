@file:OptIn(InternalSerializationApi::class)

package com.ongxeno.android.starbuttonbox.data

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import java.util.UUID

// Enum to define the type of component in the layout
@Serializable
enum class FreeFormItemType {
    MOMENTARY_BUTTON
}

/**
 * Represents the state of a single item (e.g., a button) within a FreeFormLayout.
 * Designed to be serializable for persistence.
 *
 * @param id Unique identifier for this item instance.
 * @param type The type of UI component this item represents.
 * @param text The text label displayed (primarily for buttons).
 * @param commandString The unique identifier string of the Command associated with this item.
 * @param offsetX The horizontal position (offset from left).
 * @param offsetY The vertical position (offset from top).
 * @param widthDp The width of the item in Dp.
 * @param heightDp The height of the item in Dp.
 */
@Serializable
data class FreeFormItemState(
    val id: String = UUID.randomUUID().toString(),
    val type: FreeFormItemType = FreeFormItemType.MOMENTARY_BUTTON,
    val text: String = "",
    val commandString: String,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val widthDp: Float = 120f,
    val heightDp: Float = 50f
) {
    @kotlinx.serialization.Transient
    val offset: Offset
        get() = Offset(offsetX, offsetY)

    @kotlinx.serialization.Transient
    val width: Dp
        get() = Dp(widthDp)

    @kotlinx.serialization.Transient
    val height: Dp
        get() = Dp(heightDp)

    // Helper function to create a state with Offset and Dp
    companion object {
        fun from(
            text: String,
            command: Command, // Takes the Command object
            offset: Offset = Offset.Zero,
            width: Dp = 120.dp,
            height: Dp = 50.dp,
            type: FreeFormItemType = FreeFormItemType.MOMENTARY_BUTTON,
            id: String = UUID.randomUUID().toString()
        ): FreeFormItemState {
            return FreeFormItemState(
                id = id,
                type = type,
                text = text,
                // Store the unique commandString from the Command object
                commandString = command.commandString,
                offsetX = offset.x,
                offsetY = offset.y,
                widthDp = width.value,
                heightDp = height.value
            )
        }
    }
}
