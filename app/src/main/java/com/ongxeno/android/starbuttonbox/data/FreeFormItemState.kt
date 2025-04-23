/*
 * File: StarButtonBox/app/src/main/java/com/ongxeno/android/starbuttonbox/data/FreeFormItemState.kt
 * Modified to store position and size in grid cell units.
 */
@file:OptIn(InternalSerializationApi::class)

package com.ongxeno.android.starbuttonbox.data

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class FreeFormItemType {
    MOMENTARY_BUTTON
}

/**
 * Represents the state of a single item within a FreeFormLayout.
 * Stores position and size in terms of grid cells.
 *
 * @param id Unique identifier.
 * @param type The type of UI component.
 * @param text The text label.
 * @param commandString The command identifier.
 * @param gridCol The starting column index (0-based) of the item.
 * @param gridRow The starting row index (0-based) of the item.
 * @param gridWidth The width of the item in number of grid columns (minimum 1).
 * @param gridHeight The height of the item in number of grid rows (minimum 1).
 * @param textSizeSp Optional custom text size (sp).
 * @param backgroundColorHex Optional custom background color hex.
 */
@Serializable
data class FreeFormItemState(
    val id: String = UUID.randomUUID().toString(),
    val type: FreeFormItemType = FreeFormItemType.MOMENTARY_BUTTON,
    val text: String = "",
    val commandString: String,
    // --- Grid-Based Position and Size ---
    val gridCol: Int = 1,       // Default starting column
    val gridRow: Int = 1,       // Default starting row
    val gridWidth: Int = 4,     // Default width in columns
    val gridHeight: Int = 2,    // Default height in rows
    // --- Customization ---
    val textSizeSp: Float? = null,
    val backgroundColorHex: String? = null
) {
    // Ensure dimensions are at least 1
    init {
        require(gridWidth >= 1) { "gridWidth must be at least 1" }
        require(gridHeight >= 1) { "gridHeight must be at least 1" }
    }
}
