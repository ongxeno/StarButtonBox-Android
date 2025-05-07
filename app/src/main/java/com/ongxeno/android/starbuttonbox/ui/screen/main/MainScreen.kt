package com.ongxeno.android.starbuttonbox.ui.screen.main

import androidx.compose.animation.AnimatedContent // Import AnimatedContent
import androidx.compose.animation.fadeIn // Import fadeIn
import androidx.compose.animation.fadeOut // Import fadeOut
import androidx.compose.animation.scaleIn // Import scaleIn
import androidx.compose.animation.scaleOut // Import scaleOut
import androidx.compose.animation.togetherWith // Import togetherWith (formerly with)
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudOff // For NO_CONFIG or CONNECTION_LOST
import androidx.compose.material.icons.filled.ErrorOutline // For CONNECTION_LOST
import androidx.compose.material.icons.filled.HourglassTop // For SENDING_PENDING_ACK
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi // For CONNECTED
import androidx.compose.material.icons.filled.WifiOff // For NO_CONFIG or CONNECTION_LOST
import androidx.compose.material.icons.filled.WifiTethering // For CONNECTING
// import androidx.compose.material.icons.filled.HelpOutline // Default for NO_CONFIG (Removed as WifiOff is clearer)
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.MainViewModel
import com.ongxeno.android.starbuttonbox.data.ConnectionStatus
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
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectedLayoutIndex by viewModel.selectedLayoutIndexState.collectAsStateWithLifecycle()
    val enabledLayouts by viewModel.enabledLayoutsState.collectAsStateWithLifecycle()
    val showConnectionConfigDialog by viewModel.showConnectionConfigDialogState.collectAsStateWithLifecycle()
    val showMainAddLayoutDialog by viewModel.showAddLayoutDialogState.collectAsStateWithLifecycle()

    // Collect connection status from MainViewModel
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background), // Use theme background
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background) // Use theme background
            ) {
                // --- Top Bar: Tabs, Connection Indicator, Add, Settings ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)) // Use a surface variant
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Scrollable Row for Tabs and Add Layout Button
                    Row(
                        modifier = Modifier
                            .weight(1f) // Takes available space before ConnectionIndicator and Settings
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Existing Layout Tabs
                        enabledLayouts.forEachIndexed { index, layoutInfo ->
                            IconButton(
                                onClick = { viewModel.selectLayout(index) },
                                modifier = Modifier.size(48.dp), // Standard touch target
                                enabled = selectedLayoutIndex >= 0 // Should always be true if layouts exist
                            ) {
                                Icon(
                                    imageVector = IconMapper.getIconVector(layoutInfo.iconName),
                                    contentDescription = layoutInfo.title,
                                    tint = if (selectedLayoutIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Add Layout Button - Moved inside the scrollable Row
                        IconButton(
                            onClick = { viewModel.requestAddLayout() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add New Layout",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant // Consistent tint
                            )
                        }
                    } // End Scrollable Tabs Row

                    // --- Connection Status Indicator ---
                    ConnectionStatusIndicator(status = connectionStatus)
                    // Spacer(modifier = Modifier.width(4.dp)) // Space can be adjusted or removed

                    // Settings Button
                    IconButton(
                        onClick = navigateToSettings,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp)) // Padding at the end if needed
                } // End Top Bar Row

                // Tab Content Area
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                ) {
                    val layoutInfo =
                        if (selectedLayoutIndex >= 0 && selectedLayoutIndex < enabledLayouts.size) {
                            enabledLayouts[selectedLayoutIndex]
                        } else if (enabledLayouts.isNotEmpty()) {
                            // If selected index is somehow out of bounds but layouts exist, select the first one
                            LaunchedEffect(enabledLayouts) { viewModel.selectLayout(0) }
                            enabledLayouts[0]
                        } else { null } // No layouts available

                    when (layoutInfo?.type) {
                        LayoutType.NORMAL_FLIGHT -> NormalFlightLayout(hiltViewModel())
                        LayoutType.FREE_FORM -> FreeFormLayout(viewModel, hiltViewModel())
                        LayoutType.DEMO -> DemoLayout()
                        LayoutType.PLACEHOLDER -> PlaceholderLayout("Layout: ${layoutInfo.id}")
                        else -> PlaceholderLayout("No Layouts Enabled") // Or a more welcoming empty state
                    }
                }
            }
        }

        // Connection Configuration Dialog
        if (showConnectionConfigDialog) {
            ConnectionConfigDialog(
                viewModel = hiltViewModel(), // Pass SettingViewModel
                onDismissRequest = { ctx -> viewModel.hideConnectionConfigDialog(ctx) },
                onSave = { ip, port -> viewModel.saveConnectionSettings(ip, port) },
                networkConfigFlow = viewModel.networkConfigState,
            )
        }

        // Add/Edit Layout Dialog (triggered from MainScreen's add button)
        if (showMainAddLayoutDialog) {
            AddEditLayoutDialog(
                layoutToEdit = null, // This is for adding a new layout
                onDismissRequest = { viewModel.cancelAddLayout() },
                onConfirm = { title, iconName, _ -> // Existing ID is null for new layouts
                    viewModel.confirmAddLayout(title, iconName)
                }
            )
        }
    }
}

@Composable
fun ConnectionStatusIndicator(status: ConnectionStatus) {
    val icon: ImageVector
    val tintColor = MaterialTheme.colorScheme.onSurfaceVariant
    val contentDescription: String

    when (status) {
        ConnectionStatus.NO_CONFIG -> {
            icon = Icons.Filled.WifiOff
            contentDescription = "No network configuration"
        }
        ConnectionStatus.CONNECTING -> {
            icon = Icons.Filled.WifiTethering
            contentDescription = "Connecting to server"
        }
        ConnectionStatus.CONNECTED -> {
            icon = Icons.Filled.Wifi
            contentDescription = "Connected to server"
        }
        ConnectionStatus.SENDING_PENDING_ACK -> {
            icon = Icons.Filled.HourglassTop
            contentDescription = "Sending data to server"
        }
        ConnectionStatus.CONNECTION_LOST -> {
            icon = Icons.Filled.ErrorOutline
            contentDescription = "Connection to server lost"
        }
    }

    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        // Use AnimatedContent to animate icon changes
        AnimatedContent(
            targetState = icon, // Animate when the icon itself changes
            transitionSpec = {
                // Define enter and exit transitions
                // Example: Fade in new icon, fade out old icon
                // You can also use scaleIn/scaleOut or slideIn/slideOut
                (fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) +
                        scaleIn(initialScale = 0.8f, animationSpec = androidx.compose.animation.core.tween(200)))
                    .togetherWith(
                        fadeOut(animationSpec = androidx.compose.animation.core.tween(200)) +
                                scaleOut(targetScale = 0.8f, animationSpec = androidx.compose.animation.core.tween(200))
                    )
            },
            label = "ConnectionStatusIconAnimation" // Label for tooling
        ) { targetIcon -> // The content lambda receives the target state (the icon)
            Icon(
                imageVector = targetIcon,
                contentDescription = contentDescription, // Content description should ideally also update if icon meaning changes drastically
                tint = tintColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
