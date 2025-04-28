package com.ongxeno.android.starbuttonbox.utils // Or a 'mapping' package

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline // Import specific auto-mirrored icon
import androidx.compose.material.icons.filled.* // Import all filled icons
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Singleton object responsible for mapping between Material Icon names (Strings)
 * and their corresponding ImageVector objects. Also provides the list of available icons.
 */
object IconMapper {

    // Define the canonical mapping from String name to ImageVector
    private val iconMap: Map<String, ImageVector> = mapOf(
        "DashboardCustomize" to Icons.Filled.DashboardCustomize,
        "Widgets" to Icons.Filled.Widgets,
        "Gamepad" to Icons.Filled.Gamepad,
        "Build" to Icons.Filled.Build,
        "SettingsInputComponent" to Icons.Filled.SettingsInputComponent,
        "Adjust" to Icons.Filled.Adjust,
        "Bolt" to Icons.Filled.Bolt,
        "Camera" to Icons.Filled.Camera,
        "Flag" to Icons.Filled.Flag,
        "Flight" to Icons.Filled.Flight,
        "Headset" to Icons.Filled.Headset,
        "Key" to Icons.Filled.Key,
        "Lightbulb" to Icons.Filled.Lightbulb,
        "Lock" to Icons.Filled.Lock,
        "Map" to Icons.Filled.Map,
        "Mic" to Icons.Filled.Mic,
        "Navigation" to Icons.Filled.Navigation,
        "Power" to Icons.Filled.Power,
        "Shield" to Icons.Filled.Shield,
        "Speed" to Icons.Filled.Speed,
        "Star" to Icons.Filled.Star,
        "Tune" to Icons.Filled.Tune,
        "Videocam" to Icons.Filled.Videocam,
        "Warning" to Icons.Filled.Warning,
        "WbSunny" to Icons.Filled.WbSunny,
        "Recycling" to Icons.Filled.Recycling, // Added from defaults
        "Diamond" to Icons.Filled.Diamond, // Added from defaults
        "LocalFireDepartment" to Icons.Filled.LocalFireDepartment, // Added from defaults
        "Rocket" to Icons.Filled.RocketLaunch, // Added from defaults (using RocketLaunch)
        "Construction" to Icons.Filled.Construction, // Added from defaults
        "HelpOutline" to Icons.AutoMirrored.Filled.HelpOutline // Use AutoMirrored version
        // Add any other icons you want to support here
    )

    // Provide the list of available icons for selection UI
    val availableIcons: List<ImageVector> = iconMap.values.toList()

    // Default icon name and vector
    private const val DEFAULT_ICON_NAME = "HelpOutline"
    private val DEFAULT_ICON_VECTOR = Icons.AutoMirrored.Filled.HelpOutline

    /**
     * Gets the ImageVector corresponding to a given icon name string.
     *
     * @param name The stored string name of the icon.
     * @return The matching ImageVector, or a default icon if the name is not found.
     */
    fun getIconVector(name: String?): ImageVector {
        return iconMap[name] ?: DEFAULT_ICON_VECTOR
    }

    /**
     * Gets the string name corresponding to a given ImageVector.
     * Used for saving the selected icon.
     *
     * @param icon The ImageVector selected by the user.
     * @return The matching string name, or a default name if the icon is not found in the map.
     */
    fun getIconName(icon: ImageVector): String {
        // Find the first entry where the value (ImageVector) matches the input icon
        return iconMap.entries.find { it.value == icon }?.key ?: DEFAULT_ICON_NAME
    }
}
