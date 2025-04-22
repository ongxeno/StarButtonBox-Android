package com.ongxeno.android.starbuttonbox.ui.layout

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider // Use HorizontalDivider from M3
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun NormalFlightLayout(onCommand: (Command) -> Unit) {
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
                MomentaryButton(text = "FLT RDY", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.GeneralCockpit.FlightReady) })
                MomentaryButton(text = "POWER", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.PowerManagement.TogglePowerAll) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "ENGINES", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.PowerManagement.TogglePowerEngines) })
            }
            ButtonRow(modifier = Modifier.weight(1f)){
                MomentaryButton(text = "SHIELDS", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.PowerManagement.TogglePowerShields) })
                MomentaryButton(text = "WEAPONS", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.PowerManagement.TogglePowerWeapons) })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("POWER TRIANGLE")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "WEP+", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.PowerManagement.IncreasePowerWeapons) })
                MomentaryButton(text = "ENG+", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.PowerManagement.IncreasePowerEngines) })
                MomentaryButton(text = "SHD+", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.PowerManagement.IncreasePowerShields) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "RESET", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.PowerManagement.ResetPowerDistribution) })
                Spacer(modifier = Modifier.weight(2f))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("SYSTEMS")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "LIGHTS", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Flight.ToggleHeadLight) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "DOORS", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.GeneralCockpit.ToggleAllDoors) })
                MomentaryButton(text = "LOCK", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.GeneralCockpit.TogglePortLockAll) })
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle("FLIGHT")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "GEAR", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.LandingAndDocking.ToggleLandingGear) })
                MomentaryButton(text = "VTOL", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Flight.ToggleVTOLMode) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "DECOUPLE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Flight.ToggleDecoupledMode) })
                MomentaryButton(text = "CRUISE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Flight.ToggleCruiseControl) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "SPD LMT", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Flight.ToggleSpeedLimiter) })
                MomentaryButton(text = "ESP", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Flight.ToggleLockPitchYaw) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "ATC", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.LandingAndDocking.RequestLandingTakeoff) })
                MomentaryButton(text = "DOCK", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.LandingAndDocking.RequestDocking) })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("NAVIGATION")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "QT MODE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.QuantumTravel.ToggleQuantumMode) })
                MomentaryButton(text = "QT ENGAGE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.QuantumTravel.ActivateQuantumTravel) })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("SCANNING")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "SCAN MODE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Scanning.ToggleScanningMode) })
                MomentaryButton(text = "PING", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Scanning.ActivatePing) })
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle("TARGETING")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "TGT FWD", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting.LockSelectedTarget) })
                MomentaryButton(text = "TGT BCK", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting.CycleTargetsBackward) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "HOSTILE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting.CycleLockHostilesNext) })
                MomentaryButton(text = "FRIENDLY", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting.CycleLockFriendliesNext) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "ATTACKER", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting.CycleLockAttackersNext) })
                MomentaryButton(text = "ALL", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting.CycleLockAllNext) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "SUB FWD", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting.CycleLockSubtargetsNext) })
                MomentaryButton(text = "SUB RST", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting.ResetSubtargetToMain) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "PIN", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting.PinTarget1) })
                MomentaryButton(text = "UNLOCK", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Targeting.UnlockLockedTarget) })
            }
        }

        Column(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle("COMBAT")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "FIRE W1", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.CombatPilot.FireWeaponGroup1) })
                MomentaryButton(text = "FIRE W2", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.CombatPilot.FireWeaponGroup2) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "MISSILE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.CombatPilot.ToggleMissileOperatorMode) })
                MomentaryButton(text = "LAUNCH", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.CombatPilot.LaunchMissile) })
            }
            ButtonRow(modifier = Modifier.weight(1f)){
                MomentaryButton(text = "CYCLE MSL", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.CombatPilot.CycleMissileType) })
                MomentaryButton(text = "CYCLE FIRE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.CombatPilot.CycleFireMode) })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("COUNTERMEASURES")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "DECOY", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Countermeasures.LaunchDecoy) })
                MomentaryButton(text = "NOISE", modifier = Modifier.weight(1f).fillMaxHeight(), onPress = { onCommand(Command.Countermeasures.LaunchNoise) })
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
                    onSafeClick = { onCommand(Command.GeneralCockpit.Eject) }
                )
                SafetyButton(
                    text = "S/DEST",
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    coverColor = OrangeDarkPrimary,
                    onSafeClick = { onCommand(Command.GeneralCockpit.SelfDestruct) }
                )
            }
            ButtonRow(modifier = Modifier.weight(1f)){
                MomentaryButton(text = "EXIT SEAT", modifier = Modifier.fillMaxWidth().fillMaxHeight(), onPress = { onCommand(Command.GeneralCockpit.ExitSeat)})
            }
        }
    }
}
