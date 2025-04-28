package com.ongxeno.android.starbuttonbox.ui.screen

// Removed AnimationSpec and FiniteAnimationSpec imports as we won't conditionally set the spec anymore
// Removed snap import
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
// Removed IntOffset import (not needed for this approach)
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.MainViewModel
import com.ongxeno.android.starbuttonbox.data.ImportResult
import com.ongxeno.android.starbuttonbox.data.LayoutType
import com.ongxeno.android.starbuttonbox.ui.dialog.AddEditLayoutDialog
import com.ongxeno.android.starbuttonbox.ui.dialog.ImportResultDialog
import com.ongxeno.android.starbuttonbox.ui.model.LayoutInfo
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

    val layouts by viewModel.allLayoutsState.collectAsStateWithLifecycle()
    val showDeleteDialog by viewModel.showDeleteConfirmationDialogState.collectAsStateWithLifecycle()
    val layoutToDelete by viewModel.layoutToDeleteState
    // Use combined state for Add/Edit dialog
    val showAddEditDialog by viewModel.showAddEditLayoutDialogState.collectAsStateWithLifecycle()
    val layoutToEdit by viewModel.layoutToEditState // Get layout being edited
    val importResult by viewModel.importResultState.collectAsStateWithLifecycle()
    val density = LocalDensity.current.density

    // --- Activity Result Launchers for SAF ---
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        // The URI is received here AFTER the user selects a file destination.
        // Call the ViewModel function to perform the actual export using the URI
        // and the previously stored layout ID.
        uri?.let {
            viewModel.exportLayoutToFile(it)
        } ?: run {
            // Handle case where user cancelled file selection (optional)
            viewModel.clearExportRequest() // Clear the request state if cancelled
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
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }, // Allow JSON or any file for import
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
                    // Export Action: Store ID in temp state and launch SAF
                    onExportClick = {
                        viewModel.requestExportLayout(layoutInfo.id) // Set the ID in VM state
                        val suggestedName = "${layoutInfo.title.replace(" ", "_")}.json"
                        exportLauncher.launch(suggestedName) // Launch the file creator
                    },
                    onEditClick = { viewModel.requestEditLayout(layoutInfo) },
                    onDeleteClick = { viewModel.requestDeleteLayout(layoutInfo) },
                    // Pass the conditionally created modifier
                    modifier = itemModifier
                )
            }
        }

        // Combined Add/Edit Layout Dialog
        if (showAddEditDialog) {
            AddEditLayoutDialog( // Use the renamed dialog
                layoutToEdit = layoutToEdit, // Pass the layout being edited (null if adding)
                onDismissRequest = { viewModel.cancelAddEditLayout() }, // Use combined cancel
                onConfirm = { title, iconName, existingId ->
                    viewModel.confirmSaveLayout(title, iconName, existingId) // Use combined save
                }
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

        // Import Result Dialog
        if (importResult != ImportResult.Idle) {
            ImportResultDialog( // Use correct import path if needed
                importResult = importResult,
                onDismiss = { viewModel.dismissImportResult() }
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
    val iconButtonSize = 48.dp // Standard minimum touch target size

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
                .defaultMinSize(minHeight = 56.dp) // Ensure row has a minimum height
                .padding(start = 8.dp, end = 4.dp), // Adjust end padding slightly for buttons
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag Handle
            Box(modifier = dragHandleModifier.size(iconButtonSize), contentAlignment = Alignment.Center) { // Wrap in Box for consistent size
                Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp)) // Reduced spacer

            // Layout Icon
            Icon(
                imageVector = layoutInfo.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp), // Keep icon visual size consistent
                tint = itemColor
            )
            Spacer(Modifier.width(16.dp))

            // Title
            Text(
                text = layoutInfo.title,
                modifier = Modifier.weight(1f), // Takes available space
                style = MaterialTheme.typography.bodyLarge,
                color = itemColor,
                maxLines = 2 // Allow title to wrap if long
            )
            Spacer(Modifier.width(8.dp)) // Space before action buttons

            // Action Buttons Group (aligned to end)
            // Edit Button (Only for FreeForm for now)
            if (layoutInfo.type == LayoutType.FREE_FORM) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(iconButtonSize)) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Layout",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(Modifier.width(iconButtonSize)) // Reserve space if not editable
            }
            // Export Button (Conditional)
            if (layoutInfo.type == LayoutType.FREE_FORM) {
                IconButton(onClick = onExportClick, modifier = Modifier.size(iconButtonSize)) {
                    Icon(
                        imageVector = Icons.Filled.Output,
                        contentDescription = "Export Layout",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Reserve space even if button isn't shown
                Spacer(Modifier.width(iconButtonSize))
            }

            // Visibility Toggle Button
            IconButton(onClick = onToggleVisibilityClick, modifier = Modifier.size(iconButtonSize)) {
                Icon(
                    imageVector = visibilityIcon,
                    contentDescription = visibilityDesc,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete Button
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
