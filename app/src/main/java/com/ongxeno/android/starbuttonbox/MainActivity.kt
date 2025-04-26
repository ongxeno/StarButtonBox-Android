package com.ongxeno.android.starbuttonbox

import android.app.Activity // Keep import for casting check
import android.os.Bundle
import android.util.Log // Added import for Log
import android.view.WindowManager // Added import for WindowManager flags
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.* // Keep general runtime import
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // Keep LocalContext import
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle // Correct import for collecting flows
import com.ongxeno.android.starbuttonbox.ui.layout.PlaceholderLayout
import com.ongxeno.android.starbuttonbox.ui.model.TabInfo
// Import the new layout and the renamed dialog
import com.ongxeno.android.starbuttonbox.ui.setting.ConnectionConfigDialog
import com.ongxeno.android.starbuttonbox.ui.setting.SettingsLayout
import com.ongxeno.android.starbuttonbox.ui.theme.StarButtonBoxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Inject MainViewModel using Hilt
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge() // Enable drawing behind system bars
        hideSystemBars() // Initial hide of system bars

        setContent {
            // Composable effect to keep system bars hidden when the app resumes
            HideSystemBarsEffect()

            // --- Apply 'Keep Screen On' Setting ---
            // Collect the state from the ViewModel's StateFlow
            val keepScreenOn by viewModel.keepScreenOnState.collectAsStateWithLifecycle()
            // Get the current context
            val context = LocalContext.current
            // Use LaunchedEffect to apply/remove the window flag when the state changes or context is available
            // Cast context to Activity *inside* the effect where the window is needed.
            LaunchedEffect(keepScreenOn, context) {
                (context as? Activity)?.window?.let { window -> // Safely cast context and access window
                    if (keepScreenOn) {
                        // Add the flag to keep the screen on
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        Log.d("MainActivity", "Keep screen ON flag ADDED")
                    } else {
                        // Clear the flag to allow the screen to turn off
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        Log.d("MainActivity", "Keep screen ON flag CLEARED")
                    }
                } ?: Log.w("MainActivity", "Keep screen on: Could not get Activity window from context.")
            }
            // --- End Keep Screen On ---

            // Apply the app's theme
            StarButtonBoxTheme {
                // Main surface container
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background // Use theme background color
                ) {
                    // Render the main application UI, passing the ViewModel
                    StarCitizenButtonBoxApp(viewModel = viewModel)
                }
            }
        }
    }

    /**
     * Utility function to hide system bars (status bar, navigation bar) initially.
     * Sets the behavior to allow revealing them with a swipe.
     */
    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Allow transient bars to appear with swipe gestures
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide the actual system bars
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}

/**
 * A Composable effect that observes the activity lifecycle.
 * It ensures system bars are re-hidden when the activity resumes,
 * as they might become visible due to system interactions.
 */
@Composable
private fun HideSystemBarsEffect() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    DisposableEffect(lifecycleOwner) { // Re-run effect if lifecycleOwner changes
        // Get window and controller safely
        val window = (context as? ComponentActivity)?.window ?: return@DisposableEffect onDispose {}
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        // Create a lifecycle observer
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) { // When the activity resumes...
                // Re-apply the hide settings
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // Clean up: remove the observer when the composable leaves the composition
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}


/**
 * The main Composable function defining the application's UI structure.
 * It now uses a Box to potentially overlay the settings screen.
 *
 * @param viewModel The MainViewModel instance provided by Hilt.
 */
