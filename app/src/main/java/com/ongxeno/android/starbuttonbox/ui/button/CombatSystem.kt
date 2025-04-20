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
import com.ongxeno.android.starbuttonbox.data.Command

@Composable
fun CombatSystemsBlock(
    sendCommand: (Command) -> Unit,
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
            MomentaryButton(text = "PANIC", modifier = Modifier.weight(1f)) { sendCommand(Command.Countermeasures.DeployDecoyPanic) }
            MomentaryButton(text = "DECOY", modifier = Modifier.weight(1f)) { sendCommand(Command.Countermeasures.DeployDecoyBurst) }
            MomentaryButton(text = "NOISE", modifier = Modifier.weight(1f)) { sendCommand(Command.Countermeasures.DeployNoise) }
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
                onIncrease = { sendCommand(Command.PowerManagement.IncreasePowerWeapons) },
                onDecrease = { sendCommand(Command.PowerManagement.DecreasePowerWeapons) },
                onActionClick = { sendCommand(Command.PowerManagement.TogglePowerWeapons) }
            )
            ComponentPowerControl(
                componentName = "SHIELDS",
                onIncrease = { sendCommand(Command.PowerManagement.IncreasePowerShields) },
                onDecrease = { sendCommand(Command.PowerManagement.DecreasePowerShields) },
                onActionClick = { sendCommand(Command.PowerManagement.TogglePowerShields) }
            )
            ComponentPowerControl(
                componentName = "ENGINES",
                onIncrease = { sendCommand(Command.PowerManagement.IncreasePowerEngines) },
                onDecrease = { sendCommand(Command.PowerManagement.DecreasePowerEngines) },
                onActionClick = { sendCommand(Command.PowerManagement.TogglePowerEngines) }
            )
        }
    }
}