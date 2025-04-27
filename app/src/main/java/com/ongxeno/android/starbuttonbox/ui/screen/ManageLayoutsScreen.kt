package com.ongxeno.android.starbuttonbox.ui.screen

// Removed Log import
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
// Removed SnapshotStateList import
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.MainViewModel
import com.ongxeno.android.starbuttonbox.ui.model.LayoutInfo
// Removed Job and launch imports (no longer needed here)
import kotlin.math.roundToInt

/**
 * Screen for managing layouts: reordering, deleting, hiding/showing, and adding new ones.
 * Drag starts immediately on the drag handle. Relies on ViewModel for final reorder state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageLayoutsScreen(viewModel: MainViewModel) {
    // Collect state directly from ViewModel
    val layouts by viewModel.allLayoutsState.collectAsStateWithLifecycle()
    val showDeleteDialog by viewModel.showDeleteConfirmationDialogState.collectAsStateWithLifecycle()
    val layoutToDelete by viewModel.layoutToDeleteState
    val showAddDialog by viewModel.showAddLayoutDialogState.collectAsStateWithLifecycle()
    val density = LocalDensity.current.density

    // --- Local state ONLY for drag visuals ---
    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    // Removed dragStartY state (not strictly needed for offset calculation)
    var dragCurrentOffsetY by remember { mutableStateOf(0f) }

    val currentDraggingItemIndex = draggingItemIndex
    val itemHeightPx = remember(density) { (72.dp * density).value } // Approx height + padding in px

    // --- Drag and drop handlers ---
    fun onDragStart(index: Int) {
        // Prevent dragging if list is empty or invalid index
        if (index !in layouts.indices) return
        draggingItemIndex = index
        dragCurrentOffsetY = 0f
    }

    fun onDragEnd() {
        val startIndex = draggingItemIndex // Capture the start index

        if (startIndex != null && startIndex in layouts.indices) { // Check if startIndex is valid
            // Calculate the final target index based on the final offset
            val movedBy = if (itemHeightPx > 0) (dragCurrentOffsetY / itemHeightPx).roundToInt() else 0
            val endIndex = (startIndex + movedBy).coerceIn(layouts.indices) // Use layouts.indices

            if (startIndex != endIndex) {
                // Calculate the list of IDs in the *intended* final order
                val currentIds = layouts.map { it.id }.toMutableList()
                // Ensure startIndex is still valid before removing (list might have changed)
                if(startIndex < currentIds.size){
                    val movedId = currentIds.removeAt(startIndex)
                    // Ensure endIndex is valid for insertion
                    val safeEndIndex = endIndex.coerceAtMost(currentIds.size)
                    currentIds.add(safeEndIndex, movedId)
                    // Call ViewModel to save the new order
                    viewModel.saveLayoutOrder(currentIds)
                }
            }
        }
        // Reset drag state AFTER calculations and VM call
        draggingItemIndex = null
        dragCurrentOffsetY = 0f
    }

    fun onDrag(changeY: Float) {
        // Only update offset if currently dragging
        if (draggingItemIndex != null) {
            dragCurrentOffsetY += changeY
        }
    }

    // Calculate target index for visual feedback during drag
    val targetIndexForFeedback by remember(currentDraggingItemIndex, dragCurrentOffsetY) {
        derivedStateOf {
            currentDraggingItemIndex?.let { startIndex ->
                val movedBy = if(itemHeightPx > 0) (dragCurrentOffsetY / itemHeightPx).roundToInt() else 0
                // Use layouts.indices for bounds
                (startIndex + movedBy).coerceIn(layouts.indices)
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Layouts") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.hideManageLayoutsScreen() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { /* TODO: Implement Import Layout */ }, enabled = false) { Text("Import") }
                    TextButton(onClick = { viewModel.requestAddLayout() }, enabled = true) { Text("New") }
                }
            )
        }
    ) { paddingValues ->
        // Use the layouts list directly from the ViewModel state
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
        ) {
            itemsIndexed(layouts, key = { _, item -> item.id }) { index, layoutInfo ->
                val isCurrentlyDragging = index == draggingItemIndex
                val visualOffset = if (isCurrentlyDragging) dragCurrentOffsetY else 0f
                val isTarget = !isCurrentlyDragging && index == targetIndexForFeedback && draggingItemIndex != null

                LayoutListItem(
                    layoutInfo = layoutInfo,
                    isDragging = isCurrentlyDragging,
                    isTarget = isTarget,
                    dragOffset = visualOffset,
                    dragHandleModifier = Modifier.pointerInput(layouts) { // Pass layouts as key
                        detectDragGestures(
                            onDragStart = { onDragStart(index) }, // Simplified call
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }, // Also reset on cancel
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.y)
                            }
                        )
                    },
                    onToggleVisibilityClick = { viewModel.toggleLayoutEnabled(layoutInfo.id) },
                    onDeleteClick = { viewModel.requestDeleteLayout(layoutInfo) },
                    modifier = Modifier
                )
            }
        }

        // Add Layout Dialog
        if (showAddDialog) {
            com.ongxeno.android.starbuttonbox.ui.dialog.AddLayoutDialog(
                onDismissRequest = { viewModel.cancelAddLayout() },
                onConfirm = { title, iconName -> viewModel.confirmAddLayout(title, iconName) }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            com.ongxeno.android.starbuttonbox.ui.dialog.DeleteConfirmationDialog(
                layoutInfo = layoutToDelete,
                onConfirm = { viewModel.confirmDeleteLayout() },
                onDismiss = { viewModel.cancelDeleteLayout() }
            )
        }
    }
}

/**
 * Composable for displaying a single layout item in the management list.
 */
@Composable
private fun LayoutListItem(
    layoutInfo: LayoutInfo,
    isDragging: Boolean,
    isTarget: Boolean,
    dragOffset: Float,
    dragHandleModifier: Modifier,
    onToggleVisibilityClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemColor = if (layoutInfo.isEnabled) LocalContentColor.current else Color.Gray.copy(alpha = 0.6f)
    val visibilityIcon = if (layoutInfo.isEnabled) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
    val visibilityDesc = if (layoutInfo.isEnabled) "Hide Layout" else "Show Layout"
    val cardBorder = if (isTarget) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    val containerColor = if (isTarget) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
    val cardColors = CardDefaults.cardColors( containerColor = containerColor )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer {
                translationY = dragOffset
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
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = "Drag to reorder",
                modifier = dragHandleModifier.size(24.dp), // Apply drag modifier here
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(16.dp))
            Icon(
                imageVector = layoutInfo.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = itemColor
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = layoutInfo.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = itemColor
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onToggleVisibilityClick) {
                Icon(
                    imageVector = visibilityIcon,
                    contentDescription = visibilityDesc,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onDeleteClick,
                enabled = layoutInfo.isDeletable
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Layout",
                    tint = if (layoutInfo.isDeletable) MaterialTheme.colorScheme.error else Color.Gray.copy(alpha = 0.4f)
                )
            }
        }
    }
}
