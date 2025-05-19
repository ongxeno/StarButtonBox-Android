package com.ongxeno.android.starbuttonbox.ui.layout

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.MainViewModel
import com.ongxeno.android.starbuttonbox.data.FreeFormItemState
import com.ongxeno.android.starbuttonbox.data.FreeFormItemType
import com.ongxeno.android.starbuttonbox.ui.SendMacroViewModel
import com.ongxeno.android.starbuttonbox.ui.button.MomentaryButton
import com.ongxeno.android.starbuttonbox.ui.dialog.AddEditButtonDialog
import com.ongxeno.android.starbuttonbox.utils.ColorUtils
import kotlin.math.max
import kotlin.math.roundToInt

private const val GRID_COLUMNS = 60
private const val GRID_ROWS = 35
private val gridLineColor = Color.Gray.copy(alpha = 0.3f)
private val gridStrokeWidth = 1.dp
private val gridPathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 8f), 0f)

private const val MIN_GRID_WIDTH = 2
private const val MIN_GRID_HEIGHT = 1

private val handleVisualSize = 24.dp
private val handleIconSizeMultiplier = 0.5f
private const val MIN_TOUCH_TARGET_SIZE_DP = 48

private const val TAG_LAYOUT_DRAG = "FreeFormLayoutDrag"

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun FreeFormLayout(
    viewModel: MainViewModel,
    sendMacroViewModel: SendMacroViewModel,
    modifier: Modifier = Modifier,
) {
    val itemsStateFromViewModel by viewModel.currentFreeFormItemsState.collectAsStateWithLifecycle()
    val availableMacros by viewModel.allMacrosState.collectAsStateWithLifecycle()

    val editableItemsState = remember { mutableStateListOf<FreeFormItemState>() }
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    var isLocked by rememberSaveable { mutableStateOf(true) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingItemState by remember { mutableStateOf<FreeFormItemState?>(null) }

    var currentDragVisualOffset by remember { mutableStateOf(Offset.Zero) }
    var currentResizeVisualDelta by remember { mutableStateOf(Offset.Zero) }
    var interactingItemId by remember { mutableStateOf<String?>(null) }

    val density = LocalDensity.current
    val minTouchTargetSizePx = with(density) { MIN_TOUCH_TARGET_SIZE_DP.dp.toPx() }

    LaunchedEffect(itemsStateFromViewModel, isLocked) {
        if (isLocked || !hasUnsavedChanges) {
            val viewModelList = itemsStateFromViewModel
            val localList = editableItemsState.toList()
            if (viewModelList != localList) {
                editableItemsState.clear()
                editableItemsState.addAll(viewModelList)
                hasUnsavedChanges = false
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // Ensure the root of FreeFormLayout has a background to consume touches
            .background(MaterialTheme.colorScheme.background)
    ) {
        val maxWidthPx = constraints.maxWidth.toFloat()
        val maxHeightPx = constraints.maxHeight.toFloat()
        val cellWidthPx = if (GRID_COLUMNS > 0 && maxWidthPx > 0) maxWidthPx / GRID_COLUMNS else 0f
        val cellHeightPx = if (GRID_ROWS > 0 && maxHeightPx > 0) maxHeightPx / GRID_ROWS else 0f
        val gridStrokeWidthPx = with(density) { gridStrokeWidth.toPx() }
        val isGridValid = cellWidthPx > 0 && cellHeightPx > 0

        Box(modifier = Modifier.fillMaxSize()) {
            if (!isLocked && isGridValid) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (i in 1 until GRID_COLUMNS) {
                        val x = i * cellWidthPx
                        drawLine(gridLineColor, Offset(x, 0f), Offset(x, size.height), gridStrokeWidthPx, pathEffect = gridPathEffect)
                    }
                    for (i in 1 until GRID_ROWS) {
                        val y = i * cellHeightPx
                        drawLine(gridLineColor, Offset(0f, y), Offset(size.width, y), gridStrokeWidthPx, pathEffect = gridPathEffect)
                    }
                }
            }

            editableItemsState.forEachIndexed { index, itemState ->
                val itemId = itemState.id
                val itemBaseAbsoluteX = itemState.gridCol * cellWidthPx
                val itemBaseAbsoluteY = itemState.gridRow * cellHeightPx
                val itemBaseAbsoluteWidth = itemState.gridWidth * cellWidthPx
                val itemBaseAbsoluteHeight = itemState.gridHeight * cellHeightPx
                val currentResizeDeltaW = if (itemId == interactingItemId && currentResizeVisualDelta != Offset.Zero) currentResizeVisualDelta.x else 0f
                val currentResizeDeltaH = if (itemId == interactingItemId && currentResizeVisualDelta != Offset.Zero) currentResizeVisualDelta.y else 0f
                val finalRenderWidthPx = itemBaseAbsoluteWidth + currentResizeDeltaW
                val finalRenderHeightPx = itemBaseAbsoluteHeight + currentResizeDeltaH
                val itemWidthDp = with(density) { finalRenderWidthPx.toDp() }
                val itemHeightDp = with(density) { finalRenderHeightPx.toDp() }
                val defaultM3ButtonColors = ButtonDefaults.buttonColors()
                val buttonColors = remember(itemState.backgroundColorHex) {
                    val enabledContainerColor = ColorUtils.parseHexColor(itemState.backgroundColorHex) ?: defaultM3ButtonColors.containerColor
                    val enabledContentColor = ColorUtils.getContrastingTextColor(enabledContainerColor)
                    ButtonColors(
                        containerColor = enabledContainerColor, contentColor = enabledContentColor,
                        disabledContainerColor = enabledContainerColor.copy(alpha = 0.5f),
                        disabledContentColor = enabledContentColor.copy(alpha = 0.7f)
                    )
                }
                val textSize = remember(itemState.textSizeSp) { itemState.textSizeSp?.sp ?: TextUnit.Unspecified }

                Box(
                    modifier = Modifier
                        .size(itemWidthDp, itemHeightDp)
                        .graphicsLayer {
                            val visualOffsetX = if (itemId == interactingItemId) currentDragVisualOffset.x else 0f
                            val visualOffsetY = if (itemId == interactingItemId) currentDragVisualOffset.y else 0f
                            translationX = itemBaseAbsoluteX + visualOffsetX
                            translationY = itemBaseAbsoluteY + visualOffsetY
                        }
                        .pointerInput(isLocked, isGridValid, itemId, editableItemsState.size) {
                            if (!isLocked && isGridValid) {
                                detectDragGestures(
                                    onDragStart = { interactingItemId = itemId; currentDragVisualOffset = Offset.Zero; currentResizeVisualDelta = Offset.Zero },
                                    onDragEnd = {
                                        val currentLocalState = editableItemsState.getOrNull(index) ?: return@detectDragGestures
                                        val finalDraggedX = (currentLocalState.gridCol * cellWidthPx) + currentDragVisualOffset.x
                                        val finalDraggedY = (currentLocalState.gridRow * cellHeightPx) + currentDragVisualOffset.y
                                        val targetCol = (finalDraggedX / cellWidthPx).roundToInt()
                                        val targetRow = (finalDraggedY / cellHeightPx).roundToInt()
                                        val clampedCol = targetCol.coerceIn(0, GRID_COLUMNS - currentLocalState.gridWidth)
                                        val clampedRow = targetRow.coerceIn(0, GRID_ROWS - currentLocalState.gridHeight)
                                        if (currentLocalState.gridCol != clampedCol || currentLocalState.gridRow != clampedRow) {
                                            editableItemsState[index] = currentLocalState.copy(gridCol = clampedCol, gridRow = clampedRow)
                                            hasUnsavedChanges = true
                                            Log.d(TAG_LAYOUT_DRAG, "Item $itemId DRAGGED to $clampedCol, $clampedRow. Has Unsaved: $hasUnsavedChanges")
                                        }
                                        currentDragVisualOffset = Offset.Zero; interactingItemId = null
                                    },
                                    onDragCancel = { interactingItemId = null; currentDragVisualOffset = Offset.Zero },
                                    onDrag = { change, dragAmount -> change.consume(); currentDragVisualOffset += dragAmount }
                                )
                            }
                        }
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (itemState.type) {
                            FreeFormItemType.MOMENTARY_BUTTON -> MomentaryButton(
                                Modifier.fillMaxSize(), itemState.text, isLocked,
                                onPress = { if (isLocked) itemState.macroId?.let { sendMacroViewModel.sendMacro(it) } },
                                colors = buttonColors, textSize = textSize, shape = RoundedCornerShape(8.dp)
                            )
                        }
                        if (!isLocked) {
                            val handleTouchOffsetXDp = with(density) { (minTouchTargetSizePx / 2).toDp() }
                            val handleTouchOffsetYDp = with(density) { (minTouchTargetSizePx / 2).toDp() }
                            Box(
                                modifier = Modifier.align(Alignment.BottomEnd)
                                    .offset(x = handleTouchOffsetXDp, y = handleTouchOffsetYDp)
                                    .size(MIN_TOUCH_TARGET_SIZE_DP.dp).clip(CircleShape)
                                    .pointerInput(Unit, isGridValid, itemId, editableItemsState.size) {
                                        if (isGridValid) {
                                            detectDragGestures(
                                                onDragStart = { interactingItemId = itemId; currentDragVisualOffset = Offset.Zero; currentResizeVisualDelta = Offset.Zero },
                                                onDragEnd = {
                                                    val currentLocalState = editableItemsState.getOrNull(index) ?: return@detectDragGestures
                                                    val finalResizedWidthPx = (currentLocalState.gridWidth * cellWidthPx) + currentResizeVisualDelta.x
                                                    val finalResizedHeightPx = (currentLocalState.gridHeight * cellHeightPx) + currentResizeVisualDelta.y
                                                    val targetWidthCells = max(MIN_GRID_WIDTH, (finalResizedWidthPx / cellWidthPx).roundToInt())
                                                    val targetHeightCells = max(MIN_GRID_HEIGHT, (finalResizedHeightPx / cellHeightPx).roundToInt())
                                                    val clampedWidthCells = targetWidthCells.coerceIn(MIN_GRID_WIDTH, GRID_COLUMNS - currentLocalState.gridCol)
                                                    val clampedHeightCells = targetHeightCells.coerceIn(MIN_GRID_HEIGHT, GRID_ROWS - currentLocalState.gridRow)
                                                    if (currentLocalState.gridWidth != clampedWidthCells || currentLocalState.gridHeight != clampedHeightCells) {
                                                        editableItemsState[index] = currentLocalState.copy(gridWidth = clampedWidthCells, gridHeight = clampedHeightCells)
                                                        hasUnsavedChanges = true
                                                        Log.d(TAG_LAYOUT_DRAG, "Item $itemId RESIZED to ${clampedWidthCells}x${clampedHeightCells}. Has Unsaved: $hasUnsavedChanges")
                                                    }
                                                    currentResizeVisualDelta = Offset.Zero; interactingItemId = null
                                                },
                                                onDragCancel = { interactingItemId = null; currentResizeVisualDelta = Offset.Zero },
                                                onDrag = { change, dragAmount -> change.consume(); currentResizeVisualDelta += dragAmount }
                                            )
                                        }
                                    }
                            ) {
                                Box(Modifier.align(Alignment.Center).size(handleVisualSize).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)).border(1.dp, MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.OpenInFull, "Resize Button", Modifier.size(handleVisualSize * handleIconSizeMultiplier), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                            Box(
                                modifier = Modifier.align(Alignment.TopStart)
                                    .offset(x = with(density) { (-minTouchTargetSizePx / 2).toDp() }, y = with(density) { (-minTouchTargetSizePx / 2).toDp() })
                                    .size(MIN_TOUCH_TARGET_SIZE_DP.dp).clip(CircleShape)
                                    .clickable { editingItemState = itemState; showAddEditDialog = true }
                            ) {
                                Box(Modifier.align(Alignment.Center).size(handleVisualSize).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.9f)).border(1.dp, MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Edit, "Edit Button", Modifier.size(handleVisualSize * handleIconSizeMultiplier), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                }
            }

            Row(modifier = Modifier.align(Alignment.TopEnd).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (!isLocked) {
                    IconButton(onClick = { editingItemState = null; showAddEditDialog = true }, modifier = Modifier.padding(end = 8.dp).background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f), MaterialTheme.shapes.medium)) {
                        Icon(Icons.Filled.Add, "Add New Button", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                IconButton(
                    onClick = {
                        if (!isLocked) {
                            if (hasUnsavedChanges) {
                                Log.i(TAG_LAYOUT_DRAG, "Locking layout and SAVING ${editableItemsState.size} items.")
                                viewModel.saveFreeFormLayout(editableItemsState.toList())
                                hasUnsavedChanges = false
                            } else {
                                Log.d(TAG_LAYOUT_DRAG, "Locking layout, no unsaved changes.")
                            }
                        } else {
                            Log.d(TAG_LAYOUT_DRAG, "Unlocking layout.")
                            if (editableItemsState.toList() != itemsStateFromViewModel) {
                                Log.d(TAG_LAYOUT_DRAG,"Syncing local state from ViewModel on unlock as they differ.")
                                editableItemsState.clear(); editableItemsState.addAll(itemsStateFromViewModel); hasUnsavedChanges = false
                            }
                        }
                        isLocked = !isLocked
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f), MaterialTheme.shapes.medium),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = if (isLocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                ) {
                    Icon(if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen, if (isLocked) "Layout Locked" else "Layout Unlocked (Tap to Lock & Save)")
                }
            }
        }
    }

    if (showAddEditDialog) { // Ensure this uses the local showAddEditDialog state
        AddEditButtonDialog(
            showDialog = showAddEditDialog,
            onDismiss = { showAddEditDialog = false },
            initialItemState = editingItemState,
            availableMacros = availableMacros,
            onSave = { text, macroId, type, textSizeSp, backgroundColorHex ->
                val currentEditId = editingItemState?.id
                if (currentEditId != null) {
                    val index = editableItemsState.indexOfFirst { it.id == currentEditId }
                    if (index != -1) {
                        editableItemsState[index] = editableItemsState[index].copy(text = text, macroId = macroId, type = type, textSizeSp = textSizeSp, backgroundColorHex = backgroundColorHex)
                        hasUnsavedChanges = true
                        Log.d(TAG_LAYOUT_DRAG, "Updated item $currentEditId locally. Has Unsaved: $hasUnsavedChanges")
                    }
                } else {
                    editableItemsState.add(FreeFormItemState(text = text, macroId = macroId, type = type, textSizeSp = textSizeSp, backgroundColorHex = backgroundColorHex))
                    hasUnsavedChanges = true
                    Log.d(TAG_LAYOUT_DRAG, "Added new item locally. Has Unsaved: $hasUnsavedChanges")
                }
                showAddEditDialog = false
            },
            onDelete = { itemId ->
                if (editableItemsState.removeIf { it.id == itemId }) {
                    hasUnsavedChanges = true
                    Log.d(TAG_LAYOUT_DRAG, "Deleted item $itemId locally. Has Unsaved: $hasUnsavedChanges")
                }
                showAddEditDialog = false
            }
        )
    }
}
