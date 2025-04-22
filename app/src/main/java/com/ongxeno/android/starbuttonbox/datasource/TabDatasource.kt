package com.ongxeno.android.starbuttonbox.datasource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.runtime.Composable // Import Composable
import androidx.compose.ui.Modifier
import com.ongxeno.android.starbuttonbox.ui.layout.DemoLayout
import com.ongxeno.android.starbuttonbox.ui.layout.FreeFormLayout
import com.ongxeno.android.starbuttonbox.ui.layout.NormalFlightLayout
import com.ongxeno.android.starbuttonbox.ui.layout.PlaceholderLayout
import com.ongxeno.android.starbuttonbox.ui.model.TabInfo
import com.ongxeno.android.starbuttonbox.utils.LocalLayoutDatasource // Import CompositionLocal key

/**
 * Provides the list of TabInfo objects used for app navigation.
 * FreeFormLayout tabs will implicitly use the LayoutDatasource provided via CompositionLocal.
 */
object TabDatasource {

    // Function no longer requires LayoutDatasource instance as parameter
    fun getTabs(): List<TabInfo> {
        return listOf(
            TabInfo(
                order = 0,
                title = "Normal Flight",
                icon = Icons.Filled.Rocket,
                content = { onCommand -> NormalFlightLayout(onCommand) }
            ),
            // --- FreeFormLayout Tab 1 ---
            TabInfo(
                order = 1,
                title = "Free Form 1",
                icon = Icons.Filled.DashboardCustomize,
                // Content lambda now accesses the datasource via CompositionLocal
                content = @Composable { onCommand -> // Explicitly mark lambda as Composable
                    val layoutDatasource = LocalLayoutDatasource.current // Get datasource here
                    FreeFormLayout(
                        layoutId = "freeform_1", // Unique ID for this layout
                        // layoutDatasource parameter removed
                        onCommand = onCommand
                        // Optional modifier if needed
                    )
                }
            ),
            // --- FreeFormLayout Tab 2 ---
            TabInfo(
                order = 2,
                title = "Free Form 2",
                icon = Icons.Filled.DashboardCustomize,
                content = @Composable { onCommand -> // Explicitly mark lambda as Composable
                    val layoutDatasource = LocalLayoutDatasource.current // Get datasource here
                    FreeFormLayout(
                        layoutId = "freeform_2", // Different unique ID
                        // layoutDatasource parameter removed
                        onCommand = onCommand
                        // Optional modifier if needed
                    )
                }
            ),
            // --- Existing Placeholder Tabs (adjust order) ---
            TabInfo(
                order = 3,
                title = "Salvage",
                icon = Icons.Filled.Recycling,
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
            // --- Demo Tab (adjust order) ---
            TabInfo(
                order = 6,
                title = "Demo",
                icon = Icons.Filled.Widgets,
                content = { DemoLayout() }
            )
        ).sortedBy { it.order } // Ensure tabs are sorted by order
    }
}
