package com.ongxeno.android.starbuttonbox.data

import kotlinx.serialization.Serializable

/**
 * Data class representing the structure of an exported layout file (JSON).
 * Contains the layout's definition and its associated items (if applicable).
 *
 * @param definition The core definition of the layout (title, type, icon, etc.).
 * Note: layoutItemsJson within definition might be redundant if items are present here,
 * but keeping it aligns with the LayoutDefinition structure.
 * @param items The list of FreeFormItemState objects for this layout. Null if not a FreeForm layout.
 */
@Serializable
data class ExportedLayout(
    val definition: LayoutDefinition,
    val items: List<FreeFormItemState>? // Only relevant for FREE_FORM type
)
