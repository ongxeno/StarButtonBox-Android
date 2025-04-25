package com.ongxeno.android.starbuttonbox.ui.layout

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ongxeno.android.starbuttonbox.MainViewModel
import com.ongxeno.android.starbuttonbox.data.Command
import com.ongxeno.android.starbuttonbox.ui.button.MomentaryButton
import com.ongxeno.android.starbuttonbox.ui.button.SafetyButton
import com.ongxeno.android.starbuttonbox.ui.theme.OrangeDarkPrimary

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        color = Color.Gray,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp, top = 4.dp)
    )
}

@Composable
private fun ButtonRow(
    modifier: Modifier = Modifier,
    verticalSpacing: androidx.compose.ui.unit.Dp = 8.dp,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(6.dp),
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp).padding(vertical = verticalSpacing / 2),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/**
 * Composable layout for Normal Flight controls.
 * Now accepts MainViewModel to handle command sending.
 *
 * @param viewModel The MainViewModel instance.
 */
@Composable
fun NormalFlightLayout(viewModel: MainViewModel) {
    val context: Context = LocalContext.current.applicationContext

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // --- Column 1: Power & Systems ---
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle("POWER")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "FLT RDY", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.GeneralCockpit_FlightReady, context) })
                MomentaryButton(text = "POWER", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.PowerManagement_TogglePowerAll, context) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "ENGINES", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.PowerManagement_TogglePowerEngines, context) })
            }
            ButtonRow(modifier = Modifier.weight(1f)){
                MomentaryButton(text = "SHIELDS", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.PowerManagement_TogglePowerShields, context) })
                MomentaryButton(text = "WEAPONS", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.PowerManagement_TogglePowerWeapons, context) })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("POWER TRIANGLE")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "WEP+", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.PowerManagement_IncreasePowerWeapons, context) })
                MomentaryButton(text = "ENG+", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.PowerManagement_IncreasePowerEngines, context) })
                MomentaryButton(text = "SHD+", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.PowerManagement_IncreasePowerShields, context) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "RESET", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.PowerManagement_ResetPowerDistribution, context) })
                Spacer(modifier = Modifier.weight(2f))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("SYSTEMS")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "LIGHTS", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Flight_ToggleHeadLight, context) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "DOORS", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.GeneralCockpit_ToggleAllDoors, context) })
                MomentaryButton(text = "LOCK", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.GeneralCockpit_TogglePortLockAll, context) })
            }
        }

        // --- Column 2: Flight & Navigation ---
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle("FLIGHT")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "GEAR", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.LandingAndDocking_ToggleLandingGear, context) })
                MomentaryButton(text = "VTOL", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Flight_ToggleVTOLMode, context) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "DECOUPLE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Flight_ToggleDecoupledMode, context) })
                MomentaryButton(text = "CRUISE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Flight_ToggleCruiseControl, context) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "SPD LMT", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Flight_ToggleSpeedLimiter, context) })
                MomentaryButton(text = "ESP", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Flight_ToggleLockPitchYaw, context) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "ATC", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.LandingAndDocking_RequestLandingTakeoff, context) })
                MomentaryButton(text = "DOCK", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.LandingAndDocking_RequestDocking, context) })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("NAVIGATION")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "QT MODE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.QuantumTravel_ToggleQuantumMode, context) })
                MomentaryButton(text = "QT ENGAGE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.QuantumTravel_ActivateQuantumTravel, context) })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("SCANNING")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "SCAN MODE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Scanning_ToggleScanningMode, context) })
                MomentaryButton(text = "PING", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Scanning_ActivatePing, context) })
            }
        }

        // --- Column 3: Targeting ---
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle("TARGETING")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "TGT FWD", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Targeting_LockSelectedTarget, context) })
                MomentaryButton(text = "TGT BCK", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Targeting_CycleTargetsBackward, context) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "HOSTILE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Targeting_CycleLockHostilesNext, context) })
                MomentaryButton(text = "FRIENDLY", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Targeting_CycleLockFriendliesNext, context) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "ATTACKER", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Targeting_CycleLockAttackersNext, context) })
                MomentaryButton(text = "ALL", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Targeting_CycleLockAllNext, context) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "SUB FWD", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Targeting_CycleLockSubtargetsNext, context) })
                MomentaryButton(text = "SUB RST", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Targeting_ResetSubtargetToMain, context) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "PIN", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Targeting_PinTarget1, context) })
                MomentaryButton(text = "UNLOCK", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Targeting_UnlockLockedTarget, context) })
            }
        }

        // --- Column 4: Combat & Emergency ---
        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle("COMBAT")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "FIRE W1", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.CombatPilot_FireWeaponGroup1, context) })
                MomentaryButton(text = "FIRE W2", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.CombatPilot_FireWeaponGroup2, context) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "MISSILE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.CombatPilot_ToggleMissileOperatorMode, context) })
                MomentaryButton(text = "LAUNCH", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.CombatPilot_LaunchMissile, context) })
            }
            ButtonRow(modifier = Modifier.weight(1f)){
                MomentaryButton(text = "CYCLE MSL", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.CombatPilot_CycleMissileType, context) })
                MomentaryButton(text = "CYCLE FIRE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.CombatPilot_CycleFireMode, context) })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("COUNTERMEASURES")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "DECOY", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Countermeasures_LaunchDecoy, context) })
                MomentaryButton(text = "NOISE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { viewModel.sendCommand(Command.Countermeasures_LaunchNoise, context) })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("EMERGENCY")
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SafetyButton(
                    text = "EJECT",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    coverColor = OrangeDarkPrimary,
                    onSafeClick = { viewModel.sendCommand(Command.GeneralCockpit_Eject, context) }
                )
                SafetyButton(
                    text = "S/DEST",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    coverColor = OrangeDarkPrimary,
                    onSafeClick = { viewModel.sendCommand(Command.GeneralCockpit_SelfDestruct, context) }
                )
            }
            ButtonRow(modifier = Modifier.weight(1f)){
                MomentaryButton(text = "EXIT SEAT", modifier = Modifier.fillMaxWidth().fillMaxHeight(), onPress = { viewModel.sendCommand(Command.GeneralCockpit_ExitSeat, context)})
            }
        }
    }
}
