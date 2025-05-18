package com.ongxeno.android.starbuttonbox.datasource.room // Ensure this package is correct

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
// Make sure LayoutEntity is imported if it's in a different sub-package,
// but assuming it's in the same 'room' package based on previous context.
// import com.ongxeno.android.starbuttonbox.datasource.room.LayoutEntity
import java.util.UUID

/**
 * Represents a single button or UI element within a specific layout.
 *
 * @param id Unique identifier for this button. Serves as the primary key.
 * @param layoutId Foreign key referencing the parent LayoutEntity's id.
 * @param gridCol The starting column index (0-based) of the button in the grid.
 * @param gridRow The starting row index (0-based) of the button in the grid.
 * @param gridWidth The width of the button in number of grid columns.
 * @param gridHeight The height of the button in number of grid rows.
 * @param buttonTypeString The type of button (e.g., "MOMENTARY_BUTTON"). Stored as String.
 * @param macroId Nullable ID of the macro to execute when this button is interacted with.
 * @param label The text displayed on the button.
 * @param labelSizeSp Nullable custom text size in scaled pixels (sp) for the button label.
 * @param backgroundColorHex Nullable custom background color as a hex string (e.g., "#RRGGBB").
 * @param orderInLayout Optional integer to define a specific order for buttons within a layout,
 * if grid position alone is not sufficient.
 */
@Entity(
    tableName = "buttons",
    foreignKeys = [
        ForeignKey(
            entity = LayoutEntity::class,
            parentColumns = ["id"],
            childColumns = ["layout_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["layout_id"])]
)
data class ButtonEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "layout_id")
    val layoutId: String,

    @ColumnInfo(name = "grid_col")
    val gridCol: Int,

    @ColumnInfo(name = "grid_row")
    val gridRow: Int,

    @ColumnInfo(name = "grid_width")
    val gridWidth: Int,

    @ColumnInfo(name = "grid_height")
    val gridHeight: Int,

    @ColumnInfo(name = "button_type")
    val buttonTypeString: String,

    @ColumnInfo(name = "macro_id")
    val macroId: String?,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "label_size_sp")
    val labelSizeSp: Float?,

    @ColumnInfo(name = "background_color_hex")
    val backgroundColorHex: String?,

    @ColumnInfo(name = "order_in_layout")
    val orderInLayout: Int = 0
)
