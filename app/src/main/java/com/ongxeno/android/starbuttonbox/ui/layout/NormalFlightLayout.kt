package com.ongxeno.android.starbuttonbox.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ongxeno.android.starbuttonbox.data.Command
import com.ongxeno.android.starbuttonbox.ui.button.CombatSystemsBlock
import com.ongxeno.android.starbuttonbox.ui.button.MomentaryButton
import com.ongxeno.android.starbuttonbox.ui.button.SafetyButton
import com.ongxeno.android.starbuttonbox.ui.button.TimedFeedbackButton

/**
 * Composable function defining the layout for the "Normal Flight" tab.
 *
 * @param onCommand Lambda function to call when a command should be sent.
 */
@Composable
fun NormalFlightLayout(onCommand: (Command) -> Unit) { // Accept lambda
    Column { // Removed fillMaxSize and background - handled by parent
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
                        onCommand(Command.GeneralCockpit.FlightReady) // Use lambda
                    }
                    TimedFeedbackButton(
                        text = "ENGINES",
                        modifier = Modifier.weight(1f)
                    ) { onCommand(Command.PowerManagement.TogglePowerEngines) } // Use lambda
                }
                Row {
                    TimedFeedbackButton(
                        text = "LIGHTS",
                        modifier = Modifier.weight(1f)
                    ) { onCommand(Command.Flight.ToggleHeadLight) } // Use lambda
                    TimedFeedbackButton(
                        text = "DOORS",
                        modifier = Modifier.weight(1f)
                    ) { onCommand(Command.GeneralCockpit.ToggleAllDoors) } // Use lambda
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Emergency Block
            Column(
                modifier = Modifier
                    .weight(0.6f)
                    .padding(horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("EMERGENCY", color = Color.Gray, fontSize = 10.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SafetyButton(
                        text = "EJECT",
                        modifier = Modifier.weight(1f),
                        onSafeClick = { onCommand(Command.GeneralCockpit.Eject) } // Use lambda
                    )
                    SafetyButton(
                        text = "S/DEST",
                        modifier = Modifier.weight(1f),
                        onSafeClick = { onCommand(Command.GeneralCockpit.SelfDestruct) } // Use lambda
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
                    ) { onCommand(Command.Flight.ToggleDecoupledMode) } // Use lambda
                    TimedFeedbackButton(
                        text = "VTOL",
                        modifier = Modifier.weight(1f)
                    ) { onCommand(Command.Flight.ToggleVTOLMode) } // Use lambda
                }
                Row {
                    TimedFeedbackButton(
                        text = "CRUISE",
                        modifier = Modifier.weight(1f)
                    ) { onCommand(Command.Flight.ToggleCruiseControl) } // Use lambda
                    TimedFeedbackButton(
                        text = "SPD LMT",
                        modifier = Modifier.weight(1f)
                    ) { onCommand(Command.Flight.ToggleSpeedLimiter) } // Use lambda
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
                    ) { onCommand(Command.Targeting.CycleLockHostilesNext) } // Use lambda
                    MomentaryButton(
                        text = "TGT FRND",
                        modifier = Modifier.weight(1f)
                    ) { onCommand(Command.Targeting.CycleLockFriendliesNext) } // Use lambda
                }
                Row {
                    MomentaryButton(text = "PIN TGT", modifier = Modifier.weight(1f)) {
                        onCommand(Command.Targeting.PinTarget1) // Use lambda
                    }
                    MomentaryButton(text = "SUB RST", modifier = Modifier.weight(1f)) {
                        onCommand(Command.Targeting.ResetSubtargetToMain) // Use lambda
                    }
                }
                Row {
                    MomentaryButton(text = "SUB CYC", modifier = Modifier.weight(1f)) {
                        onCommand(Command.Targeting.CycleLockSubtargetsNext) // Use lambda
                    }
                    MomentaryButton(text = "UNLOCK", modifier = Modifier.weight(1f)) {
                        onCommand(Command.Targeting.UnlockLockedTarget) // Use lambda
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
                    ) { onCommand(Command.QuantumTravel.ToggleQuantumMode) } // Use lambda
                    MomentaryButton(
                        text = "QT ENGAGE",
                        modifier = Modifier.weight(1f)
                    ) { onCommand(Command.QuantumTravel.ActivateQuantumTravel) } // Use lambda
                }
                Row {
                    TimedFeedbackButton(
                        text = "SCAN MODE",
                        modifier = Modifier.weight(1f)
                    ) { onCommand(Command.Scanning.ToggleScanningMode) } // Use lambda
                    MomentaryButton(
                        text = "SCAN PING",
                        modifier = Modifier.weight(1f)
                    ) { onCommand(Command.Scanning.ActivatePing) } // Use lambda
                }
                Row {
                    TimedFeedbackButton(
                        text = "GEAR",
                        modifier = Modifier.weight(1f)
                    ) { onCommand(Command.LandingAndDocking.ToggleLandingGear) } // Use lambda
                    MomentaryButton(
                        text = "ATC",
                        modifier = Modifier.weight(1f)
                    ) { onCommand(Command.LandingAndDocking.RequestLandingTakeoff) } // Use lambda
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Countermeasures & Power Block
            CombatSystemsBlock(onCommand) // Pass the lambda down
        }
    }
}
