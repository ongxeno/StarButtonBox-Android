package com.ongxeno.android.starbuttonbox.ui.screen

// Removed AnimationSpec and FiniteAnimationSpec imports as we won't conditionally set the spec anymore
// Removed snap import
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
// Removed IntOffset import (not needed for this approach)
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.MainViewModel
import com.ongxeno.android.starbuttonbox.ui.model.LayoutInfo
import kotlin.math.roundToInt

/**
 * Screen for managing layouts: reordering, deleting, hiding/showing, and adding new ones.
 * Includes item placement animations via animateItem.
 * The dropped item snaps to place by *not* applying animateItem temporarily.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ManageLayoutsScreen(viewModel: MainViewModel) {

    val layouts by viewModel.allLayoutsState.collectAsStateWithLifecycle()
    val showDeleteDialog by viewModel.showDeleteConfirmationDialogState.collectAsStateWithLifecycle()
    val layoutToDelete by viewModel.layoutToDeleteState
    val showAddDialog by viewModel.showAddLayoutDialogState.collectAsStateWithLifecycle()
    val density = LocalDensity.current.density

    // --- Local state for drag visuals ---
    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragCurrentOffsetY by remember { mutableStateOf(0f) }

    // --- State to track the ID of the item just dropped ---
    var justDroppedItemId by remember { mutableStateOf<String?>(null) }

    val currentDraggingItemIndex = draggingItemIndex
    val itemHeightPx = remember(density) { (72.dp * density).value }

    // Reset justDroppedItemId when the list changes after a drop
    LaunchedEffect(layouts) {
        if (justDroppedItemId != null) {
            justDroppedItemId = null
        }
    }

    // --- Drag and drop handlers ---
    fun onDragStart(index: Int) {
        if (index !in layouts.indices) return
        justDroppedItemId = null // Clear previous drop state
        draggingItemIndex = index
        dragCurrentOffsetY = 0f
    }

    fun onDragEnd() {
        val startIndex = draggingItemIndex
        var movedId: String? = null

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
                    // Set the just dropped ID *after* telling the VM to save
                    // This ID will be used to skip animateItem for this item
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

    // Calculate target index for visual feedback
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

                // Determine the base modifier, conditionally adding animateItem
                val itemModifier = if (layoutInfo.id == justDroppedItemId) {
                    // If this item was just dropped, DON'T apply animateItem
                    Modifier
                } else {
                    // Otherwise, apply animateItem for smooth placement of other items
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
                    onDeleteClick = { viewModel.requestDeleteLayout(layoutInfo) },
                    // Pass the conditionally created modifier
                    modifier = itemModifier
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
    isTargetDropSlot: Boolean,
    dragOffset: Float,
    dragHandleModifier: Modifier,
    onToggleVisibilityClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier // Base modifier, potentially includes animateItem
) {
    val itemColor = if (layoutInfo.isEnabled) LocalContentColor.current else Color.Gray.copy(alpha = 0.6f)
    val visibilityIcon = if (layoutInfo.isEnabled) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
    val visibilityDesc = if (layoutInfo.isEnabled) "Hide Layout" else "Show Layout"
    val cardBorder = if (isTargetDropSlot) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    val containerColor = if (isTargetDropSlot) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
    val cardColors = CardDefaults.cardColors( containerColor = containerColor )

    Card(
        // Apply the modifier passed from the LazyColumn item scope
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .graphicsLayer {
                translationY = if (isDragging) dragOffset else 0f // Only dragged item uses offset
                shadowElevation = if (isDragging) 8f else 1f
                alpha = if (isDragging) 0.9f else if (!layoutInfo.isEnabled) 0.7f else 1f
            }
            .zIndex(if (isDragging) 1f else 0f), // Lift dragging item
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
