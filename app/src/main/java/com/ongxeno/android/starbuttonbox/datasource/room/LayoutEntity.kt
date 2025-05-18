package com.ongxeno.android.starbuttonbox.datasource.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents the core information for each layout.
 *
 * @param id Unique identifier for this layout. Serves as the primary key.
 * @param title The display title of the layout.
 * @param layoutTypeString The type of the layout (e.g., "NORMAL_FLIGHT", "FREE_FORM"). Stored as String.
 * @param iconName String identifier for the Material Icon (e.g., "Rocket").
 * @param isEnabled Whether the layout is currently visible as a tab.
 * @param isUserDefined Flag indicating if the layout was created by the user.
 * @param isDeletable Flag indicating if the layout can be deleted by the user.
 * @param orderIndex Integer defining the display order of the layout in lists/tabs.
 */
@Entity(
    tableName = "layouts",
    indices = [Index(value = ["order_index"])]
)
data class LayoutEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "layout_type")
    val layoutTypeString: String, // Store LayoutType.name() here

    @ColumnInfo(name = "icon_name")
    val iconName: String,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "is_user_defined")
    val isUserDefined: Boolean = false,

    @ColumnInfo(name = "is_deletable")
    val isDeletable: Boolean = !isUserDefined, // Default based on isUserDefined

    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0
)