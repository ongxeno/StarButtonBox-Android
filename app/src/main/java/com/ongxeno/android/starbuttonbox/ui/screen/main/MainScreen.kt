package com.ongxeno.android.starbuttonbox.ui.screen.main

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.MainViewModel
import com.ongxeno.android.starbuttonbox.data.ConnectionStatus
import com.ongxeno.android.starbuttonbox.data.LayoutType
import com.ongxeno.android.starbuttonbox.ui.SendMacroViewModel
import com.ongxeno.android.starbuttonbox.ui.dialog.AddEditLayoutDialog
import com.ongxeno.android.starbuttonbox.ui.dialog.ConnectionConfigDialog
import com.ongxeno.android.starbuttonbox.ui.layout.AutoDragAndDropLayout
import com.ongxeno.android.starbuttonbox.ui.layout.DemoLayout
import com.ongxeno.android.starbuttonbox.ui.layout.FreeFormLayout
import com.ongxeno.android.starbuttonbox.ui.layout.LayoutInfo
import com.ongxeno.android.starbuttonbox.ui.layout.NormalFlightLayout
import com.ongxeno.android.starbuttonbox.ui.layout.PlaceholderLayout
import com.ongxeno.android.starbuttonbox.utils.IconMapper

fun Modifier.nonInteractive(): Modifier = this.pointerInput(Unit) {}

