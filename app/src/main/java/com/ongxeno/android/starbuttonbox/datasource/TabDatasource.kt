package com.ongxeno.android.starbuttonbox.datasource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ongxeno.android.starbuttonbox.ui.layout.DemoLayout
import com.ongxeno.android.starbuttonbox.ui.layout.FreeFormLayout
import com.ongxeno.android.starbuttonbox.ui.layout.NormalFlightLayout
import com.ongxeno.android.starbuttonbox.ui.layout.PlaceholderLayout
import com.ongxeno.android.starbuttonbox.ui.model.TabInfo
// Removed LayoutDatasource import, no longer needed here
// Removed LocalLayoutDatasource import

/**
 * Provides the list of TabInfo objects used for app navigation.
 * Layouts requiring commands now accept a lambda that takes a command identifier String.
 */
object TabDatasource {

    // Function signature simplified, no longer needs LayoutDatasource
    fun getTabs(): List<TabInfo> {
        return listOf(
            TabInfo(
                order = 0,
                title = "Normal Flight",
                icon = Icons.Filled.Rocket,
                // Pass the (String) -> Unit lambda
                content = { onCommand -> NormalFlightLayout(onCommand) }
            ),
            TabInfo(
                order = 1,
                title = "Free Form 1",
                icon = Icons.Filled.DashboardCustomize,
                // Content lambda now expects (String) -> Unit
                content = @Composable { onCommand ->
                    // LayoutDatasource is accessed via CompositionLocal inside FreeFormLayout
                    FreeFormLayout(
                        layoutId = "freeform_1",
                        onCommand = onCommand // Pass the (String) -> Unit lambda
                    )
                }
            ),
            TabInfo(
                order = 2,
                title = "Free Form 2",
                icon = Icons.Filled.DashboardCustomize,
                content = @Composable { onCommand ->
                    FreeFormLayout(
                        layoutId = "freeform_2",
                        onCommand = onCommand
                    )
                }
            ),
            TabInfo(
                order = 3,
                title = "Salvage",
                icon = Icons.Filled.Recycling,
                // Placeholder doesn't need onCommand
                content = { PlaceholderLayout("Salvage Layout Placeholder") }
            ),
            TabInfo(
                order = 4,
                title = "Mining",
                icon = Icons.Filled.Diamond,
                content = { PlaceholderLayout("Mining Layout Placeholder") }
            ),
            TabInfo(
                order = 5,
                title = "Combat",
                icon = Icons.Filled.LocalFireDepartment,
                content = { PlaceholderLayout("Combat Layout Placeholder") }
            ),
            TabInfo(
                order = 6,
                title = "Demo",
                icon = Icons.Filled.Widgets,
                // DemoLayout might not need onCommand, adjust if it does
                content = { DemoLayout() }
            )
        ).sortedBy { it.order }
    }
}
