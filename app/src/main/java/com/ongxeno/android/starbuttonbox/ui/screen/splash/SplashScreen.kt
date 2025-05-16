package com.ongxeno.android.starbuttonbox.ui.screen.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModelInterface = hiltViewModel<SplashViewModel>()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()
    val isInitializationComplete by viewModel.isInitializationComplete.collectAsState()

    val inPreviewMode = LocalInspectionMode.current

    val navigateToMain = {
        if (isFirstLaunch) {
            viewModel.markFirstLaunchCompletedInViewModel()
        }
        if (!inPreviewMode && navController.currentDestination?.route != AppScreenRoute.Main.route) {
            navController.navigate(AppScreenRoute.Main.route) {
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
                        painter = painterResource(id = R.drawable.ic_launcher_foreground), // <<< CHANGED HERE
                        contentDescription = "App Logo",
                        modifier = Modifier.size(128.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Initializing...", style = MaterialTheme.typography.bodyLarge)
                }
            } else if (isInitializationComplete) {
                if (isFirstLaunch) {
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
                                    navigateToMain()
                                }
                            ) {
                                Text("Skip for Now")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { /* No action for now */ },
                                enabled = false
                            ) {
                                Text("Start Setup (Soon)")
                            }
                        }
                    }
                } else {
                    // UI is blank here intentionally, as LaunchedEffect will navigate.
                    // You could show a brief "Loading Main Screen..." if desired.
                }
            }
        }
    }

    LaunchedEffect(isInitializationComplete, isFirstLaunch, inPreviewMode) {
        if (isInitializationComplete && !inPreviewMode) {
            if (!isFirstLaunch) {
                navigateToMain()
            } else {
            }
        }
    }
}

// --- Preview Composable Wrapper ---

class PreviewSplashViewModel(
    initialIsLoading: Boolean = true,
    initialIsFirstLaunch: Boolean = false,
    initialIsInitializationComplete: Boolean = false
) : SplashViewModelInterface {
    override val isLoading = MutableStateFlow(initialIsLoading)
    override val isFirstLaunch = MutableStateFlow(initialIsFirstLaunch)
    override val isInitializationComplete = MutableStateFlow(initialIsInitializationComplete)
    override fun markFirstLaunchCompletedInViewModel() { /* No-op for preview */ }
}

@Composable
private fun SplashScreenPreview(
    isLoading: Boolean,
    isFirstLaunch: Boolean,
    isInitializationComplete: Boolean
) {
    StarButtonBoxTheme { // Apply your app's theme for consistent previews
        SplashScreen(
            navController = rememberNavController(), // A mock NavController for preview
            viewModel = PreviewSplashViewModel(
                initialIsLoading = isLoading,
                initialIsFirstLaunch = isFirstLaunch,
                initialIsInitializationComplete = isInitializationComplete
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
        isFirstLaunch = false, // Can be false as loading UI is independent
        isInitializationComplete = false
    )
}

@Preview(name = "Splash Screen First Launch UI", showBackground = true)
@Composable
fun SplashScreenFirstLaunchPreview() {
    SplashScreenPreview(
        isLoading = false,
        isFirstLaunch = true,
        isInitializationComplete = true
    )
}