@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    navigateToSettings: () -> Unit,
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val selectedLayoutIndex by viewModel.selectedLayoutIndexState.collectAsStateWithLifecycle()
    val enabledLayouts by viewModel.enabledLayoutsState.collectAsStateWithLifecycle()
    val cachedLayoutsToRender by viewModel.cachedLayoutsToRenderState.collectAsStateWithLifecycle()
    val currentSelectedLayoutId by viewModel.selectedLayoutIdFlow.collectAsStateWithLifecycle(initialValue = null)

    val showConnectionConfigDialog by viewModel.showConnectionConfigDialogState.collectAsStateWithLifecycle()
    val showMainAddLayoutDialog by viewModel.showAddLayoutDialogState.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val latestResponseTimeMs by viewModel.latestResponseTimeMs.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        enabledLayouts.forEachIndexed { index, layoutInfo ->
                            IconButton(
                                onClick = { viewModel.selectLayout(index) },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    imageVector = IconMapper.getIconVector(layoutInfo.iconName),
                                    contentDescription = layoutInfo.title,
                                    tint = if (selectedLayoutIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.requestAddLayout() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Filled.Add, "Add New Layout", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    ResponseTimeIndicator(responseTimeMs = latestResponseTimeMs, connectionStatus = connectionStatus)
                    ConnectionStatusIndicator(status = connectionStatus)
                    IconButton(onClick = navigateToSettings, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.Settings, "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                ) {
                    if (cachedLayoutsToRender.isEmpty() && enabledLayouts.isNotEmpty()) {
                        val firstEnabledLayout = enabledLayouts.firstOrNull()
                        if (firstEnabledLayout != null) {
                            Log.d("MainScreen", "CachedLayoutsToRender is empty, but enabledLayouts exist. Rendering first enabled: ${firstEnabledLayout.title}")
                            ActualLayoutContent(layoutInfo = firstEnabledLayout, mainViewModel = viewModel)
                        } else {
                            PlaceholderLayout("No layouts available or selected.")
                        }
                    } else if (cachedLayoutsToRender.isEmpty() && enabledLayouts.isEmpty()) {
                         PlaceholderLayout("No layouts configured. Add one via Settings.")
                    }
                    else {
                        cachedLayoutsToRender.forEach { layoutInfo ->
                            key(layoutInfo.id) {
                                val isActuallyVisible = layoutInfo.id == currentSelectedLayoutId
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer { alpha = if (isActuallyVisible) 1f else 0f }
                                        .zIndex(if (isActuallyVisible) 1f else 0f)
                                        .then(if (!isActuallyVisible) Modifier.nonInteractive() else Modifier)
                                ) {
                                    ActualLayoutContent(layoutInfo = layoutInfo, mainViewModel = viewModel)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showConnectionConfigDialog) {
            ConnectionConfigDialog(
                viewModel = hiltViewModel(),
                onDismissRequest = { ctx -> viewModel.hideConnectionConfigDialog(ctx) },
                onSave = { ip, port -> viewModel.saveConnectionSettings(ip, port) },
                networkConfigFlow = viewModel.networkConfigState,
            )
        }

        if (showMainAddLayoutDialog) {
            AddEditLayoutDialog(
                layoutToEdit = null,
                onDismissRequest = { viewModel.cancelAddLayout() },
                onConfirm = { title, iconName, _ -> viewModel.confirmAddLayout(title, iconName) }
            )
        }
    }
}

@Composable
private fun ActualLayoutContent(layoutInfo: LayoutInfo, mainViewModel: MainViewModel) {
    when (layoutInfo.type) {
        LayoutType.NORMAL_FLIGHT -> NormalFlightLayout(hiltViewModel())
        LayoutType.FREE_FORM -> {
            FreeFormLayout(
                viewModel = mainViewModel,
                sendMacroViewModel = hiltViewModel()
            )
        }
        LayoutType.DEMO -> DemoLayout()
        LayoutType.AUTO_DRAG_AND_DROP -> AutoDragAndDropLayout(hiltViewModel())
        LayoutType.PLACEHOLDER -> PlaceholderLayout("Layout: ${layoutInfo.title}")
    }
}

@SuppressLint("UnusedContentLambdaTargetStateParameter")
@Composable
fun ResponseTimeIndicator(responseTimeMs: Long?, connectionStatus: ConnectionStatus) {
    val tintColor = MaterialTheme.colorScheme.onSurfaceVariant
    val placeholderText = "--"
    val unitText = "ms"
    val displayValue: String
    val displayUnit: String

    when {
        connectionStatus == ConnectionStatus.NO_CONFIG || connectionStatus == ConnectionStatus.CONNECTION_LOST -> {
            displayValue = placeholderText; displayUnit = ""
        }
        responseTimeMs != null -> { displayValue = responseTimeMs.toString(); displayUnit = unitText }
        else -> { displayValue = placeholderText; displayUnit = "" }
    }

    Box(modifier = Modifier.size(width = 56.dp, height = 48.dp).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(displayValue, color = tintColor, fontSize = 14.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, lineHeight = 16.sp)
            if (displayUnit.isNotEmpty()) {
                Text(displayUnit, color = tintColor, fontSize = 10.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 8.sp)
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ConnectionStatusIndicator(status: ConnectionStatus) {
    val icon: ImageVector
    val tintColor = MaterialTheme.colorScheme.onSurfaceVariant
    val contentDescription: String

    when (status) {
        ConnectionStatus.NO_CONFIG -> { icon = Icons.Filled.WifiOff; contentDescription = "No network configuration" }
        ConnectionStatus.CONNECTING -> { icon = Icons.Filled.WifiTethering; contentDescription = "Connecting to server" }
        ConnectionStatus.CONNECTED -> { icon = Icons.Filled.Wifi; contentDescription = "Connected to server" }
        ConnectionStatus.SENDING_PENDING_ACK -> { icon = Icons.Filled.HourglassTop; contentDescription = "Sending data, awaiting acknowledgment" }
        ConnectionStatus.CONNECTION_LOST -> { icon = Icons.Filled.ErrorOutline; contentDescription = "Connection to server lost" }
    }

    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = icon,
            transitionSpec = {
                (fadeIn(animationSpec = tween(200)) + scaleIn(initialScale = 0.8f, animationSpec = tween(200)))
                    .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.8f, animationSpec = tween(200)))
            },
            label = "ConnectionStatusIconAnimation"
        ) { targetIcon ->
            Icon(targetIcon, contentDescription, tint = tintColor, modifier = Modifier.size(24.dp))
        }
    }
}
