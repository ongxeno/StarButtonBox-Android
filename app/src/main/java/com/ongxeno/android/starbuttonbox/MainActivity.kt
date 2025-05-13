package com.ongxeno.android.starbuttonbox

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ongxeno.android.starbuttonbox.navigation.AppScreenRoute
import com.ongxeno.android.starbuttonbox.ui.screen.main.MainScreen
import com.ongxeno.android.starbuttonbox.ui.screen.managelayout.ManageLayoutsScreen
import com.ongxeno.android.starbuttonbox.ui.screen.setting.SettingViewModel
import com.ongxeno.android.starbuttonbox.ui.screen.setting.SettingsScreen
import com.ongxeno.android.starbuttonbox.ui.theme.StarButtonBoxTheme
import dagger.hilt.android.AndroidEntryPoint

private const val ANIMATION_DURATION_MS = 300

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KeepScreenOnEffect(viewModel) // Pass viewModel to the effect

            StarButtonBoxTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = AppScreenRoute.Main.route // Set the starting screen route
                ) {
                    composable(
                        route = AppScreenRoute.Main.route,
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth }, // Slide in from Left
                                animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)
                            )
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth }, // Slide out to Left
                                animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)
                            )
                        }
                    ) {
                        MainScreen(
                            viewModel = viewModel, navigateToSettings = {
                                navController.navigate(AppScreenRoute.Settings.route)
                            })
                    }
                    composable(
                        AppScreenRoute.Settings.route,
                        // Animation: When entering Settings FROM Main
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth }, // Slide in from Right
                                animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)
                            )
                        },
                        // Animation: When leaving Settings TO Main (Back)
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth }, // Slide out to Right
                                animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)
                            )
                        },
                        // Animation: When returning TO Settings from ManageLayouts/ManageMacros
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth }, // Slide in from Left
                                animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)
                            )
                        },
                        // Animation: When leaving Settings FOR ManageLayouts/ManageMacros
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth }, // Slide out to Left
                                animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)
                            )
                        }
                    ) {
                        val settingViewModel: SettingViewModel = hiltViewModel()
                        SettingsScreen(viewModel = settingViewModel, onNavigateToManageLayouts = {
                            navController.navigate(AppScreenRoute.ManageLayouts.route)
                        }, onNavigateToManageMacros = {
                            navController.navigate(AppScreenRoute.ManageMacros.route)
                        }, onNavigateBack = { navController.popBackStack() })
                    }
                    composable(
                        AppScreenRoute.ManageLayouts.route,
                        // Animation: When entering ManageLayouts FROM Settings
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth }, // Slide in from Right
                                animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)
                            )
                        },
                        // Animation: When leaving ManageLayouts TO Settings (Back)
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth }, // Slide out to Right
                                animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)
                            )
                        }
                    ) {
                        ManageLayoutsScreen(
                            onNavigateBack = { navController.popBackStack() })
                    }
                    composable(
                        AppScreenRoute.ManageMacros.route,
                        // Animation: When entering ManageMacros FROM Settings
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth }, // Slide in from Right
                                animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)
                            )
                        },
                        // Animation: When leaving ManageMacros TO Settings (Back)
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth }, // Slide out to Right
                                animationSpec = tween(durationMillis = ANIMATION_DURATION_MS)
                            )
                        }
                    ) {
                        com.ongxeno.android.starbuttonbox.ui.layout.PlaceholderLayout(title = "Manage Macros Screen (WIP)")
                    }
                }
            }
        }
    }
}

/**
 * Effect to apply the Keep Screen On flag based on ViewModel state.
 */
@Composable
private fun KeepScreenOnEffect(viewModel: MainViewModel) {
    val keepScreenOn by viewModel.keepScreenOnState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(keepScreenOn, context) {
        (context as? Activity)?.window?.let { window ->
            if (keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Log.d("KeepScreenOnEffect", "Keep screen ON flag ADDED")
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                Log.d("KeepScreenOnEffect", "Keep screen ON flag CLEARED")
            }
        } ?: Log.w("KeepScreenOnEffect", "Could not get Activity window from context.")
    }
}