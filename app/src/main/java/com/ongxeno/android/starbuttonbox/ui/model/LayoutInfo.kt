package com.ongxeno.android.starbuttonbox.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.ongxeno.android.starbuttonbox.MainViewModel // Assuming ViewModel is passed
import com.ongxeno.android.starbuttonbox.data.LayoutType

/**
 * Represents a layout/tab ready for display in the UI.
 * Derived from LayoutDefinition, adding non-serializable UI elements.
 *
 * @param id Unique identifier for this layout.
 * @param title The display title.
 * @param icon The actual ImageVector icon for the UI.
 * @param type The type of the layout.
 * @param isEnabled Whether this layout should be shown as a tab.
 * @param isDeletable Whether this layout can be deleted in the management screen.
 * @param content The composable function that renders the content for this layout/tab.
 */
data class LayoutInfo(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val type: LayoutType, // Keep type info if needed for UI logic
    val isEnabled: Boolean,
    val isDeletable: Boolean, // Pass deletable status to UI
    val content: @Composable (viewModel: MainViewModel) -> Unit
)
