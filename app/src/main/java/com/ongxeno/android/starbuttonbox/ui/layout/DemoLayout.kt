package com.ongxeno.android.starbuttonbox.ui.layout

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ongxeno.android.starbuttonbox.ui.button.ComponentPowerControl
import com.ongxeno.android.starbuttonbox.ui.button.MomentaryButton
import com.ongxeno.android.starbuttonbox.ui.button.SafetyButton
import com.ongxeno.android.starbuttonbox.ui.theme.GreyDarkSecondary
import com.ongxeno.android.starbuttonbox.ui.theme.StarButtonBoxTheme

private data class DemoButtonInfo(
    val name: String,
    val buttonComposable: @Composable () -> Unit
)

@Composable
fun DemoLayout(modifier: Modifier = Modifier) {
    val buttonsToShow = listOf(
        DemoButtonInfo("Momentary") {
            MomentaryButton(
                text = "Momentary",
                onPress = { Log.d("DemoLayout", "MomentaryButton clicked") }
            )
        },
        DemoButtonInfo("Timed Feedback") {
            MomentaryButton(
                text = "Timed",
                onPress = { Log.d("DemoLayout", "TimedFeedbackButton pressed") },
            )
        },
        DemoButtonInfo("Safety") {
            SafetyButton(
                text = "Safety",
                onSafeClick = { Log.d("DemoLayout", "SafetyButton pressed") }
            )
        },
        DemoButtonInfo("Power Control") {
            ComponentPowerControl( // Assuming ComponentPowerControl takes similar params or adjust as needed
                componentName = "Demo",
                onIncrease = { Log.d("DemoLayout", "Increase pressed") },
                onDecrease = { Log.d("DemoLayout", "Decrease pressed") },
                onActionClick = { Log.d("DemoLayout", "Action pressed") }
            )
        }
    )

    // Create a lazy vertical grid with 3 fixed columns
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .fillMaxSize()
            // Set the background of the grid itself to the desired line color
            .background(GreyDarkSecondary)
            // Apply overall padding around the grid if needed
            .padding(1.dp), // Use 1dp padding to show the background as the outer border
        // Use Arrangement.spacedBy to create space between cells, revealing the grid background as lines
        verticalArrangement = Arrangement.spacedBy(1.dp), // This will be the vertical line thickness
        horizontalArrangement = Arrangement.spacedBy(1.dp), // This will be the horizontal line thickness
        contentPadding = PaddingValues(0.dp) // No extra padding needed within the grid content area
    ) {
        // Iterate through the list of buttons and display each one
        items(buttonsToShow) { buttonInfo ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    // Set the background of the cell itself to contrast with the grid lines
                    .background(MaterialTheme.colorScheme.surface)
                    // Apply a fixed height to make all cells even. Adjust value as needed.
                    .height(250.dp)
                    // Apply padding *inside* the cell for the content
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                // Removed .fillMaxSize() as we now have a fixed height
            ) {
                // Display the button composable
                buttonInfo.buttonComposable()
                // Display the button name below it
                Text(
                    text = buttonInfo.name,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun DemoLayoutPreview() {
    StarButtonBoxTheme { // Use your app's theme
        DemoLayout()
    }
}
