package com.ongxeno.android.starbuttonbox.data

sealed class ImportResult {
    data class Success(val importedLayout: LayoutDefinition, val itemCount: Int) : ImportResult()
    data class Failure(val message: String) : ImportResult()
    data object Idle : ImportResult()
}