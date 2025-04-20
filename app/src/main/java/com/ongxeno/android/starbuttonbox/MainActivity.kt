package com.ongxeno.android.starbuttonbox

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ongxeno.android.starbuttonbox.data.Command
import com.ongxeno.android.starbuttonbox.datasource.UdpSender
import com.ongxeno.android.starbuttonbox.ui.button.CombatSystemsBlock
import com.ongxeno.android.starbuttonbox.ui.button.MomentaryButton
import com.ongxeno.android.starbuttonbox.ui.button.SafetyButton
import com.ongxeno.android.starbuttonbox.ui.button.TimedFeedbackButton
import com.ongxeno.android.starbuttonbox.ui.theme.StarButtonBoxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StarButtonBoxTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StarCitizenButtonBoxUi()
                }
            }
        }
    }
}

@Composable
fun StarCitizenButtonBoxUi() {
    val context = LocalContext.current

    // --- Network Configuration ---
    val targetIpAddress = "192.168.50.102"
    val targetPort = 5005

    val udpSender = remember {
        UdpSender(targetIpAddress, targetPort)
    }

    val sendCommand = { command: Command ->
        udpSender.sendCommandAction(command)

        val feedbackText = command.commandString
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, "Sent: $feedbackText", Toast.LENGTH_SHORT).apply {
                show()
                Handler(Looper.getMainLooper()).postDelayed({ this.cancel() }, 500)
            }
        }
        Unit
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Dark background
            .padding(8.dp)
    ) {
        // --- TOP ROW: Startup/Systems & Emergency ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Startup & Systems Block
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("SYSTEMS", color = Color.Gray, fontSize = 10.sp)
                Row {
                    MomentaryButton(text = "FLT RDY", modifier = Modifier.weight(1f)) {
                        sendCommand(Command.GeneralCockpit.FlightReady)
                    }
                    TimedFeedbackButton(
                        text = "ENGINES",
                        modifier = Modifier.weight(1f)
                    ) { sendCommand(Command.PowerManagement.TogglePowerEngines) }
                }
                Row {
                    TimedFeedbackButton(
                        text = "LIGHTS",
                        modifier = Modifier.weight(1f)
                    ) { sendCommand(Command.Flight.ToggleHeadLight) }
                    TimedFeedbackButton(
                        text = "DOORS",
                        modifier = Modifier.weight(1f)
                    ) { sendCommand(Command.GeneralCockpit.ToggleAllDoors) }
                }
            }

            Spacer(modifier = Modifier.width(16.dp)) // Gap between blocks

            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("EMERGENCY", color = Color.Gray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(8.dp)) // Space between title and button row

                Row( // *** Use Row for side-by-side layout ***
                    modifier = Modifier.fillMaxWidth(), // Let the Row take the available width
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // Adds space between items in the Row
                    verticalAlignment = Alignment.CenterVertically // Align items vertically if heights differ slightly
                ) {
                    SafetyButton(
                        text = "EJECT",
                        modifier = Modifier.weight(1f), // <<< Make buttons share width equally
                        onSafeClick = { sendCommand(Command.GeneralCockpit.Eject) }
                        // ... other parameters ...
                    )

                    // Spacer(Modifier.width(8.dp)) // No longer needed if using Arrangement.spacedBy

                    SafetyButton(
                        text = "S/DEST",
                        modifier = Modifier.weight(1f), // <<< Make buttons share width equally
                        // coverColor = Color(0xFFDDDD00), // Optional styling
                        // actionButtonColor = Color(0xFF660000), // Optional styling
                        onSafeClick = { sendCommand(Command.GeneralCockpit.SelfDestruct) }
                        // ... other parameters ...
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- MIDDLE ROW: Flight Modes & Targeting ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Flight Modes Block
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("FLIGHT MODES", color = Color.Gray, fontSize = 10.sp)
                Row {
                    TimedFeedbackButton(
                        text = "DECOUPLE",
                        modifier = Modifier.weight(1f)
                    ) { sendCommand(Command.Flight.ToggleDecoupledMode) }
                    TimedFeedbackButton(
                        text = "VTOL",
                        modifier = Modifier.weight(1f)
                    ) { sendCommand(Command.Flight.ToggleVTOLMode) }
                }
                Row {
                    TimedFeedbackButton(
                        text = "CRUISE",
                        modifier = Modifier.weight(1f)
                    ) { sendCommand(Command.Flight.ToggleCruiseControl) }
                    TimedFeedbackButton(
                        text = "SPD LIM",
                        modifier = Modifier.weight(1f)
                    ) { sendCommand(Command.Flight.ToggleSpeedLimiter) }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Targeting Block
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("TARGETING", color = Color.Gray, fontSize = 10.sp)
                Row {
                    MomentaryButton(
                        text = "TGT HOST",
                        modifier = Modifier.weight(1f)
                    ) { sendCommand(Command.Targeting.CycleLockHostilesClosest) }
                    MomentaryButton(
                        text = "TGT FRND",
                        modifier = Modifier.weight(1f)
                    ) { sendCommand(Command.Targeting.CycleLockFriendliesClosest) }
                }
                Row {
                    MomentaryButton(text = "PIN TGT", modifier = Modifier.weight(1f)) {
                        sendCommand(Command.Targeting.LockSelectedTarget)
                    }
                    MomentaryButton(text = "SUB RST", modifier = Modifier.weight(1f)) {
                        sendCommand(Command.Targeting.ResetSubtargetToMain)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- BOTTOM ROW: Nav/Scan/Landing & CM/Power ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Nav/Scan/Landing Block
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("NAVIGATION / LANDING", color = Color.Gray, fontSize = 10.sp)
                Row {
                    MomentaryButton(
                        text = "QT SPOOL",
                        modifier = Modifier.weight(1f)
                    ) { sendCommand(Command.QuantumTravel.ToggleQuantumMode) }
                    MomentaryButton(
                        text = "QT ENGAGE",
                        modifier = Modifier.weight(1f)
                    ) { sendCommand(Command.QuantumTravel.ActivateQuantumTravel) }
                }
                Row {
                    TimedFeedbackButton(
                        text = "SCAN MODE",
                        modifier = Modifier.weight(1f)
                    ) { sendCommand(Command.Scanning.ToggleScanningMode) }
                    MomentaryButton(
                        text = "SCAN PING",
                        modifier = Modifier.weight(1f)
                    ) { sendCommand(Command.Scanning.ActivatePing) }
                }
                Row {
                    TimedFeedbackButton(
                        text = "GEAR",
                        modifier = Modifier.weight(1f)
                    ) { sendCommand(Command.LandingAndDocking.ToggleLandingGear) }
                    MomentaryButton(
                        text = "ATC",
                        modifier = Modifier.weight(1f)
                    ) { sendCommand(Command.LandingAndDocking.RequestLandingTakeoff) }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Countermeasures & Power Block
            CombatSystemsBlock(sendCommand)
        }
        // Add more rows/columns if needed
    }
}