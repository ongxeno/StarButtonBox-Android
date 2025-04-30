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


