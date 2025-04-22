@file:OptIn(InternalSerializationApi::class)

package com.ongxeno.android.starbuttonbox.data

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents different types of input actions that can be sent to the PC server.
 * Designed to be serialized to JSON using kotlinx.serialization.
 */
@Serializable
sealed interface InputAction {

    /**
     * Represents a keyboard key press action.
     * Refers to the top-level PressType.
     */
    @Serializable
    @SerialName("key_event")
    data class KeyEvent(
        val key: String,
        val modifiers: List<String> = emptyList(),
        val pressType: PressType = PressType.TAP
    ) : InputAction

    /**
     * Represents a mouse button click or hold action.
     * Now includes modifiers.
     * Secondary constructors removed as primary constructor defaults suffice.
     */
    @Serializable
    @SerialName("mouse_event")
    data class MouseEvent(
        val button: MouseButton,
        val modifiers: List<String> = emptyList(),
        val pressType: PressType = PressType.TAP
    ) : InputAction

    /**
     * Represents a mouse scroll wheel action.
     * Now includes modifiers.
     */
    @Serializable
    @SerialName("mouse_scroll")
    data class MouseScroll(
        val direction: Direction,
        val clicks: Int = 1,
        val modifiers: List<String> = emptyList()
    ) : InputAction {

        /**
         * Defines the direction for mouse scroll events.
         */
        @Serializable
        enum class Direction {
            UP,
            DOWN
        }
    }

}

/**
 * Defines the type of press action (tap or hold).
 * Used by KeyEvent and MouseEvent.
 */
@Serializable
sealed interface PressType {
    @Serializable
    @SerialName("tap")
    data object TAP : PressType

    @Serializable
    @SerialName("hold")
    data class HOLD(val durationMs: Long) : PressType
}

/**
 * Defines the mouse buttons, including the keyName for potential mapping.
 */
@Serializable
enum class MouseButton(val keyName: String) {
    LEFT("left"),
    RIGHT("right"),
    MIDDLE("middle")
}

/**
 * Defines common modifier key names compatible with pydirectinput.
 */
object ModifierKeys {
    const val SHIFT = "shift"
    const val SHIFT_LEFT = "shiftleft"
    const val SHIFT_RIGHT = "shiftright"
    const val ALT = "alt"
    const val ALT_LEFT = "altleft"
    const val ALT_RIGHT = "altright"
    const val CTRL = "ctrl"
    const val CTRL_LEFT = "ctrlleft"
    const val CTRL_RIGHT = "ctrlright"
    const val WIN_LEFT = "winleft"
    const val WIN_RIGHT = "winright"
}

