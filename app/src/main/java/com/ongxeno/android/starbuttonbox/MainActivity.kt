package com.ongxeno.android.starbuttonbox

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ongxeno.android.starbuttonbox.data.Command
import com.ongxeno.android.starbuttonbox.datasource.UdpSender
import com.ongxeno.android.starbuttonbox.ui.layout.NormalFlightLayout
import com.ongxeno.android.starbuttonbox.ui.model.TabInfo
import com.ongxeno.android.starbuttonbox.ui.theme.StarButtonBoxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        // Initial hide, effect will manage re-hiding on resume
        hideSystemBars()

        setContent {
            HideSystemBarsEffect() // Apply immersive effect
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

    // Helper to initially hide system bars
    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}

/**
 * Composable effect to manage hiding system bars based on lifecycle.
 */
@Composable
private fun HideSystemBarsEffect() {
    // Use the correct LocalLifecycleOwner from lifecycle-runtime-compose
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

        // Apply initial state when effect runs
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

    // --- Network Configuration ---
    val targetIpAddress = "192.168.50.102" // TODO: Make configurable?
    val targetPort = 5005

    // Remember UdpSender instance
    val udpSender = remember {
        UdpSender(targetIpAddress, targetPort)
    }

    // Command handler lambda
    val handleCommand = { command: Command ->
        udpSender.sendCommandAction(command)
        // UI Feedback (Toast)
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Sent: ${command.commandString}", Toast.LENGTH_SHORT).apply {
                show()
                Handler(Looper.getMainLooper()).postDelayed({ this.cancel() }, 500)
            }
        }
        Unit
    }

    // --- Define Tabs using TabInfo ---
    val tabItems = remember { // Remember the list to avoid recreation
        listOf(
            TabInfo(
                order = 0,
                title = "Normal Flight",
                icon = Icons.Filled.Flight,
                content = { onCommand -> NormalFlightLayout(onCommand) }
            ),
            TabInfo(
                order = 1,
                title = "Salvage",
                icon = Icons.Filled.Recycling,
                content = { PlaceholderLayout("Salvage Layout Placeholder") } // Pass placeholder directly
            ),
            TabInfo(
                order = 2,
                title = "Mining",
                icon = Icons.Filled.Diamond,
                content = { PlaceholderLayout("Mining Layout Placeholder") }
            ),
            TabInfo(
                order = 3,
                title = "Combat",
                icon = Icons.Filled.MyLocation,
                content = { PlaceholderLayout("Combat Layout Placeholder") }
            )
        )
    }

    // --- Tab State ---
    var selectedTabIndex by remember { mutableStateOf(0) }

    // --- Main UI Structure ---
    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) {
        // Top Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                .padding(start = 8.dp, end = 8.dp)
                .background(Color.DarkGray.copy(alpha = 0.5f))
                // Ensure the Row has a defined height, e.g., 56.dp
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scrollable Tabs - Use Row instead of ScrollableTabRow for IconButtons
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()) // Make the Row scrollable
            ) {
                // Iterate through the TabInfo list
                tabItems.forEachIndexed { index, tabInfo ->
                    // Use IconButton for better size control
                    IconButton(
                        onClick = { selectedTabIndex = index },
                        modifier = Modifier.size(72.dp) // Control size directly
                    ) {
                        Icon(
                            imageVector = tabInfo.icon,
                            contentDescription = tabInfo.title,
                            // Change tint based on selection
                            tint = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary
                            else Color.White
                        )
                    }
                }
            }


            // Settings Button
            IconButton(onClick = {
                Toast.makeText(context, "Settings Clicked", Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }

        // Content Area Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Display content based on selected tab's Composable lambda
            if (selectedTabIndex >= 0 && selectedTabIndex < tabItems.size) {
                val selectedTab = tabItems[selectedTabIndex]
                selectedTab.content(handleCommand) // Invoke the content lambda
            } else {
                PlaceholderLayout("Error: Invalid Tab")
            }
        }
    }
}

// PlaceholderLayout remains the same
@Composable
fun PlaceholderLayout(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
    }
}
