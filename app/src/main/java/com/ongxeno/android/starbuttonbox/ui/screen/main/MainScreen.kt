package com.ongxeno.android.starbuttonbox.ui.screen.main

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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.MainViewModel
import com.ongxeno.android.starbuttonbox.data.LayoutType
import com.ongxeno.android.starbuttonbox.ui.dialog.AddEditLayoutDialog
import com.ongxeno.android.starbuttonbox.ui.dialog.ConnectionConfigDialog
import com.ongxeno.android.starbuttonbox.ui.layout.DemoLayout
import com.ongxeno.android.starbuttonbox.ui.layout.FreeFormLayout
import com.ongxeno.android.starbuttonbox.ui.layout.NormalFlightLayout
import com.ongxeno.android.starbuttonbox.ui.layout.PlaceholderLayout
import com.ongxeno.android.starbuttonbox.utils.IconMapper

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    navigateToSettings: () -> Unit,
) {
    // Collect loading state
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    // Collect state from ViewModel
    val selectedLayoutIndex by viewModel.selectedLayoutIndexState.collectAsStateWithLifecycle()
    // Use the flow of *enabled* layouts for the tab bar
    val enabledLayouts by viewModel.enabledLayoutsState.collectAsStateWithLifecycle()

    // Screen visibility states
    val showConnectionConfigDialog by viewModel.showConnectionConfigDialogState.collectAsStateWithLifecycle()
    val showMainAddLayoutDialog by viewModel.showAddLayoutDialogState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        // Show loading indicator or main content based on isLoading state
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black), // Match background
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
                            .weight(1f) // Take available space, pushing Add/Settings to the right
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Existing Layout Tabs
                        enabledLayouts.forEachIndexed { index, layoutInfo ->
                            IconButton(
                                onClick = { viewModel.selectLayout(index) },
                                modifier = Modifier.size(48.dp),
                                enabled = selectedLayoutIndex >= 0
                            ) {
                                Icon(
                                    imageVector = IconMapper.getIconVector(layoutInfo.iconName),
                                    contentDescription = layoutInfo.title,
                                    tint = if (selectedLayoutIndex == index) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                        // Add Layout Button at the end of the tabs
                        IconButton(
                            onClick = { viewModel.requestAddLayout() }, // Show AddLayoutDialog
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add New Layout",
                                tint = Color.White.copy(alpha = 0.7f) // Consistent tint
                            )
                        }
                    } // End Scrollable Tabs Row

                    // --- Settings Button ---
                    IconButton(onClick = navigateToSettings) { // Shows main settings
                        Icon(Icons.Filled.Settings, "Settings", tint = Color.White)
                    }
                } // End Top Bar Row

                // --- Tab Content Area ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                ) {
                    val layoutInfo =
                        if (selectedLayoutIndex >= 0 && selectedLayoutIndex < enabledLayouts.size) {
                            enabledLayouts[selectedLayoutIndex]
                    } else if (enabledLayouts.isNotEmpty()) {
                        LaunchedEffect(enabledLayouts) {
                            viewModel.selectLayout(0)
                        }
                            enabledLayouts[0]
                    } else {
                            null
                    }

                    when (layoutInfo?.type) {
                        LayoutType.NORMAL_FLIGHT -> NormalFlightLayout(hiltViewModel())
                        LayoutType.FREE_FORM -> {
                            FreeFormLayout(viewModel, hiltViewModel())
                        }

                        LayoutType.DEMO -> DemoLayout()
                        LayoutType.PLACEHOLDER -> PlaceholderLayout("Layout: ${layoutInfo.id}")
                        else -> PlaceholderLayout("No Layouts Enabled")
                    }
                }
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

        // --- Add Layout Dialog (Overlay) ---
        if (showMainAddLayoutDialog) { // Use the state from MainViewModel
            AddEditLayoutDialog(
                layoutToEdit = null, // Always adding from here
                onDismissRequest = { viewModel.cancelAddLayout() }, // Use correct cancel function
                onConfirm = { title, iconName, _ -> // Ignore existingId
                    viewModel.confirmAddLayout(title, iconName) // Use correct confirm function
                }
            )
        }
    }
}