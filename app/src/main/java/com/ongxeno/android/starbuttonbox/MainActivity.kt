package com.ongxeno.android.starbuttonbox

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.ui.dialog.ConnectionConfigDialog
import com.ongxeno.android.starbuttonbox.ui.layout.PlaceholderLayout
import com.ongxeno.android.starbuttonbox.ui.screen.ManageLayoutsScreen
import com.ongxeno.android.starbuttonbox.ui.screen.SettingsScreen
import com.ongxeno.android.starbuttonbox.ui.theme.StarButtonBoxTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideSystemBars()

        setContent {
            HideSystemBarsEffect()
            KeepScreenOnEffect(viewModel) // Pass viewModel to the effect

            StarButtonBoxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StarCitizenButtonBoxApp(viewModel = viewModel)
                }
            }
        }
    }

    private fun hideSystemBars() { /* ... (unchanged) ... */ }
}

@Composable
private fun HideSystemBarsEffect() { /* ... (unchanged) ... */ }

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


/**
 * The main Composable function defining the application's UI structure.
 * Uses LayoutRepository state and handles navigation between main content,
 * settings, and manage layouts screens.
 */
@Composable
fun StarCitizenButtonBoxApp(viewModel: MainViewModel) {
    // Collect loading state
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    // Collect state from ViewModel
    val selectedLayoutIndex by viewModel.selectedLayoutIndexState.collectAsStateWithLifecycle()
    // Use the flow of *enabled* layouts for the tab bar
    val enabledLayouts by viewModel.enabledLayoutsState.collectAsStateWithLifecycle()

    // Screen visibility states
    val showSettingsScreen by viewModel.showSettingsScreenState.collectAsStateWithLifecycle()
    val showManageLayoutsScreen by viewModel.showManageLayoutsScreenState.collectAsStateWithLifecycle()
    val showConnectionConfigDialog by viewModel.showConnectionConfigDialogState.collectAsStateWithLifecycle()

    // --- Main UI Structure: Box allows overlaying Settings/Manage Screens ---
    Box(modifier = Modifier.fillMaxSize()) {

        // Show loading indicator or main content based on isLoading state
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black), // Match background
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // --- Main Content Area (Tabs + Tab Content) ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // --- Top Bar: Contains scrollable tabs and the settings button ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                        .background(Color.DarkGray.copy(alpha = 0.5f))
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // --- Scrollable Row for Tabs (using enabledLayoutsState) ---
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Iterate through the list of *enabled* LayoutInfo objects
                        enabledLayouts.forEachIndexed { index, layoutInfo ->
                            IconButton(
                                onClick = { viewModel.selectLayout(index) }, // Use selectLayout
                                modifier = Modifier.size(48.dp),
                                // Enable button (selectedTabIndexState refers to index in enabled list)
                                enabled = selectedLayoutIndex >= 0
                            ) {
                                Icon(
                                    imageVector = layoutInfo.icon,
                                    contentDescription = layoutInfo.title,
                                    // Tint icon based on selection state
                                    tint = if (selectedLayoutIndex == index) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    } // End Scrollable Tabs Row

                    // --- Settings Button ---
                    IconButton(onClick = { viewModel.showSettingsScreen() }) { // Shows main settings
                        Icon(Icons.Filled.Settings, "Settings", tint = Color.White)
                    }
                } // End Top Bar Row

                // --- Tab Content Area ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                ) {
                    if (selectedLayoutIndex >= 0 && selectedLayoutIndex < enabledLayouts.size) {
                        val selectedLayout = enabledLayouts[selectedLayoutIndex]
                        selectedLayout.content(viewModel)
                    } else if (enabledLayouts.isNotEmpty()) {
                        LaunchedEffect(enabledLayouts) {
                            viewModel.selectLayout(0)
                        }
                        enabledLayouts[0].content(viewModel)
                    } else {
                        PlaceholderLayout("No Layouts Enabled")
                    }
                } // End Tab Content Column
            } // End Main Content Column
        }

        // --- Settings Screen (Overlay) ---
        if (showSettingsScreen) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                SettingsScreen(viewModel = viewModel)
            }
        }

        // --- Manage Layouts Screen (Overlay) ---
        if (showManageLayoutsScreen) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                ManageLayoutsScreen(viewModel = viewModel)
            }
        }


        // --- Connection Config Dialog (Overlay on top of everything) ---
        if (showConnectionConfigDialog) {
            ConnectionConfigDialog(
                onDismissRequest = { ctx -> viewModel.hideConnectionConfigDialog(ctx) },
                onSave = { ip, port -> viewModel.saveConnectionSettings(ip, port) },
                networkConfigFlow = viewModel.networkConfigState,
            )
        }

        // Note: DeleteConfirmationDialog is shown *within* ManageLayoutsScreen

    } // End Outer Box
}
