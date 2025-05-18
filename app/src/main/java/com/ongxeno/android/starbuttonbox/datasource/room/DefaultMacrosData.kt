package com.ongxeno.android.starbuttonbox.datasource.room

import kotlinx.serialization.Serializable

@Serializable
data class DefaultMacroJsonItem(
    val xmlCategoryName: String,
    val xmlActionName: String,
    val label: String,
    val title: String,
    val description: String,
    val inputAction: String?,
    val gameId: String
)