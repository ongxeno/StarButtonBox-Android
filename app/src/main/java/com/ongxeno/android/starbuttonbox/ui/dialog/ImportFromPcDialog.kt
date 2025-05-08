package com.ongxeno.android.starbuttonbox.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.ui.screen.managelayout.ManageLayoutsViewModel
import com.ongxeno.android.starbuttonbox.ui.theme.StarButtonBoxTheme // Import your theme

/**
 * A dialog to manage the process of importing a layout file from the connected PC.
 * Displays status messages and allows cancellation. It is not dismissible by
 * back press or clicking outside.
 *
 * @param showDialog Boolean state controlling the dialog's visibility.
 * @param viewModel The ManageLayoutsViewModel instance providing status and actions.
 */
@Composable
fun ImportFromPcDialog(
    viewModel: ManageLayoutsViewModel = hiltViewModel() // Get ViewModel via Hilt
) {
    // Collect the necessary state from the ViewModel
    val statusMessage by viewModel.importFromPcStatusMessageState.collectAsStateWithLifecycle()

    Dialog(
        onDismissRequest = { /* Dialog is not dismissible by clicking outside or back press */ },
        properties = DialogProperties(
            dismissOnBackPress = false, // Cannot dismiss with back button
            dismissOnClickOutside = false // Cannot dismiss by clicking outside
        )
    ) {
        Card(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp) // Padding around the card
        ) {
            Column(
                modifier = Modifier.padding(24.dp), // Padding inside the card
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Title ---
                Text(
                    text = "Import Layout from PC",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // --- Status Display ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = statusMessage ?: "Initializing...", // Show default if null
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- Action Button (Cancel) ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { viewModel.cancelPcImport() } // Call cancel function on ViewModel
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

// --- Preview Composable ---
@Preview(showBackground = true)
@Composable
private fun ImportFromPcDialogPreview() {
    // This preview won't have a real ViewModel, so we simulate the state
    StarButtonBoxTheme { // Apply your app's theme
        // Simulate the dialog being shown with a sample status message
        Dialog(onDismissRequest = { /* No-op for preview */ }) {
            Card(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Import Layout from PC",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Waiting for PC browser...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { /* No-op in preview */ }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}
