package com.ongxeno.android.starbuttonbox.ui.screen.managelayout

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.data.ImportResult
import com.ongxeno.android.starbuttonbox.data.ConnectionStatus
import com.ongxeno.android.starbuttonbox.data.LayoutType
import com.ongxeno.android.starbuttonbox.ui.dialog.AddEditLayoutDialog
import com.ongxeno.android.starbuttonbox.ui.dialog.DeleteConfirmationDialog
import com.ongxeno.android.starbuttonbox.ui.dialog.ImportFromPcDialog // <-- Import the new dialog
import com.ongxeno.android.starbuttonbox.ui.dialog.ImportResultDialog
import com.ongxeno.android.starbuttonbox.utils.IconMapper
import kotlin.math.roundToInt

/**
 * Screen for managing layouts: reordering, deleting, hiding/showing, and adding new ones.
 * Includes item placement animations via animateItem.
 * The dropped item snaps to place by *not* applying animateItem temporarily.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ManageLayoutsScreen(
    viewModel: ManageLayoutsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {

    val layouts by viewModel.manageLayoutsState.collectAsStateWithLifecycle()
    val showDeleteDialog by viewModel.showDeleteConfirmationDialogState.collectAsStateWithLifecycle()
    val layoutToDelete by viewModel.layoutToDeleteState
    val showAddEditDialog by viewModel.showAddEditLayoutDialogState.collectAsStateWithLifecycle()
    val layoutToEdit by viewModel.layoutToEditState
    val importResult by viewModel.importResultState.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatusState.collectAsStateWithLifecycle()
    // --- Collect state for the new dialog ---
    val showImportFromPcDialog by viewModel.showImportFromPcDialogState.collectAsStateWithLifecycle()

    val density = LocalDensity.current.density
    val context = LocalContext.current

    // --- Activity Result Launchers for SAF ---
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportLayoutToFile(it)
        } ?: run {
            viewModel.clearExportRequest()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importLayoutFromFile(it)
        }
    }

    // --- Local state for drag visuals ---
    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragCurrentOffsetY by remember { mutableStateOf(0f) }
    var justDroppedItemId by remember { mutableStateOf<String?>(null) }
    val currentDraggingItemIndex = draggingItemIndex
    val itemHeightPx = remember(density) { (72.dp * density).value }

    LaunchedEffect(layouts) {
        if (justDroppedItemId != null) {
            justDroppedItemId = null
        }
    }

    // --- Drag and drop handlers ---
    fun onDragStart(index: Int) {
        if (index !in layouts.indices) return
        justDroppedItemId = null
        draggingItemIndex = index
        dragCurrentOffsetY = 0f
    }

    fun onDragEnd() {
        val startIndex = draggingItemIndex
        var movedId: String?

        if (startIndex != null && startIndex in layouts.indices) {
            val movedBy = if (itemHeightPx > 0) (dragCurrentOffsetY / itemHeightPx).roundToInt() else 0
            val endIndex = (startIndex + movedBy).coerceIn(layouts.indices)

            if (startIndex != endIndex) {
                val currentIds = layouts.map { it.id }.toMutableList()
                if(startIndex < currentIds.size){
                    movedId = currentIds.removeAt(startIndex)
                    val safeEndIndex = endIndex.coerceAtMost(currentIds.size)
                    currentIds.add(safeEndIndex, movedId)
                    viewModel.saveLayoutOrder(currentIds)
                    justDroppedItemId = movedId
                }
            }
        }
        draggingItemIndex = null
        dragCurrentOffsetY = 0f
    }

    fun onDrag(changeY: Float) {
        if (draggingItemIndex != null) {
            dragCurrentOffsetY += changeY
        }
    }

    val targetIndexForFeedback by remember(currentDraggingItemIndex, dragCurrentOffsetY) {
        derivedStateOf {
            currentDraggingItemIndex?.let { startIndex ->
                val movedBy = if(itemHeightPx > 0) (dragCurrentOffsetY / itemHeightPx).roundToInt() else 0
                (startIndex + movedBy).coerceIn(layouts.indices)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Layouts") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (connectionStatus == ConnectionStatus.CONNECTED || connectionStatus == ConnectionStatus.SENDING_PENDING_ACK) {
                                viewModel.initiatePcImport()
                            } else {
                                Toast.makeText(context, "Server connection required for PC import", Toast.LENGTH_SHORT).show()
                            }
                         },
                        enabled = true
                    ) {
                        Text("Import PC")
                    }
                    TextButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        enabled = true
                    ) {
                        Text("Import")
                    }
                    TextButton(
                        onClick = { viewModel.requestAddLayout() },
                        enabled = true
                    ) {
                        Text("New")
                    }
                }
            )
        }
    ) { paddingValues ->
        // --- Main Content (LazyColumn) ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
        ) {
            itemsIndexed(
                items = layouts,
                key = { _, item -> item.id }
            ) { index, layoutInfo ->
                val isCurrentlyDragging = index == currentDraggingItemIndex
                val visualDragOffset = if (isCurrentlyDragging) dragCurrentOffsetY else 0f
                val isTargetDropSlot = !isCurrentlyDragging && index == targetIndexForFeedback && draggingItemIndex != null

                val itemModifier = if (layoutInfo.id == justDroppedItemId) {
                    Modifier
                } else {
                    Modifier.animateItem(
                        placementSpec = tween(durationMillis = 300)
                    )
                }

                LayoutListItem(
                    layoutInfo = layoutInfo,
                    isDragging = isCurrentlyDragging,
                    isTargetDropSlot = isTargetDropSlot,
                    dragOffset = visualDragOffset,
                    dragHandleModifier = Modifier.pointerInput(layouts) {
                        detectDragGestures(
                            onDragStart = { onDragStart(index) },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            }
                        )
                    },
                    onToggleVisibilityClick = { viewModel.toggleLayoutEnabled(layoutInfo.id) },
                    onExportClick = {
                        viewModel.requestExportLayout(layoutInfo.id)
                        val suggestedName = "${layoutInfo.title.replace(" ", "_")}.json"
                        exportLauncher.launch(suggestedName)
                    },
                    onEditClick = { viewModel.requestEditLayout(layoutInfo) },
                    onDeleteClick = { viewModel.requestDeleteLayout(layoutInfo) },
                    modifier = itemModifier
                )
            }
        } // End LazyColumn

        // --- Dialogs ---
        // (AddEditLayoutDialog, DeleteConfirmationDialog, ImportResultDialog remain unchanged)
        if (showAddEditDialog) {
            AddEditLayoutDialog(
                layoutToEdit = layoutToEdit,
                onDismissRequest = { viewModel.cancelAddEditLayout() },
                onConfirm = { title, iconName, existingId ->
                    viewModel.confirmSaveLayout(title, iconName, existingId)
                }
            )
        }

        if (showDeleteDialog) {
            DeleteConfirmationDialog(
                layoutInfo = layoutToDelete,
                onConfirm = { viewModel.confirmDeleteLayout() },
                onDismiss = { viewModel.cancelDeleteLayout() }
            )
        }

        if (importResult != ImportResult.Idle) {
            ImportResultDialog(
                importResult = importResult,
                onDismiss = { viewModel.dismissImportResult() }
            )
        }

        if (showImportFromPcDialog) {
            ImportFromPcDialog(
                viewModel = viewModel
            )
        }

    }
}

/**
 * Composable for displaying a single layout item in the management list.
 * (This function remains unchanged)
 */
