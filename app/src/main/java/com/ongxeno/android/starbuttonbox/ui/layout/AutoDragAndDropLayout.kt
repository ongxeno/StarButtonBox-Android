package com.ongxeno.android.starbuttonbox.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults // Added for potential color customization
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ongxeno.android.starbuttonbox.ui.button.MomentaryButton

/**
 * Composable for the Auto Drag and Drop layout.
 * Provides buttons to set source/destination mouse positions on the PC
 * and to start/stop an automated drag-and-drop loop.
 *
 * @param viewModel The ViewModel for this layout, handling the logic.
 */
@Composable
fun AutoDragAndDropLayout(
    viewModel: AutoDragAndDropLayoutViewModel = hiltViewModel()
) {
    val isLoopingActive by viewModel.isLoopingActive.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Instructional Text
        Text(
            text = "Automate Drag & Drop Operations",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "How to use:\n" +
                    "1. On your PC, position the mouse where the drag should START.\n" +
                    "2. Tap 'Set Source Position' below.\n" +
                    "3. On your PC, position the mouse where the drag should END.\n" +
                    "4. Tap 'Set Destination Position' below.\n" +
                    "5. Tap 'Start Auto Drag' to begin the loop.\n" +
                    "   The PC will repeatedly drag from source to destination.\n" +
                    "6. Tap 'Stop Auto Drag' to end the loop.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Start, // Align instructions to the start
            lineHeight = 20.sp, // Improve readability of multi-line text
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = MaterialTheme.shapes.medium)
                .padding(12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Buttons
        MomentaryButton(
            text = "Set Source Position (SRC)",
            onPress = { viewModel.onSetSourceClicked() }, // Changed onClick to onPress
            modifier = Modifier
                .fillMaxWidth(0.8f) // Make buttons a bit narrower than full width
                .height(60.dp)     // Give buttons a decent height
        )

        MomentaryButton(
            text = "Set Destination Position (DES)",
            onPress = { viewModel.onSetDestinationClicked() }, // Changed onClick to onPress
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(60.dp)
        )

        MomentaryButton(
            text = if (isLoopingActive) "Stop Auto Drag" else "Start Auto Drag",
            onPress = { viewModel.onStartStopClicked() }, // Changed onClick to onPress
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(60.dp),
            // Optionally, change button color when active
            colors = if (isLoopingActive) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer) else ButtonDefaults.buttonColors()
        )
    }
}
