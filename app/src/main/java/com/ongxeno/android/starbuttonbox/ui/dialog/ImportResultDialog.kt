package com.ongxeno.android.starbuttonbox.ui.dialog

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.ongxeno.android.starbuttonbox.data.ImportResult

/**
 * Dialog to display the result of a layout import operation.
 *
 * @param importResult The result state (Success or Failure).
 * @param onDismiss Lambda to call when the dialog should be dismissed.
 */
@Composable
fun ImportResultDialog(
    importResult: ImportResult,
    onDismiss: () -> Unit
) {
    // Determine content based on the result type
    val title: String
    val text: String
    val icon: @Composable (() -> Unit)? // Icon is optional

    when (importResult) {
        is ImportResult.Success -> {
            title = "Import Successful"
            text = "Imported layout \"${importResult.importedLayout.title}\" with ${importResult.itemCount} buttons."
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = "Success") }
        }
        is ImportResult.Failure -> {
            title = "Import Failed"
            text = importResult.message
            icon = { Icon(Icons.Filled.Error, contentDescription = "Error") }
        }
        ImportResult.Idle -> {
            // Should not typically be shown in Idle state, but handle defensively
            return // Don't show anything if Idle
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
