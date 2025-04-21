package com.ongxeno.android.starbuttonbox.datasource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Rocket
import com.ongxeno.android.starbuttonbox.ui.layout.NormalFlightLayout
import com.ongxeno.android.starbuttonbox.ui.layout.PlaceholderLayout
import com.ongxeno.android.starbuttonbox.ui.model.TabInfo

/**
 * Provides the list of TabInfo objects used for app navigation.
 */
object TabDatasource {

    fun getTabs(): List<TabInfo> {
        return listOf(
            TabInfo(
                order = 0,
                title = "Normal Flight",
                icon = Icons.Filled.Rocket,
                content = { onCommand -> NormalFlightLayout(onCommand) }
            ),
            TabInfo(
                order = 1,
                title = "Salvage",
                icon = Icons.Filled.Recycling,
                content = { PlaceholderLayout("Salvage Layout Placeholder") }
            ),
            TabInfo(
                order = 2,
                title = "Mining",
                icon = Icons.Filled.Diamond,
                content = { PlaceholderLayout("Mining Layout Placeholder") }
            ),
            TabInfo(
                order = 3,
                title = "Combat",
                icon = Icons.Filled.LocalFireDepartment,
                content = { PlaceholderLayout("Combat Layout Placeholder") }
            )
        )
    }
}
