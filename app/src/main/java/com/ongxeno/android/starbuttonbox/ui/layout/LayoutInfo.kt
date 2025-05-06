package com.ongxeno.android.starbuttonbox.ui.layout

import com.ongxeno.android.starbuttonbox.data.LayoutType

/**
 * Represents a layout/tab ready for display in the UI.
 * Derived from LayoutDefinition, adding non-serializable UI elements.
 *
 * @param id Unique identifier for this layout.
 * @param title The display title.
 * @param iconName The icon name that need to be mapped to an ImageVector icon for the UI.
 * @param type The type of the layout.
 * @param isEnabled Whether this layout should be shown as a tab.
 */
data class LayoutInfo(
    val id: String,
    val title: String,
    val iconName: String,
    val type: LayoutType,
    val isEnabled: Boolean = true,
)