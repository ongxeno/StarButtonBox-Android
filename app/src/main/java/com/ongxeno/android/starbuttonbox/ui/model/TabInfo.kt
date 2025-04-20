package com.ongxeno.android.starbuttonbox.ui.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.ongxeno.android.starbuttonbox.data.Command

/**
 * Data class to hold information about each tab in the UI.
 *
 * @property order The display order of the tab (0-indexed).
 * @property title The title of the tab (used for accessibility).
 * @property icon The vector icon representing the tab.
 * @property content A composable lambda function that defines the UI content for this tab's screen.
 * It receives the command handler lambda as a parameter.
 */
data class TabInfo(
    val order: Int,
    val title: String,
    val icon: ImageVector,
    val content: @Composable (onCommand: (Command) -> Unit) -> Unit
)
