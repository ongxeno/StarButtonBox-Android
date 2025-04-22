package com.ongxeno.android.starbuttonbox.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.NonSkippableComposable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.currentComposer

@Composable
@OptIn(InternalComposeApi::class)
@NonSkippableComposable
fun NestedCompositionLocalProvider(
    vararg values: ProvidedValue<*>,
    content: @Composable () -> Unit
) {
    values.forEach { currentComposer.startProvider(it) }
    content()
    values.reversed().forEach { currentComposer.endProvider() }
}