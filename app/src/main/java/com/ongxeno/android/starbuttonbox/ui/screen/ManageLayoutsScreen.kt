package com.ongxeno.android.starbuttonbox.ui.screen // Ensure correct package

import androidx.compose.foundation.gestures.detectDragGestures // Changed from detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.* // Import Material 3 components
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // Import Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity // Import LocalDensity
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.MainViewModel
import com.ongxeno.android.starbuttonbox.ui.model.LayoutInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Screen for managing layouts: reordering, deleting, hiding/showing, and adding new ones.
 * Drag starts immediately on the drag handle.
 */
@OptIn(ExperimentalMaterial3Api::class) // OptIn for TopAppBar
@Composable
fun ManageLayoutsScreen(viewModel: MainViewModel) {
    // Collect state from ViewModel
    val layouts by viewModel.allLayoutsState.collectAsStateWithLifecycle() // Use all layouts here
    val showDeleteDialog by viewModel.showDeleteConfirmationDialogState.collectAsStateWithLifecycle()
    val layoutToDelete by viewModel.layoutToDeleteState
    val density = LocalDensity.current.density

    // State for drag and drop
    var overscrollJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }

    // Drag and drop handlers
    fun onMove(from: Int, to: Int) {
        if (from != to && from in layouts.indices && to in layouts.indices) { // Check bounds against 'layouts'
            viewModel.reorderLayouts(from, to)
        }
        // Reset state after triggering move, list will recompose from VM state
        draggingItemIndex = null
        dragOffset = 0f
    }

    fun onDragStart(index: Int) {
        draggingItemIndex = index
        dragOffset = 0f
    }

    fun onDragEnd() {
        draggingItemIndex = null
        dragOffset = 0f
    }

    // onDrag lambda now uses the density captured from the outer scope
    fun onDrag(offset: Float) {
        dragOffset += offset
        draggingItemIndex?.let { index ->
            val itemHeightDp = 72.dp // Approximate height
            // Use the captured density value here
            val itemHeightPx = itemHeightDp.value * density
            // Avoid division by zero if itemHeightPx is somehow zero
            if (itemHeightPx > 0) {
                val movedBy = (dragOffset / itemHeightPx).toInt()
                val targetIndex = (index + movedBy).coerceIn(0, layouts.lastIndex)
                if (targetIndex != index) {
                    onMove(index, targetIndex)
                    // Update dragging index immediately after move is triggered
                    draggingItemIndex = targetIndex
                    // Reset offset after move to prevent accumulating large offsets across multiple moves
                    dragOffset = 0f
                }
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar( // Requires OptIn(ExperimentalMaterial3Api::class)
                title = { Text("Manage Layouts") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.hideManageLayoutsScreen() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Import Button (Placeholder)
                    TextButton(
                        onClick = { /* TODO: Implement Import Layout */ },
                        enabled = false // Disabled for now
                    ) {
                        Text("Import")
                    }
                    // New Layout Button (Placeholder)
                    TextButton(
                        onClick = { /* TODO: Implement Add Layout */ },
                        enabled = false // Disabled for now
                    ) {
                        Text("New")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
        ) {
            itemsIndexed(layouts, key = { _, item -> item.id }) { index, layoutInfo ->
                val isDragging = index == draggingItemIndex
                val currentDragOffset = if (isDragging) dragOffset else 0f

                LayoutListItem(
                    layoutInfo = layoutInfo,
                    isDragging = isDragging,
                    dragOffset = currentDragOffset,
                    // Pass the drag gesture modifier factory for the handle
                    dragHandleModifier = Modifier.pointerInput(Unit) {
                        detectDragGestures( // Changed from detectDragGesturesAfterLongPress
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
                    onDeleteClick = { viewModel.requestDeleteLayout(layoutInfo) },
                    // Remove pointerInput from the item's main modifier
                    modifier = Modifier // Pass a base modifier if needed, but not the drag one
                )
            }
        }

        // Confirmation Dialog
        if (showDeleteDialog) {
            // Use correct import path for dialog
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
 * Now accepts a specific modifier for the drag handle.
 */
@Composable
private fun LayoutListItem(
    layoutInfo: LayoutInfo,
    isDragging: Boolean,
    dragOffset: Float,
    dragHandleModifier: Modifier, // Modifier specifically for the drag handle
    onToggleVisibilityClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier // General modifier for the Card
) {
    val itemColor = if (layoutInfo.isEnabled) LocalContentColor.current else Color.Gray.copy(alpha = 0.6f)
    val visibilityIcon = if (layoutInfo.isEnabled) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
    val visibilityDesc = if (layoutInfo.isEnabled) "Hide Layout" else "Show Layout"

    Card(
        modifier = modifier // Apply general modifier to the Card
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer {
                translationY = dragOffset
                shadowElevation = if (isDragging) 8f else 1f
                alpha = if (isDragging) 0.9f else if (!layoutInfo.isEnabled) 0.7f else 1f
            }
            .zIndex(if (isDragging) 1f else 0f),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Apply the specific drag handle modifier ONLY to the DragHandle Icon
            Icon(
                imageVector = Icons.Filled.DragHandle,
                contentDescription = "Drag to reorder",
                // Apply the dragHandleModifier here
                modifier = dragHandleModifier.size(24.dp),
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
