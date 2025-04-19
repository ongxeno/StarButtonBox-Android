package com.ongxeno.android.starbuttonbox.ui.button

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CombatSystemsBlock(
    sendCommand: (String) -> Unit,
) {
    val powerInfoMsg = "Note: +/- Power may require MFD mapping or macros."

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color.DarkGray.copy(alpha = 0.1f))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("COMBAT SYSTEMS", color = Color.LightGray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // --- Countermeasures (Remains the same) ---
        Text("COUNTERMEASURES", color = Color.Gray, fontSize = 10.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MomentaryButton(text = "PANIC", modifier = Modifier.weight(1f)) { sendCommand("CM_Panic") }
            MomentaryButton(text = "DECOY", modifier = Modifier.weight(1f)) { sendCommand("CM_Launch_Decoy") }
            MomentaryButton(text = "NOISE", modifier = Modifier.weight(1f)) { sendCommand("CM_Launch_Noise") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Engineering Components ---
        Text("POWER MANAGEMENT", color = Color.Gray, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(8.dp)) // Add space before the row

        // Arrange ComponentPowerControls HORIZONTALLY now
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly, // Distribute components evenly
            verticalAlignment = Alignment.Top // Align tops if heights differ slightly
        ) {
            // Each ComponentPowerControl is now a vertical stack, placed side-by-side
            ComponentPowerControl(
                componentName = "WEAPONS",
                onIncrease = { sendCommand("Weapons_IncreasePower") },
                onDecrease = { sendCommand("Weapons_DecreasePower") },
                onActionClick = { sendCommand("Weapons_Toggle") }
            )
            ComponentPowerControl(
                componentName = "SHIELDS",
                onIncrease = { sendCommand("Shields_IncreasePower") },
                onDecrease = { sendCommand("Shields_DecreasePower") },
                onActionClick = { sendCommand("Shields_Toggle") }
            )
            ComponentPowerControl(
                componentName = "ENGINES",
                onIncrease = { sendCommand("Engines_IncreasePower") },
                onDecrease = { sendCommand("Engines_DecreasePower") },
                onActionClick = { sendCommand("Engines_Toggle") }
            )
            ComponentPowerControl(
                componentName = "COOLERS",
                onIncrease = { sendCommand("Coolers_IncreasePower") },
                onDecrease = { sendCommand("Coolers_DecreasePower") },
                onActionClick = { sendCommand("Coolers_Toggle") }
            )
        }
    }
}