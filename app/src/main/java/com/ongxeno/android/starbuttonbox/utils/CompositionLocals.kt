package com.ongxeno.android.starbuttonbox.utils

import androidx.compose.runtime.staticCompositionLocalOf
import com.ongxeno.android.starbuttonbox.datasource.LayoutDatasource

val LocalLayoutDatasource = staticCompositionLocalOf<LayoutDatasource> {
    error("No LayoutDatasource provided")
}