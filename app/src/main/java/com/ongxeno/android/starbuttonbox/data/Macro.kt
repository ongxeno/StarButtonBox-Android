package com.ongxeno.android.starbuttonbox.data

import com.ongxeno.android.starbuttonbox.datasource.room.MacroEntity
import kotlinx.serialization.Serializable
import java.util.UUID // Import UUID

/**
 * Represents a configurable macro (action) within a specific game.
 *
 * @property id A unique identifier for this macro instance.
 * @property title The user-facing name or label for the macro (e.g., "Toggle Landing Gear").
 * @property description A brief explanation of what the macro does in the game.
 * @property gameId The ID of the game this macro belongs to. References Game.id.
 * @property defaultInputAction The default key binding suggested by the app or game defaults. Nullable.
 * @property customInputAction The user's overridden key binding. Takes precedence over default. Nullable.
 * @property isUserCreated Flag indicating if this macro was predefined or created by the user.
 */
@Serializable
data class Macro(
    val id: String,
    val title: String,
    val label: String,
    val description: String,
    val gameId: String,
    val defaultInputAction: InputAction? = null,
    val customInputAction: InputAction? = null,
    val isUserCreated: Boolean = false
) {
    /**
     * Gets the effective InputAction to be used, prioritizing custom over default.
     */
    val effectiveInputAction: InputAction?
        get() = customInputAction ?: defaultInputAction
}

fun MacroEntity.toUi() = Macro(
    id = this.id,
    title = this.title,
    label = this.label,
    description = this.description,
    gameId = this.gameId,
    defaultInputAction = this.defaultInputAction,
    customInputAction = this.customInputAction,
    isUserCreated = this.isUserCreated
)

/**
 * Converts an InputAction to a simplified, human-readable string representation.
 * Example outputs:
 * - "Key: A (Tap)"
 * - "Ctrl + Shift + Key: B (Hold 500ms)"
 * - "Mouse: LEFT (Tap)"
 * - "Alt + Mouse: RIGHT (Hold 1s)"
 * - "Scroll: UP (3 clicks)"
 * - "Shift + Scroll: DOWN (1 click)"
 * - "Not Set" if inputAction is null.
 */
fun InputAction?.toSimplifiedString(): String {
    if (this == null) return "Not Set"

    val parts = mutableListOf<String>()
    val pressTypeString: String

    when (this) {
        is InputAction.KeyEvent -> {
            if (this.modifiers.isNotEmpty()) {
                parts.add(this.modifiers.joinToString(" + ") { it.toModifierSymbol() })
            }
            parts.add("Key: ${this.key.uppercase()}")
            pressTypeString = when (val pt = this.pressType) {
                is PressType.TAP -> "(Tap)"
                is PressType.HOLD -> "(Hold ${formatDuration(pt.durationMs)})"
            }
        }
        is InputAction.MouseEvent -> {
            if (this.modifiers.isNotEmpty()) {
                parts.add(this.modifiers.joinToString(" + ") { it.toModifierSymbol() })
            }
            parts.add("Mouse: ${this.button.name}")
            pressTypeString = when (val pt = this.pressType) {
                is PressType.TAP -> "(Tap)"
                is PressType.HOLD -> "(Hold ${formatDuration(pt.durationMs)})"
            }
        }
        is InputAction.MouseScroll -> {
            if (this.modifiers.isNotEmpty()) {
                parts.add(this.modifiers.joinToString(" + ") { it.toModifierSymbol() })
            }
            parts.add("Scroll: ${this.direction.name}")
            pressTypeString = "(${this.clicks} click${if (this.clicks > 1) "s" else ""})"
        }
    }
    return parts.joinToString(" + ") + " " + pressTypeString
}

private fun String.toModifierSymbol(): String {
    return when (this.lowercase()) {
        ModifierKeys.SHIFT, ModifierKeys.SHIFT_LEFT, ModifierKeys.SHIFT_RIGHT -> "Shift"
        ModifierKeys.ALT, ModifierKeys.ALT_LEFT, ModifierKeys.ALT_RIGHT -> "Alt"
        ModifierKeys.CTRL, ModifierKeys.CTRL_LEFT, ModifierKeys.CTRL_RIGHT -> "Ctrl"
        ModifierKeys.WIN_LEFT, ModifierKeys.WIN_RIGHT -> "Win"
        else -> this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "Tap" // Or handle as error/default
    if (ms >= 1000) {
        val seconds = ms / 1000.0
        // Format to 1 decimal place if it's not a whole number, otherwise show as integer
        return if (seconds % 1 == 0.0) "${seconds.toInt()}s" else String.format("%.1fs", seconds)
    }
    return "${ms}ms"
}


