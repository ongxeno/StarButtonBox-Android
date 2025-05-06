package com.ongxeno.android.starbuttonbox.ui.screen.managelayout

import com.ongxeno.android.starbuttonbox.data.LayoutType

data class ManageLayoutInfo(
    val id: String,
    val title: String,
    val type: LayoutType,
    val iconName: String,
    val isEnabled: Boolean = true,
    val isUserDefined: Boolean = false,
    val isDeletable: Boolean = !isUserDefined, // Default based on isUserDefined
)
