package com.ongxeno.android.starbuttonbox

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ongxeno.android.starbuttonbox.data.Command
import com.ongxeno.android.starbuttonbox.datasource.UdpSender
import com.ongxeno.android.starbuttonbox.ui.layout.NormalFlightLayout
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
    val lifecycleOwner = LocalLifecycleOwner.current // Use platform version
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

    // --- Tab State ---
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Normal Flight", "Salvage", "Mining", "Combat")

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
                .background(Color.DarkGray.copy(alpha = 0.5f)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Scrollable Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.weight(1f),
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        // Use TabRowDefaults.Indicator directly
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }

            // Settings Button
            IconButton(onClick = {
                Toast.makeText(context, "Settings Clicked", Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurface
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
            // Content based on selected tab
            when (selectedTabIndex) {
                0 -> NormalFlightLayout(handleCommand) // Use imported composable
                1 -> PlaceholderLayout("Salvage Layout Placeholder")
                2 -> PlaceholderLayout("Mining Layout Placeholder")
                3 -> PlaceholderLayout("Combat Layout Placeholder")
            }
        }
    }
}

// NormalFlightLayout function definition removed from here

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
