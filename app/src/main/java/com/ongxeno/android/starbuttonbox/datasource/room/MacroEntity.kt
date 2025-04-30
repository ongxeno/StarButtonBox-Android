package com.ongxeno.android.starbuttonbox.datasource.room // Updated package

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index // Import Index
import androidx.room.PrimaryKey
import com.ongxeno.android.starbuttonbox.data.InputAction // Ensure this import points to the correct data package
import java.util.UUID

/**
 * Represents a configurable macro (action) within a specific game,
 * defined as a Room database entity.
 *
 * Includes original XML identifiers for potential future mapping/updates.
 *
 * @property id A unique identifier for this macro instance. Serves as the primary key.
 * @property xmlCategoryName The original 'name' attribute from the <actionmap> tag in the XML. Indexed.
 * @property xmlActionName The original 'name' attribute from the <action> tag in the XML. Indexed.
 * @property label A short label suitable for display on a button.
 * @property title The user-facing name or label for the macro (e.g., "Spaceship Movement: Toggle Landing Gear").
 * @property description A brief explanation of what the macro does in the game.
 * @property gameId The ID of the game this macro belongs to. References Game.id. Indexed.
 * @property defaultInputAction The default key binding suggested by the app or game defaults. Nullable. Stored as JSON.
 * @property customInputAction The user's overridden key binding. Takes precedence over default. Nullable. Stored as JSON.
 * @property isUserCreated Flag indicating if this macro was predefined or created by the user.
 */
@Entity(
    tableName = "macros",
    // Add indices for faster lookups based on original XML names
    indices = [
        Index(value = ["game_id"]),
        Index(value = ["xmlCategoryName"]),
        Index(value = ["xmlActionName"]),
        Index(value = ["xmlCategoryName", "xmlActionName"], unique = true) // Ensure combination is unique
    ]
)
data class MacroEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    // Store original XML names for mapping
    @ColumnInfo(name = "xmlCategoryName")
    val xmlCategoryName: String, // Original <actionmap name="...">

    @ColumnInfo(name = "xmlActionName")
    val xmlActionName: String, // Original <action name="...">

    @ColumnInfo(name = "label") // Added label field
    val label: String,

    @ColumnInfo(name = "title")
    val title: String, // User-friendly generated title

    @ColumnInfo(name = "description")
    val description: String, // User-friendly generated description

    @ColumnInfo(name = "game_id") // Already indexed via @Entity annotation
    val gameId: String,

    @ColumnInfo(name = "default_input_action")
    val defaultInputAction: InputAction? = null, // Stored as JSON via TypeConverter

    @ColumnInfo(name = "custom_input_action")
    val customInputAction: InputAction? = null, // Stored as JSON via TypeConverter

    @ColumnInfo(name = "is_user_created")
    val isUserCreated: Boolean = false
) {
    val effectiveInputAction: InputAction?
        get() = customInputAction ?: defaultInputAction
}