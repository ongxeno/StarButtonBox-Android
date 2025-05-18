package com.ongxeno.android.starbuttonbox.ui.screen.splash

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.ongxeno.android.starbuttonbox.R
import com.ongxeno.android.starbuttonbox.navigation.AppScreenRoute
import com.ongxeno.android.starbuttonbox.ui.theme.StarButtonBoxTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Assuming SplashViewModelInterface is defined elsewhere and imported
// For example: import com.ongxeno.android.starbuttonbox.ui.SplashViewModelInterface

class PreviewSplashViewModel(
    initialIsLoading: Boolean = true,
    initialIsFirstLaunch: Boolean = false,
    initialIsInitializationComplete: Boolean = false,
    initialNavigateToSetup: Boolean = false
) : SplashViewModelInterface {
    override val isLoading = MutableStateFlow(initialIsLoading)
    override val isFirstLaunch = MutableStateFlow(initialIsFirstLaunch)
    override val isInitializationComplete = MutableStateFlow(initialIsInitializationComplete)
    override val navigateToSetupFlow = MutableStateFlow(initialNavigateToSetup)
    override fun markFirstLaunchCompletedInViewModel() { /* No-op for preview */ }
}

private const val SPLASH_SCREEN_TAG = "SplashScreen"

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModelInterface = hiltViewModel<SplashViewModel>()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState() // Still useful for markFirstLaunchCompleted
    val isInitializationComplete by viewModel.isInitializationComplete.collectAsState()
    val navigateToSetup by viewModel.navigateToSetupFlow.collectAsState()

    val inPreviewMode = LocalInspectionMode.current

    val performNavigation = { destinationRoute: String ->
        if (isFirstLaunch) { // Mark first launch completed when navigating away from splash
            viewModel.markFirstLaunchCompletedInViewModel()
        }
        if (!inPreviewMode && navController.currentDestination?.route != destinationRoute) {
            Log.i(SPLASH_SCREEN_TAG, "Navigating from Splash to $destinationRoute")
            navController.navigate(destinationRoute) {
                popUpTo(AppScreenRoute.Splash.route) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "App Logo",
                        modifier = Modifier.size(128.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Initializing...", style = MaterialTheme.typography.bodyLarge)
                }
            } else if (isInitializationComplete) {
                // The UI for "Welcome..." is shown if navigateToSetup is true.
                // Navigation is handled by LaunchedEffect or the "Skip" button.
                if (navigateToSetup) { // This implies it's a first launch AND network config is missing
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                    ) {
                        Text(
                            "Welcome to StarButtonBox!",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Initial setup is recommended for the best experience.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(
                                onClick = {
                                    // Skipping setup means going directly to Main.
                                    // markFirstLaunchCompleted will be called by performNavigation.
                                    performNavigation(AppScreenRoute.Main.route)
                                }
                            ) {
                                Text("Skip for Now")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    performNavigation(AppScreenRoute.SetupStartScreen.route)
                                }
                            ) {
                                Text("Start Setup")
                            }
                        }
                    }
                }
                // If navigateToSetup is false, LaunchedEffect will handle navigation to MainScreen.
                // If navigateToSetup is true and user does nothing on the UI above, LaunchedEffect will navigate to SetupStartScreen.
            }
        }
    }

    LaunchedEffect(isInitializationComplete, navigateToSetup, inPreviewMode) {
        if (isInitializationComplete && !inPreviewMode) {
            // If the "Welcome UI" for setup is being shown (because navigateToSetup is true),
            // we let user interaction (Skip or Start Setup buttons) handle navigation.
            // This LaunchedEffect will only navigate if that UI is *not* currently relevant
            // or if no interaction happens on it (leading to SetupStartScreen by default if navigateToSetup is true).
            if (navigateToSetup) {
                // This will be the default navigation if the user doesn't click "Skip"
                // from the "Welcome..." UI when navigateToSetup is true.
                // If the "Welcome..." UI is *not* shown at all when navigateToSetup is true,
                // this becomes the primary navigation to the setup flow.
                // Adding a small delay to allow the "Welcome" UI to be seen if shown.
                kotlinx.coroutines.delay(500) // Optional delay
                if (navController.currentDestination?.route == AppScreenRoute.Splash.route) { // Ensure we are still on splash
                    performNavigation(AppScreenRoute.SetupStartScreen.route)
                }
            } else {
                // Not a first launch needing setup OR (first launch with config already present).
                performNavigation(AppScreenRoute.Main.route)
            }
        }
    }
}

// --- Preview Composable Wrapper ---
@Composable
private fun SplashScreenPreview(
    isLoading: Boolean,
    isFirstLaunch: Boolean,
    isInitializationComplete: Boolean,
    navigateToSetup: Boolean
) {
    StarButtonBoxTheme {
        SplashScreen(
            navController = rememberNavController(),
            viewModel = PreviewSplashViewModel(
                initialIsLoading = isLoading,
                initialIsFirstLaunch = isFirstLaunch,
                initialIsInitializationComplete = isInitializationComplete,
                initialNavigateToSetup = navigateToSetup
            )
        )
    }
}

// --- Previews ---
@Preview(name = "Splash Screen Loading", showBackground = true)
@Composable
fun SplashScreenLoadingPreview() {
    SplashScreenPreview(
        isLoading = true,
        isFirstLaunch = false,
        isInitializationComplete = false,
        navigateToSetup = false
    )
}

@Preview(name = "Splash Screen First Launch UI (Needs Setup)", showBackground = true)
@Composable
fun SplashScreenFirstLaunchNeedsSetupPreview() {
    SplashScreenPreview(
        isLoading = false,
        isFirstLaunch = true,
        isInitializationComplete = true,
        navigateToSetup = true
    )
}

@Preview(name = "Splash Screen First Launch (Config OK)", showBackground = true)
@Composable
fun SplashScreenFirstLaunchConfigOkPreview() {
    SplashScreenPreview(
        isLoading = false,
        isFirstLaunch = true,
        isInitializationComplete = true,
        navigateToSetup = false
    )
}
