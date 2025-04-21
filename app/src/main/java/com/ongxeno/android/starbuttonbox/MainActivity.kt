package com.ongxeno.android.starbuttonbox

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.ongxeno.android.starbuttonbox.data.Command
import com.ongxeno.android.starbuttonbox.datasource.ConfigDatasource
import com.ongxeno.android.starbuttonbox.datasource.TabDatasource
import com.ongxeno.android.starbuttonbox.datasource.UdpSender
import com.ongxeno.android.starbuttonbox.ui.layout.PlaceholderLayout
import com.ongxeno.android.starbuttonbox.ui.setting.SettingsDialog
import com.ongxeno.android.starbuttonbox.ui.theme.StarButtonBoxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        hideSystemBars()

        setContent {
            HideSystemBarsEffect()
            StarButtonBoxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StarCitizenButtonBoxApp()
                }
            }
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}

/**
 * Composable effect to manage hiding system bars based on lifecycle events.
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

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}


@Composable
fun StarCitizenButtonBoxApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // --- Datasource Instantiation ---
    val configDatasource = remember { ConfigDatasource(context.applicationContext) }

    val targetNetworkConfig by configDatasource.networkConfigFlow.collectAsStateWithLifecycle(initialValue = null)

    var showSettingsDialog by remember { mutableStateOf(false) }

    // LaunchedEffect now keyed only on config values
    LaunchedEffect(targetNetworkConfig) {
        val (ip, port) = targetNetworkConfig ?: return@LaunchedEffect
        // Show dialog if config is null AND the dialog isn't already manually shown
        if (!showSettingsDialog && (ip == null || port == null)) {
            Log.d("StarCitizenButtonBoxApp", "Config missing, showing settings dialog automatically.")
            showSettingsDialog = true
        }
    }

    val udpSender: UdpSender? = remember(targetNetworkConfig) {
        targetNetworkConfig?.let { (ip, port) ->
            if (ip != null && port != null) {
                Log.d("UdpSender", "Creating/Updating UdpSender for $ip:$port")
                UdpSender(ip, port)
            } else {
                Log.d("UdpSender", "Config not ready, UdpSender is null")
                null
            }
        } ?: run {
            Log.d("UdpSender", "NetworkConfig is still loading")
            null
        }
    }

    // Command handler lambda - check if sender is available
    val handleCommand = { command: Command ->
        if (udpSender != null) {
            udpSender.sendCommandAction(command)
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Settings required", Toast.LENGTH_SHORT).show()
            }
            // Attempt to show settings dialog again if sender is null and dialog isn't already showing
            if (!showSettingsDialog) showSettingsDialog = true
        }
        Unit
    }

    // Tab Definitions and State
    val tabItems = remember { TabDatasource.getTabs() }
    var selectedTabIndex by remember { mutableStateOf(0) }

    // --- Main UI Structure ---
    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        // Top Bar (Tabs + Settings)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                .background(Color.DarkGray.copy(alpha = 0.5f))
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scrollable Tab Buttons
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabItems.forEachIndexed { index, tabInfo ->
                    IconButton(
                        onClick = { selectedTabIndex = index },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = tabInfo.icon,
                            contentDescription = tabInfo.title,
                            tint = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Settings Button
            IconButton(onClick = {
                showSettingsDialog = true
            }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }

        // Tab Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
        ) {
            if (selectedTabIndex >= 0 && selectedTabIndex < tabItems.size) {
                val selectedTab = tabItems[selectedTabIndex]
                selectedTab.content(handleCommand)
            } else {
                PlaceholderLayout("Error: Invalid Tab")
            }
        }
    }

    // --- Conditionally display the Settings Dialog ---
    if (showSettingsDialog) {
        SettingsDialog(
            onDismissRequest = {
                targetNetworkConfig?.let { (ip, port) ->
                    if (ip != null && port != null) {
                        showSettingsDialog = false
                    } else {
                        Toast.makeText(context, "Please save settings first", Toast.LENGTH_SHORT).show()
                    }
                } ?: Toast.makeText(context, "Loading Settings...", Toast.LENGTH_SHORT).show()
            },
            onSave = { ip, port ->
                scope.launch {
                    configDatasource.saveSettings(ip, port)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Settings Saved", Toast.LENGTH_SHORT).show()
                        showSettingsDialog = false
                    }
                }
            },
            networkConfigFlow = configDatasource.networkConfigFlow,
        )
    }
}