@Composable
fun StarCitizenButtonBoxApp(viewModel: MainViewModel) {
    // Collect necessary state from the ViewModel using collectAsStateWithLifecycle
    // This ensures the UI recomposes efficiently when state changes.
    val selectedTabIndex by viewModel.selectedTabIndexState.collectAsStateWithLifecycle()
    // Collect screen/dialog visibility states from the ViewModel's StateFlows
    val showSettingsScreen by viewModel.showSettingsScreenState.collectAsStateWithLifecycle()
    val showConnectionConfigDialog by viewModel.showConnectionConfigDialogState.collectAsStateWithLifecycle()

    // Get the list of tabs (assuming static for now)
    val tabItems = viewModel.tabItems

    // --- Main UI Structure: Box allows overlaying Settings Screen ---
    Box(modifier = Modifier.fillMaxSize()) {

        // --- Main Content Area (Tabs + Tab Content) ---
        // This Column contains the standard app UI (top bar with tabs, and the selected tab's content)
        Column(
            modifier = Modifier
                .fillMaxSize() // Take full size within the Box
                .background(Color.Black) // Set background for the main content area
        ) {
            // --- Top Bar: Contains scrollable tabs and the settings button ---
            Row(
                modifier = Modifier
                    .fillMaxWidth() // Span full width
                    // Apply padding for the status bar area (top inset)
                    .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                    .background(Color.DarkGray.copy(alpha = 0.5f)) // Semi-transparent background
                    .height(56.dp), // Fixed height for the top bar
                verticalAlignment = Alignment.CenterVertically // Align items vertically center
            ) {
                // --- Scrollable Row for Tabs ---
                Row(
                    modifier = Modifier
                        .weight(1f) // Take available horizontal space
                        .horizontalScroll(rememberScrollState()), // Allow horizontal scrolling if tabs overflow
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Iterate through tab items and create an IconButton for each
                    tabItems.forEachIndexed { index, tabInfo ->
                        IconButton(
                            onClick = { viewModel.selectTab(index) }, // Call ViewModel to change tab
                            modifier = Modifier.size(48.dp), // Standard touch target size
                            enabled = selectedTabIndex != null // Enable only when tab index is loaded
                        ) {
                            Icon(
                                imageVector = tabInfo.icon,
                                contentDescription = tabInfo.title,
                                // Tint icon based on selection state
                                tint = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                } // End Scrollable Tabs Row

                // --- Settings Button ---
                // This button now triggers the main settings *screen*
                IconButton(onClick = { viewModel.showSettingsScreen() }) {
                    Icon(Icons.Filled.Settings, "Settings", tint = Color.White) // White icon
                }
            } // End Top Bar Row

            // --- Tab Content Area ---
            // This Column holds the content of the currently selected tab
            Column(
                modifier = Modifier
                    .fillMaxSize() // Fill remaining space below the top bar
                    // Apply padding for navigation bars (bottom/horizontal insets)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            ) {
                // Display content based on the selected tab index
                selectedTabIndex?.let { indexValue ->
                    if (indexValue >= 0 && indexValue < tabItems.size) {
                        // Valid index: Get the selected tab info
                        val selectedTab = tabItems[indexValue]
                        // Call the content lambda associated with the tab, passing the ViewModel
                        selectedTab.content(viewModel)
                    } else {
                        // Handle invalid index (e.g., if tabs changed dynamically)
                        if (tabItems.isNotEmpty()) {
                            // Default to the first tab if index is invalid but tabs exist
                            LaunchedEffect(Unit) { viewModel.selectTab(0) } // Reset VM state
                            val firstTab = tabItems[0]
                            firstTab.content(viewModel) // Render first tab
                        } else {
                            // No tabs available: Show placeholder
                            PlaceholderLayout("No Tabs Available")
                        }
                    }
                } ?: run {
                    // Case: selectedTabIndex is null (still loading initial state)
                    // Show a loading indicator
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } // End Tab Content Column
        } // End Main Content Column

        // --- Settings Screen (Overlay) ---
        // Conditionally display the SettingsLayout on top of the main content
        // when showSettingsScreen state is true.
        if (showSettingsScreen) {
            Surface( // Use Surface for proper background and elevation handling
                modifier = Modifier.fillMaxSize(), // Cover the entire screen
                color = MaterialTheme.colorScheme.background // Use theme background
            ) {
                // Render the SettingsLayout composable, passing the ViewModel
                SettingsLayout(viewModel = viewModel)
            }
        }

        // --- Connection Config Dialog (Overlay on top of everything) ---
        // Conditionally display the ConnectionConfigDialog when its state is true.
        // This will appear over both the main content and the settings screen if both are active.
        if (showConnectionConfigDialog) {
            ConnectionConfigDialog(
                // Pass lambdas referencing ViewModel functions
                onDismissRequest = { ctx -> viewModel.hideConnectionConfigDialog(ctx) }, // Pass optional context
                onSave = { ip, port -> viewModel.saveConnectionSettings(ip, port) }, // Use renamed save function
                networkConfigFlow = viewModel.networkConfigState, // Provide the config flow
            )
        }

    } // End Outer Box
}
