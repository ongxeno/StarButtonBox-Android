package com.ongxeno.android.starbuttonbox.data

import kotlinx.serialization.Serializable

/**
 * Serializable data class representing the persistent definition of a layout.
 * This is what gets stored in DataStore.
 *
 * @param id Unique identifier for the layout (e.g., "normal_flight", "freeform_1").
 * @param title Display title shown in the tab and management screen.
 * @param layoutType The type of layout, determining its content structure.
 * @param iconName String identifier for the Material Icon (e.g., "Rocket").
 * @param isEnabled Whether the layout is currently visible as a tab (used for hiding). Defaults to true.
 * @param isUserDefined Flag indicating if the layout was created by the user. Defaults to false.
 * @param isDeletable Flag indicating if the layout can be deleted by the user. Defaults to true for user-defined, false otherwise.
 * @param layoutItemsJson Serialized JSON string of `List<FreeFormItemState>`. Only used if `layoutType` is `FREE_FORM`. Null otherwise.
 */
@Serializable
data class LayoutDefinition(
    val id: String,
    val title: String,
    val layoutType: LayoutType,
    val iconName: String,
    val isEnabled: Boolean = true,
    val isUserDefined: Boolean = false,
    val isDeletable: Boolean = !isUserDefined, // Default based on isUserDefined
    val layoutItemsJson: String? = null // Nullable, only for FREE_FORM
)
