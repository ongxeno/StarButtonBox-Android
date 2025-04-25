/*
 * File: StarButtonBox/app/src/main/java/com/ongxeno/android/starbuttonbox/MainActivity.kt
 * Refactored to use Hilt for dependency injection and a MainViewModel.
 * Updated tab content call to pass ViewModel.
 */
package com.ongxeno.android.starbuttonbox

import android.os.Bundle
import android.widget.Toast // Keep for potential direct usage if needed outside VM
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels // Import for viewModels delegate
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
// Removed unused state imports: mutableStateOf, remember, rememberCoroutineScope, setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
// Removed datasource imports no longer needed here
import com.ongxeno.android.starbuttonbox.ui.layout.PlaceholderLayout // Keep for fallback
import com.ongxeno.android.starbuttonbox.ui.model.TabInfo // Keep for type usage
import com.ongxeno.android.starbuttonbox.ui.setting.SettingsDialog
import com.ongxeno.android.starbuttonbox.ui.theme.StarButtonBoxTheme
// Removed CompositionLocal imports and NestedCompositionLocalProvider
import dagger.hilt.android.AndroidEntryPoint // Import Hilt annotation

@AndroidEntryPoint // Enable Hilt injection for this Activity
class MainActivity : ComponentActivity() {

    // Inject MainViewModel using the Hilt viewModels delegate
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        hideSystemBars()

        setContent {
            HideSystemBarsEffect() // Apply the effect to hide system bars consistently

            StarButtonBoxTheme {
                // No need for NestedCompositionLocalProvider anymore for datasources/utils
                // Hilt handles dependency provision to the ViewModel.

                // Main Surface of the application
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Pass ViewModel state and event handlers to the main App composable
                    StarCitizenButtonBoxApp(viewModel = viewModel)
                }
            }
        }
    }

    // Utility function to initially hide system bars (remains the same)
    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}

/**
 * A Composable effect that observes the lifecycle and ensures system bars are hidden
 * when the activity resumes. (Remains the same)
 */
@Composable
private fun HideSystemBarsEffect() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    DisposableEffect(lifecycleOwner) {
        val window = (context as? ComponentActivity)?.window ?: return@DisposableEffect onDispose {}
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}


/**
 * The main Composable function that sets up the application's UI.
 * Now receives state and event handlers from the MainViewModel.
 *
 * @param viewModel The MainViewModel instance provided by Hilt.
 */
@Composable
fun StarCitizenButtonBoxApp(viewModel: MainViewModel) {
    // Get context for Toast messages passed to ViewModel
    val context = LocalContext.current.applicationContext

    // Collect state from ViewModel using collectAsStateWithLifecycle
    val targetNetworkConfig by viewModel.networkConfigState.collectAsStateWithLifecycle()
    val selectedTabIndex by viewModel.selectedTabIndexState.collectAsStateWithLifecycle()
    val showSettingsDialog by viewModel.showSettings // Collect simple state

    // Get tab items from ViewModel (assuming static for now)
    val tabItems = viewModel.tabItems

    // Command handler now calls the ViewModel's method
    val handleCommand = { commandIdentifier: String ->
        viewModel.sendCommand(commandIdentifier, context)
    }

    // --- Main UI Structure: Column layout ---
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // --- Top Bar: Contains tabs and settings button ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                .background(Color.DarkGray.copy(alpha = 0.5f))
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- Scrollable Tabs Row ---
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabItems.forEachIndexed { index, tabInfo ->
                    IconButton(
                        onClick = { viewModel.selectTab(index) }, // Call ViewModel to select tab
                        modifier = Modifier.size(48.dp),
                        enabled = selectedTabIndex != null // Enable only when index is loaded
                    ) {
                        Icon(
                            imageVector = tabInfo.icon,
                            contentDescription = tabInfo.title,
                            tint = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            // --- Settings Button ---
            IconButton(onClick = { viewModel.showSettingsDialog() }) { // Call VM to show settings
                Icon(Icons.Filled.Settings, "Settings", tint = Color.White)
            }
        }

        // --- Tab Content Area ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
        ) {
            // Display content based on the selected tab index from ViewModel state
            selectedTabIndex?.let { indexValue ->
                if (indexValue >= 0 && indexValue < tabItems.size) {
                    val selectedTab = tabItems[indexValue]
                    // Call the content lambda, passing BOTH handleCommand and viewModel
                    selectedTab.content(viewModel)
                } else {
                    // Handle invalid index (e.g., if tabs changed)
                    if (tabItems.isNotEmpty()) {
                        // Default to the first tab if index is invalid but tabs exist
                        LaunchedEffect(Unit) {
                            viewModel.selectTab(0) // Ensure VM state is consistent
                        }
                        // Render first tab based on potentially updated state
                        val firstTab = tabItems[0]
                        // Call the content lambda, passing BOTH handleCommand and viewModel
                        firstTab.content(viewModel)

                    } else {
                        PlaceholderLayout("No Tabs Available")
                    }
                }
            } ?: run {
                // Case: selectedTabIndex is null (still loading)
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    // --- Settings Dialog ---
    // Show the dialog conditionally based on the ViewModel state
    if (showSettingsDialog) {
        SettingsDialog(
            // Pass lambdas referencing ViewModel functions
            onDismissRequest = { viewModel.hideSettingsDialog(context) },
            onSave = { ip, port -> viewModel.saveSettings(ip, port) },
            // Pass the network config flow directly from the ViewModel state
            networkConfigFlow = viewModel.networkConfigState,
        )
    }
}
