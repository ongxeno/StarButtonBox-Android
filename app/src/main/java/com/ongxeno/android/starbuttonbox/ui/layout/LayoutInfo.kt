package com.ongxeno.android.starbuttonbox.ui.layout

import com.ongxeno.android.starbuttonbox.data.LayoutType
import com.ongxeno.android.starbuttonbox.datasource.room.LayoutEntity

/**
 * Represents a layout/tab ready for display in the UI.
 * Derived from LayoutEntity.
 *
 * @param id Unique identifier for this layout.
 * @param title The display title.
 * @param iconName The icon name that needs to be mapped to an ImageVector icon for the UI.
 * @param type The type of the layout.
 * @param isEnabled Whether this layout should be shown as a tab.
 */
data class LayoutInfo(
    val id: String,
    val title: String,
    val iconName: String,
    val type: LayoutType, // Keep as LayoutType enum for UI logic
    val isEnabled: Boolean = true,
)

/**
 * Extension function to convert a LayoutEntity to a LayoutInfo.
 * Handles the conversion of layoutTypeString to the LayoutType enum.
 */
fun LayoutEntity.toLayoutInfo(): LayoutInfo {
    val layoutTypeEnum = try {
        LayoutType.valueOf(this.layoutTypeString)
    } catch (e: IllegalArgumentException) {
        // Fallback to a default or handle error appropriately
        // For example, log an error and use a placeholder type
        // Log.e("LayoutInfo", "Invalid layoutTypeString: ${this.layoutTypeString} for ID: ${this.id}")
        LayoutType.PLACEHOLDER // Or throw an exception if this case should not happen
    }
    return LayoutInfo(
        id = this.id,
        title = this.title,
        iconName = this.iconName,
        type = layoutTypeEnum,
        isEnabled = this.isEnabled
    )
}