@Composable
private fun LayoutListItem(
    layoutInfo: ManageLayoutInfo,
    isDragging: Boolean,
    isTargetDropSlot: Boolean,
    dragOffset: Float,
    onToggleVisibilityClick: () -> Unit,
    onExportClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    dragHandleModifier: Modifier,
    modifier: Modifier = Modifier
) {
    val itemColor = if (layoutInfo.isEnabled) LocalContentColor.current else Color.Gray.copy(alpha = 0.6f)
    val visibilityIcon = if (layoutInfo.isEnabled) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
    val visibilityDesc = if (layoutInfo.isEnabled) "Hide Layout" else "Show Layout"
    val cardBorder = if (isTargetDropSlot) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    val containerColor = if (isTargetDropSlot) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
    val cardColors = CardDefaults.cardColors( containerColor = containerColor )
    val iconButtonSize = 48.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer {
                translationY = if (isDragging) dragOffset else 0f
                shadowElevation = if (isDragging) 8f else 1f
                alpha = if (isDragging) 0.9f else if (!layoutInfo.isEnabled) 0.7f else 1f
            }
            .zIndex(if (isDragging) 1f else 0f),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 2.dp),
        border = cardBorder,
        colors = cardColors
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(start = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = dragHandleModifier.size(iconButtonSize), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = IconMapper.getIconVector(layoutInfo.iconName),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = itemColor
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = layoutInfo.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = itemColor,
                maxLines = 2
            )
            Spacer(Modifier.width(8.dp))
            if (layoutInfo.type == LayoutType.FREE_FORM) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(iconButtonSize)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Layout",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(Modifier.width(iconButtonSize))
            }
            if (layoutInfo.type == LayoutType.FREE_FORM) {
                IconButton(onClick = onExportClick, modifier = Modifier.size(iconButtonSize)) {
                    Icon(
                        imageVector = Icons.Filled.Output,
                        contentDescription = "Export Layout",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(Modifier.width(iconButtonSize))
            }
            IconButton(onClick = onToggleVisibilityClick, modifier = Modifier.size(iconButtonSize)) {
                Icon(
                    imageVector = visibilityIcon,
                    contentDescription = visibilityDesc,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onDeleteClick,
                enabled = layoutInfo.isDeletable,
                modifier = Modifier.size(iconButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Layout",
                    tint = if (layoutInfo.isDeletable) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
