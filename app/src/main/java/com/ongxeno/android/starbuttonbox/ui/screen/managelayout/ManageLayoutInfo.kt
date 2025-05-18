package com.ongxeno.android.starbuttonbox.ui.screen.managelayout

import com.ongxeno.android.starbuttonbox.data.LayoutType
import com.ongxeno.android.starbuttonbox.datasource.room.LayoutEntity

/**
 * Data class representing layout information specifically for the ManageLayoutsScreen.
 * Derived from LayoutEntity.
 *
 * @param id Unique identifier for this layout.
 * @param title The display title.
 * @param type The type of the layout.
 * @param iconName String identifier for the Material Icon.
 * @param isEnabled Whether the layout is currently enabled/visible.
 * @param isUserDefined Flag indicating if the layout was created by the user.
 * @param isDeletable Flag indicating if the layout can be deleted by the user.
 */
data class ManageLayoutInfo(
    val id: String,
    val title: String,
    val type: LayoutType, // Keep as LayoutType enum
    val iconName: String,
    val isEnabled: Boolean,
    val isUserDefined: Boolean,
    val isDeletable: Boolean,
)

/**
 * Extension function to convert a LayoutEntity to a ManageLayoutInfo.
 * Handles the conversion of layoutTypeString to the LayoutType enum.
 */
fun LayoutEntity.toManageLayoutInfo(): ManageLayoutInfo {
    val layoutTypeEnum = try {
        LayoutType.valueOf(this.layoutTypeString)
    } catch (e: IllegalArgumentException) {
        // Fallback for safety, though ideally layoutTypeString should always be valid
        // Log.e("ManageLayoutInfo", "Invalid layoutTypeString: ${this.layoutTypeString} for ID: ${this.id}")
        LayoutType.PLACEHOLDER
    }
    return ManageLayoutInfo(
        id = this.id,
        title = this.title,
        type = layoutTypeEnum,
        iconName = this.iconName,
        isEnabled = this.isEnabled,
        isUserDefined = this.isUserDefined,
        isDeletable = this.isDeletable
    )
}
