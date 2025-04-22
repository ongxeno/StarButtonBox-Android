package com.ongxeno.android.starbuttonbox.ui.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
        Text(
            "COMBAT SYSTEMS",
            color = Color.LightGray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text("COUNTERMEASURES", color = Color.Gray, fontSize = 10.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MomentaryButton(
                text = "PANIC",
                modifier = Modifier.weight(1f),
                onPress = { sendCommand(Command.Countermeasures.DeployDecoyPanic) })
            MomentaryButton(
                text = "DECOY",
                modifier = Modifier.weight(1f),
                onPress = { sendCommand(Command.Countermeasures.DeployDecoyBurst) })
            MomentaryButton(
                text = "NOISE",
                modifier = Modifier.weight(1f),
                onPress = { sendCommand(Command.Countermeasures.DeployNoise) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("POWER MANAGEMENT", color = Color.Gray, fontSize = 10.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top
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