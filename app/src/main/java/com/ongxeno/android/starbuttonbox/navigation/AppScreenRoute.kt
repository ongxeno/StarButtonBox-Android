package com.ongxeno.android.starbuttonbox.navigation

import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * Defines the routes for different screens in the application using a sealed interface.
 * This provides a structured and type-safe way to manage navigation destinations.
 */
sealed interface AppScreenRoute {
    /** The string representation of the route used by the NavController. */
    val route: String

    /** Route for the Splash screen, shown on app launch for initialization. */
    data object Splash : AppScreenRoute {
        override val route = "splash"
    }

    /** Route for the main screen displaying button layouts. */
    data object Main : AppScreenRoute {
        override val route = "main"
    }

    /** Route for the initial setup flow. */
    data object SetupStartScreen : AppScreenRoute {
        override val route = "setup_start"
    }

    /** Route for the application settings screen. */
    data object Settings : AppScreenRoute {
        override val route = "settings"
    }

    /** Route for the screen to manage (add, edit, delete, reorder) layouts. */
    data object ManageLayouts : AppScreenRoute {
        override val route = "manage_layouts"
    }

    /** Route for the screen to manage (view, search, potentially edit/delete) macros. */
    data object ManageMacros : AppScreenRoute {
        override val route = "manage_macros"
    }
}
