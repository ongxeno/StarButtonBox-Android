package com.ongxeno.android.starbuttonbox.ui.dialog // Or ui.setting

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.ongxeno.android.starbuttonbox.ui.screen.managelayout.ManageLayoutInfo

/**
 * A confirmation dialog displayed before deleting a layout.
 *
 * @param layoutInfo The LayoutInfo object representing the layout to be deleted. Used to display the name.
 * @param onConfirm Lambda function to execute when the user confirms the deletion.
 * @param onDismiss Lambda function to execute when the dialog is dismissed (cancelled).
 */
@Composable
fun DeleteConfirmationDialog(
    layoutInfo: ManageLayoutInfo?, // Make nullable to handle potential state issues
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Only show the dialog if layoutInfo is not null
    if (layoutInfo != null) {
        AlertDialog(
            onDismissRequest = onDismiss, // Call onDismiss when clicking outside or back button
            icon = { Icon(Icons.Filled.Warning, contentDescription = "Warning") },
            title = { Text("Confirm Deletion") },
            text = {
                Text("Are you sure you want to delete the layout \"${layoutInfo.title}\"? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm() // Execute the confirm action
                        onDismiss() // Dismiss the dialog after confirming
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { // Execute dismiss action on cancel
                    Text("Cancel")
                }
            }
        )
    }
}
