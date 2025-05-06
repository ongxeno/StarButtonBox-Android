package com.ongxeno.android.starbuttonbox.ui.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.ongxeno.android.starbuttonbox.ui.SendMacroViewModel
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
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(vertical = verticalSpacing / 2),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/**
 * Composable layout for Normal Flight controls.
 * Uses Macro IDs to trigger actions via the ViewModel.
 *
 * @param viewModel The MainViewModel instance, expected to have sendMacro(macroId).
 */
@Composable
fun NormalFlightLayout(viewModel: SendMacroViewModel) {
    // Define macro IDs based on the XML names (ensure these match your generated IDs)
    // It's recommended to define these as constants elsewhere for better maintenance
    val macroIdFlightReady = "sc_spaceship_general_v_flightready"
    val macroIdPowerToggle = "sc_spaceship_power_v_power_toggle"
    val macroIdPowerEngines = "sc_spaceship_power_v_power_toggle_thrusters"
    val macroIdPowerShields = "sc_spaceship_power_v_power_toggle_shields"
    val macroIdPowerWeapons = "sc_spaceship_power_v_power_toggle_weapons"
    val macroIdPowerIncWep = "sc_spaceship_power_v_engineering_assignment_weapons_increase"
    val macroIdPowerIncEng = "sc_spaceship_power_v_engineering_assignment_engine_increase"
    val macroIdPowerIncShd = "sc_spaceship_power_v_engineering_assignment_shields_increase"
    val macroIdPowerReset = "sc_spaceship_power_v_engineering_assignment_reset"
    val macroIdLights = "sc_lights_controller_v_lights"
    val macroIdToggleDoors = "sc_spaceship_general_v_toggle_all_doors" // Assuming this maps if default exists
    val macroIdToggleLock = "sc_spaceship_general_v_toggle_all_portlocks"
    val macroIdToggleGear = "sc_spaceship_movement_v_toggle_landing_system"
    val macroIdToggleVtol = "sc_spaceship_movement_v_toggle_vtol"
    val macroIdToggleDecouple = "sc_spaceship_movement_v_ifcs_toggle_vector_decoupling"
    val macroIdToggleCruise = "sc_spaceship_movement_v_ifcs_toggle_cruise_control"
    val macroIdToggleSpdLmt = "sc_spaceship_movement_v_ifcs_limiter_toggle"
    val macroIdToggleEsp = "sc_spaceship_movement_v_ifcs_toggle_esp"
    val macroIdAtcRequest = "sc_spaceship_movement_v_atc_request"
    val macroIdInvokeDock = "sc_spaceship_docking_v_invoke_docking"
    val macroIdQtMode = "sc_spaceship_quantum_v_toggle_qdrive_engagement" // Tap for mode
    val macroIdQtEngage = "sc_spaceship_quantum_v_toggle_qdrive_engagement" // Hold for engage (ViewModel handles action based on ID)
    val macroIdScanMode = "sc_seat_general_v_toggle_scan_mode"
    val macroIdScanPing = "sc_spaceship_radar_v_invoke_ping"
    val macroIdTargetFwd = "sc_spaceship_targeting_v_target_lock_selected" // Or cycle all fwd? Check mapping
    val macroIdTargetBack = "sc_spaceship_targeting_advanced_v_target_cycle_all_back"
    val macroIdTargetHostile = "sc_spaceship_targeting_advanced_v_target_cycle_hostile_fwd"
    val macroIdTargetFriendly = "sc_spaceship_targeting_advanced_v_target_cycle_friendly_fwd"
    val macroIdTargetAttacker = "sc_spaceship_targeting_advanced_v_target_cycle_attacker_fwd"
    val macroIdTargetAll = "sc_spaceship_targeting_advanced_v_target_cycle_all_fwd"
    val macroIdTargetSubFwd = "sc_spaceship_targeting_advanced_v_target_cycle_subitem_fwd"
    val macroIdTargetSubReset = "sc_spaceship_targeting_advanced_v_target_cycle_subitem_reset"
    val macroIdTargetPin = "sc_spaceship_targeting_v_target_pin_selected" // Or toggle pin 1? Check mapping
    val macroIdTargetUnlock = "sc_spaceship_targeting_v_target_unlock"
    val macroIdFireWpn1 = "sc_spaceship_weapons_v_weapon_preset_fire_guns0"
    val macroIdFireWpn2 = "sc_spaceship_weapons_v_weapon_preset_fire_guns1"
    val macroIdMissileMode = "sc_seat_general_v_set_missile_mode"
    val macroIdLaunchMissile = "sc_spaceship_missiles_v_weapon_launch_missile"
    val macroIdCycleMissile = "sc_spaceship_missiles_v_weapon_cycle_missile_fwd"
    val macroIdCycleFireMode = "sc_spaceship_weapons_v_weapon_change_firemode"
    val macroIdLaunchDecoy = "sc_spaceship_defensive_v_weapon_countermeasure_decoy_launch"
    val macroIdLaunchNoise = "sc_spaceship_defensive_v_weapon_countermeasure_noise_launch"
    val macroIdEject = "sc_seat_general_v_eject"
    val macroIdSelfDestruct = "sc_spaceship_general_v_self_destruct"
    val macroIdExitSeat = "sc_seat_general_v_emergency_exit" // Assuming emergency exit is desired

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // --- Column 1: Power & Systems ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle("POWER")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "FLT RDY", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdFlightReady) })
                MomentaryButton(text = "POWER", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdPowerToggle) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "ENGINES", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdPowerEngines) })
            }
            ButtonRow(modifier = Modifier.weight(1f)){
                MomentaryButton(text = "SHIELDS", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdPowerShields) })
                MomentaryButton(text = "WEAPONS", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdPowerWeapons) })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("POWER TRIANGLE")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "WEP+", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdPowerIncWep) })
                MomentaryButton(text = "ENG+", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdPowerIncEng) })
                MomentaryButton(text = "SHD+", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdPowerIncShd) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "RESET", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdPowerReset) })
                Spacer(modifier = Modifier.weight(2f))
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("SYSTEMS")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "LIGHTS", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdLights) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "DOORS", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdToggleDoors) })
                MomentaryButton(text = "LOCK", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdToggleLock) })
            }
        }

        // --- Column 2: Flight & Navigation ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle("FLIGHT")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "GEAR", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdToggleGear) })
                MomentaryButton(text = "VTOL", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdToggleVtol) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "DECOUPLE", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdToggleDecouple) })
                MomentaryButton(text = "CRUISE", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdToggleCruise) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "SPD LMT", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdToggleSpdLmt) })
                MomentaryButton(text = "ESP", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdToggleEsp) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "ATC", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdAtcRequest) })
                MomentaryButton(text = "DOCK", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdInvokeDock) })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("NAVIGATION")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "QT MODE", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdQtMode) })
                MomentaryButton(text = "QT ENGAGE", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdQtEngage) })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("SCANNING")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "SCAN MODE", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdScanMode) })
                MomentaryButton(text = "PING", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdScanPing) })
            }
        }

        // --- Column 3: Targeting ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle("TARGETING")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "TGT FWD", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdTargetFwd) })
                MomentaryButton(text = "TGT BCK", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdTargetBack) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "HOSTILE", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdTargetHostile) })
                MomentaryButton(text = "FRIENDLY", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdTargetFriendly) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "ATTACKER", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdTargetAttacker) })
                MomentaryButton(text = "ALL", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdTargetAll) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "SUB FWD", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdTargetSubFwd) })
                MomentaryButton(text = "SUB RST", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdTargetSubReset) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "PIN", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdTargetPin) })
                MomentaryButton(text = "UNLOCK", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdTargetUnlock) })
            }
        }

        // --- Column 4: Combat & Emergency ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle("COMBAT")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "FIRE W1", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdFireWpn1) })
                MomentaryButton(text = "FIRE W2", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdFireWpn2) })
            }
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "MISSILE", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdMissileMode) })
                MomentaryButton(text = "LAUNCH", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdLaunchMissile) })
            }
            ButtonRow(modifier = Modifier.weight(1f)){
                MomentaryButton(text = "CYCLE MSL", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdCycleMissile) })
                MomentaryButton(text = "CYCLE FIRE", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdCycleFireMode) })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("COUNTERMEASURES")
            ButtonRow(modifier = Modifier.weight(1f)) {
                MomentaryButton(text = "DECOY", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdLaunchDecoy) })
                MomentaryButton(text = "NOISE", modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdLaunchNoise) })
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp, color = Color.Gray)
            SectionTitle("EMERGENCY")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SafetyButton(
                    text = "EJECT",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    coverColor = OrangeDarkPrimary,
                    onSafeClick = { viewModel.sendMacro(macroIdEject) } // Pass ID
                )
                SafetyButton(
                    text = "S/DEST",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    coverColor = OrangeDarkPrimary,
                    onSafeClick = { viewModel.sendMacro(macroIdSelfDestruct) } // Pass ID
                )
            }
            ButtonRow(modifier = Modifier.weight(1f)){
                MomentaryButton(text = "EXIT SEAT", modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(), onPress = { viewModel.sendMacro(macroIdExitSeat)}) // Pass ID
            }
        }
    }
}