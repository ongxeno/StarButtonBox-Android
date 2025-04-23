package com.ongxeno.android.starbuttonbox.ui.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ongxeno.android.starbuttonbox.data.Command // Import Command object for constants
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

@Composable
fun NormalFlightLayout(onCommand: (String) -> Unit) { // Changed signature
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle("POWER")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "FLT RDY", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.GeneralCockpit_FlightReady) }) // Pass String
                MomentaryButton(text = "POWER", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.PowerManagement_TogglePowerAll) }) // Pass String
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "ENGINES", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.PowerManagement_TogglePowerEngines) }) // Pass String
            }
            ButtonRow(modifier = Modifier.weight(1f)){
                MomentaryButton(text = "SHIELDS", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.PowerManagement_TogglePowerShields) }) // Pass String
                MomentaryButton(text = "WEAPONS", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.PowerManagement_TogglePowerWeapons) }) // Pass String
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("POWER TRIANGLE")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "WEP+", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.PowerManagement_IncreasePowerWeapons) }) // Pass String
                MomentaryButton(text = "ENG+", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.PowerManagement_IncreasePowerEngines) }) // Pass String
                MomentaryButton(text = "SHD+", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.PowerManagement_IncreasePowerShields) }) // Pass String
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "RESET", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.PowerManagement_ResetPowerDistribution) }) // Pass String
                Spacer(modifier = Modifier.weight(2f))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("SYSTEMS")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "LIGHTS", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Flight_ToggleHeadLight) }) // Pass String
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "DOORS", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.GeneralCockpit_ToggleAllDoors) }) // Pass String
                MomentaryButton(text = "LOCK", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.GeneralCockpit_TogglePortLockAll) }) // Pass String
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle("FLIGHT")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "GEAR", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.LandingAndDocking_ToggleLandingGear) }) // Pass String
                MomentaryButton(text = "VTOL", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Flight_ToggleVTOLMode) }) // Pass String
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "DECOUPLE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Flight_ToggleDecoupledMode) }) // Pass String
                MomentaryButton(text = "CRUISE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Flight_ToggleCruiseControl) }) // Pass String
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "SPD LMT", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Flight_ToggleSpeedLimiter) }) // Pass String
                MomentaryButton(text = "ESP", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Flight_ToggleLockPitchYaw) }) // Pass String
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "ATC", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.LandingAndDocking_RequestLandingTakeoff) }) // Pass String
                MomentaryButton(text = "DOCK", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.LandingAndDocking_RequestDocking) }) // Pass String
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("NAVIGATION")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "QT MODE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.QuantumTravel_ToggleQuantumMode) }) // Pass String
                MomentaryButton(text = "QT ENGAGE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.QuantumTravel_ActivateQuantumTravel) }) // Pass String
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("SCANNING")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "SCAN MODE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Scanning_ToggleScanningMode) }) // Pass String
                MomentaryButton(text = "PING", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Scanning_ActivatePing) }) // Pass String
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle("TARGETING")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "TGT FWD", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting_LockSelectedTarget) }) // Pass String
                MomentaryButton(text = "TGT BCK", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting_CycleTargetsBackward) }) // Pass String
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "HOSTILE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting_CycleLockHostilesNext) }) // Pass String
                MomentaryButton(text = "FRIENDLY", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting_CycleLockFriendliesNext) }) // Pass String
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "ATTACKER", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting_CycleLockAttackersNext) }) // Pass String
                MomentaryButton(text = "ALL", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting_CycleLockAllNext) }) // Pass String
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "SUB FWD", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting_CycleLockSubtargetsNext) }) // Pass String
                MomentaryButton(text = "SUB RST", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting_ResetSubtargetToMain) }) // Pass String
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "PIN", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting_PinTarget1) }) // Pass String
                MomentaryButton(text = "UNLOCK", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting_UnlockLockedTarget) }) // Pass String
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle("COMBAT")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "FIRE W1", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.CombatPilot_FireWeaponGroup1) }) // Pass String
                MomentaryButton(text = "FIRE W2", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.CombatPilot_FireWeaponGroup2) }) // Pass String
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "MISSILE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.CombatPilot_ToggleMissileOperatorMode) }) // Pass String
                MomentaryButton(text = "LAUNCH", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.CombatPilot_LaunchMissile) }) // Pass String
            }
            ButtonRow(modifier = Modifier.weight(1f)){
                MomentaryButton(text = "CYCLE MSL", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.CombatPilot_CycleMissileType) }) // Pass String
                MomentaryButton(text = "CYCLE FIRE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.CombatPilot_CycleFireMode) }) // Pass String
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("COUNTERMEASURES")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "DECOY", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Countermeasures_LaunchDecoy) }) // Pass String
                MomentaryButton(text = "NOISE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Countermeasures_LaunchNoise) }) // Pass String
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
                    onSafeClick = { onCommand(Command.GeneralCockpit_Eject) } // Pass String
                )
                SafetyButton(
                    text = "S/DEST",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    coverColor = OrangeDarkPrimary,
                    onSafeClick = { onCommand(Command.GeneralCockpit_SelfDestruct) } // Pass String
                )
            }
            ButtonRow(modifier = Modifier.weight(1f)){
                MomentaryButton(text = "EXIT SEAT", modifier = Modifier.fillMaxWidth().fillMaxHeight(), onPress = { onCommand(Command.GeneralCockpit_ExitSeat)}) // Pass String
            }
        }
    }
}
