package com.ongxeno.android.starbuttonbox.data

/**
 * Defines command identifiers for Star Citizen actions using a sealed class hierarchy.
 * Each nested data object represents a specific command.
 * The 'commandString' property holds a unique identifier (e.g., "Flight.Boost")
 * used for serialization, lookup, and logging.
 */
sealed class Command {
    // The primary property is now the unique identifier string.
    abstract val commandString: String

    // --- Categories ---
    sealed class Flight : Command() {
        data object Boost : Flight() { override val commandString = "Flight.Boost" } // HOLD Left Shift
        data object Spacebrake : Flight() { override val commandString = "Flight.Spacebrake" } // X
        data object ToggleDecoupledMode : Flight() { override val commandString = "Flight.ToggleDecoupledMode" } // Left Alt + C
        data object ToggleCruiseControl : Flight() { override val commandString = "Flight.ToggleCruiseControl" } // C
        data object AdjustSpeedLimiterUp : Flight() { override val commandString = "Flight.AdjustSpeedLimiterUp" } // MWU
        data object AdjustSpeedLimiterDown : Flight() { override val commandString = "Flight.AdjustSpeedLimiterDown" } // MWD
        data object ToggleVTOLMode : Flight() { override val commandString = "Flight.ToggleVTOLMode" } // K
        data object ToggleLockPitchYaw : Flight() { override val commandString = "Flight.ToggleLockPitchYaw" } // Right Shift (Often ESP Toggle)
        data object ToggleHeadLight : Flight() { override val commandString = "Flight.ToggleHeadLight" } // L
        data object ToggleSpeedLimiter : Flight() { override val commandString = "Flight.ToggleSpeedLimiter" } // middle click
        data object StrafeUp : Flight() { override val commandString = "Flight.StrafeUp" } // Space
        data object StrafeDown : Flight() { override val commandString = "Flight.StrafeDown" } // Left Ctrl
        data object StrafeLeft : Flight() { override val commandString = "Flight.StrafeLeft" } // A
        data object StrafeRight : Flight() { override val commandString = "Flight.StrafeRight" } // D
        data object StrafeForward : Flight() { override val commandString = "Flight.StrafeForward" } // W
        data object StrafeBackward : Flight() { override val commandString = "Flight.StrafeBackward" } // S
        data object RollLeft : Flight() { override val commandString = "Flight.RollLeft" } // Q
        data object RollRight : Flight() { override val commandString = "Flight.RollRight" } // E
    }

    sealed class QuantumTravel : Command() {
        data object ToggleQuantumMode : QuantumTravel() { override val commandString = "QuantumTravel.ToggleQuantumMode" } // Tap B (Spool)
        data object ActivateQuantumTravel : QuantumTravel() { override val commandString = "QuantumTravel.ActivateQuantumTravel" } // HOLD B (Engage)
        data object CalibrateQuantumDrive : QuantumTravel() { override val commandString = "QuantumTravel.CalibrateQuantumDrive" } // Tap B (While Spooled) - Same as Toggle? Verify in-game. Using Toggle for now.
        data object SetQuantumRoute : QuantumTravel() { override val commandString = "QuantumTravel.SetQuantumRoute" } // Requires manual binding or interaction mode
    }

    sealed class LandingAndDocking : Command() {
        data object ToggleLandingGear : LandingAndDocking() { override val commandString = "LandingAndDocking.ToggleLandingGear" } // N
        data object AutoLand : LandingAndDocking() { override val commandString = "LandingAndDocking.AutoLand" } // HOLD N
        data object RequestLandingTakeoff : LandingAndDocking() { override val commandString = "LandingAndDocking.RequestLandingTakeoff" } // Left Alt + N (ATC)
        data object RequestDocking : LandingAndDocking() { override val commandString = "LandingAndDocking.RequestDocking" } // N (while in Docking Mode) - Often same as Gear toggle contextually
        data object ToggleDockingCamera : LandingAndDocking() { override val commandString = "LandingAndDocking.ToggleDockingCamera" } // 0
    }

    sealed class PowerManagement : Command() {
        data object TogglePowerWeapons : PowerManagement() { override val commandString = "PowerManagement.TogglePowerWeapons" } // P or 1
        data object TogglePowerShields : PowerManagement() { override val commandString = "PowerManagement.TogglePowerShields" } // O or 2
        data object TogglePowerEngines : PowerManagement() { override val commandString = "PowerManagement.TogglePowerEngines" } // I or 3 (Previously Thrusters)
        data object TogglePowerAll : PowerManagement() { override val commandString = "PowerManagement.TogglePowerAll" } // U (Power On/Off)
        data object IncreasePowerWeapons : PowerManagement() { override val commandString = "PowerManagement.IncreasePowerWeapons" } // Tap F5
        data object MaxPowerWeapons : PowerManagement() { override val commandString = "PowerManagement.MaxPowerWeapons" } // HOLD F5
        data object IncreasePowerEngines : PowerManagement() { override val commandString = "PowerManagement.IncreasePowerEngines" } // Tap F6
        data object MaxPowerEngines : PowerManagement() { override val commandString = "PowerManagement.MaxPowerEngines" } // HOLD F6
        data object IncreasePowerShields : PowerManagement() { override val commandString = "PowerManagement.IncreasePowerShields" } // Tap F7
        data object MaxPowerShields : PowerManagement() { override val commandString = "PowerManagement.MaxPowerShields" } // HOLD F7
        data object ResetPowerDistribution : PowerManagement() { override val commandString = "PowerManagement.ResetPowerDistribution" } // F8
        data object DecreasePowerWeapons : PowerManagement() { override val commandString = "PowerManagement.DecreasePowerWeapons" } // Tap Left Alt + F5
        data object MinPowerWeapons : PowerManagement() { override val commandString = "PowerManagement.MinPowerWeapons" } // HOLD Left Alt + F5
        data object DecreasePowerEngines : PowerManagement() { override val commandString = "PowerManagement.DecreasePowerEngines" } // Tap Left Alt + F6
        data object MinPowerEngines : PowerManagement() { override val commandString = "PowerManagement.MinPowerEngines" } // HOLD Left Alt + F6
        data object DecreasePowerShields : PowerManagement() { override val commandString = "PowerManagement.DecreasePowerShields" } // Tap Left Alt + F7
        data object MinPowerShields : PowerManagement() { override val commandString = "PowerManagement.MinPowerShields" } // HOLD Left Alt + F7
        data object PowerTrianglePresetWeapons : PowerManagement() { override val commandString = "PowerManagement.PowerTrianglePresetWeapons" } // F5 (Map to MaxPowerWeapons?)
        data object PowerTrianglePresetEngines : PowerManagement() { override val commandString = "PowerManagement.PowerTrianglePresetEngines" } // F6 (Map to MaxPowerEngines?)
        data object PowerTrianglePresetShields : PowerManagement() { override val commandString = "PowerManagement.PowerTrianglePresetShields" } // F7 (Map to MaxPowerShields?)
        data object PowerTriangleReset : PowerManagement() { override val commandString = "PowerManagement.PowerTriangleReset" } // F8 (Map to ResetPowerDistribution)
    }

    sealed class Targeting : Command() {
        data object LockSelectedTarget : Targeting() { override val commandString = "Targeting.LockSelectedTarget" } // Tap T
        data object UnlockLockedTarget : Targeting() { override val commandString = "Targeting.UnlockLockedTarget" } // Left Alt + T
        data object CycleLockHostilesNext : Targeting() { override val commandString = "Targeting.CycleLockHostilesNext" } // Tap 5
        data object CycleLockHostilesClosest : Targeting() { override val commandString = "Targeting.CycleLockHostilesClosest" } // Hold 5
        data object CycleLockFriendliesNext : Targeting() { override val commandString = "Targeting.CycleLockFriendliesNext" } // Tap 6
        data object CycleLockFriendliesClosest : Targeting() { override val commandString = "Targeting.CycleLockFriendliesClosest" } // Hold 6
        data object CycleLockAllNext : Targeting() { override val commandString = "Targeting.CycleLockAllNext" } // Tap 7
        data object CycleLockAllClosest : Targeting() { override val commandString = "Targeting.CycleLockAllClosest" } // Hold 7
        data object CycleLockAttackersNext : Targeting() { override val commandString = "Targeting.CycleLockAttackersNext" } // Tap 4
        data object CycleLockAttackersClosest : Targeting() { override val commandString = "Targeting.CycleLockAttackersClosest" } // Hold 4
        data object CycleLockSubtargetsNext : Targeting() { override val commandString = "Targeting.CycleLockSubtargetsNext" } // Tap R (Often bound to TGT Cycle Fwd)
        data object ResetSubtargetToMain : Targeting() { override val commandString = "Targeting.ResetSubtargetToMain" } // Left Alt + R (Often bound to TGT Cycle Reset)
        data object PinTarget1 : Targeting() { override val commandString = "Targeting.PinTarget1" } // Left Alt + 1
        data object PinTarget2 : Targeting() { override val commandString = "Targeting.PinTarget2" } // Left Alt + 2
        data object PinTarget3 : Targeting() { override val commandString = "Targeting.PinTarget3" } // Left Alt + 3
        data object RemoveAllPinnedTargets : Targeting() { override val commandString = "Targeting.RemoveAllPinnedTargets" } // 0 (or Left Alt + 0)
        data object ToggleLookAhead : Targeting() { override val commandString = "Targeting.ToggleLookAhead" } // Left Alt + L (Often Look Behind?) - Verify
        data object TargetNearestHostile : Targeting() { override val commandString = "Targeting.TargetNearestHostile" } // Tap 5 (Map to CycleLockHostilesNext?)
        data object CycleTargetsForward : Targeting() { override val commandString = "Targeting.CycleTargetsForward" } // T (Map to LockSelectedTarget?)
        data object CycleTargetsBackward : Targeting() { override val commandString = "Targeting.CycleTargetsBackward" } // Y (Often unbound, requires manual binding)
        data object CycleSubtargetsForward : Targeting() { override val commandString = "Targeting.CycleSubtargetsForward" } // R (Map to CycleLockSubtargetsNext)
        data object CycleSubtargetsBackward : Targeting() { override val commandString = "Targeting.CycleSubtargetsBackward" } // Left Alt + R (Map to ResetSubtargetToMain?) - Verify
        data object PinSelectedTarget : Targeting() { override val commandString = "Targeting.PinSelectedTarget" } // Left Alt + 1 (Map to PinTarget1)
        data object UnpinSelectedTarget : Targeting() { override val commandString = "Targeting.UnpinSelectedTarget" } // Left Alt + 0 (Map to RemoveAllPinnedTargets?)
    }

    sealed class CombatPilot : Command() {
        data object FireWeaponGroup1 : CombatPilot() { override val commandString = "CombatPilot.FireWeaponGroup1" } // LMB
        data object FireWeaponGroup2 : CombatPilot() { override val commandString = "CombatPilot.FireWeaponGroup2" } // RMB
        data object CycleGimbalMode : CombatPilot() { override val commandString = "CombatPilot.CycleGimbalMode" } // Long Press G (or Tap G)
        data object ToggleMissileOperatorMode : CombatPilot() { override val commandString = "CombatPilot.ToggleMissileOperatorMode" } // MMB
        data object LaunchMissile : CombatPilot() { override val commandString = "CombatPilot.LaunchMissile" } // LMB (in Missile Mode)
        data object CycleMissileType : CombatPilot() { override val commandString = "CombatPilot.CycleMissileType" } // Mouse Wheel (in Missile Mode) or G
        data object IncreaseArmedMissiles : CombatPilot() { override val commandString = "CombatPilot.IncreaseArmedMissiles" } // Tap G
        data object ResetArmedMissiles : CombatPilot() { override val commandString = "CombatPilot.ResetArmedMissiles" } // Left Alt + G
        data object CycleFireMode : CombatPilot() { override val commandString = "CombatPilot.CycleFireMode" } // V (Often unbound, requires manual binding)
    }

    sealed class Countermeasures : Command() {
        data object DeployDecoyPanic : Countermeasures() { override val commandString = "Countermeasures.DeployDecoyPanic" } // Tap H (Often just Launch Decoy)
        data object DeployDecoyBurst : Countermeasures() { override val commandString = "Countermeasures.DeployDecoyBurst" } // Tap H (Often just Launch Decoy)
        data object SetLaunchDecoyBurst : Countermeasures() { override val commandString = "Countermeasures.SetLaunchDecoyBurst" } // Hold H (Often just Launch Decoy)
        data object IncreaseDecoyBurstSize : Countermeasures() { override val commandString = "Countermeasures.IncreaseDecoyBurstSize" } // Right Alt + H
        data object DecreaseDecoyBurstSize : Countermeasures() { override val commandString = "Countermeasures.DecreaseDecoyBurstSize" } // Left Alt + H
        data object DeployNoise : Countermeasures() { override val commandString = "Countermeasures.DeployNoise" } // J
        data object LaunchDecoy : Countermeasures() { override val commandString = "Countermeasures.LaunchDecoy" } // H
        data object LaunchNoise : Countermeasures() { override val commandString = "Countermeasures.LaunchNoise" } // J (Map to DeployNoise)
    }

    sealed class Scanning : Command() {
        data object ToggleScanningMode : Scanning() { override val commandString = "Scanning.ToggleScanningMode" } // V
        data object ActivatePing : Scanning() { override val commandString = "Scanning.ActivatePing" } // TAB
        data object ActivateScanTargeted : Scanning() { override val commandString = "Scanning.ActivateScanTargeted" } // LMB (in Scan Mode)
        data object IncreaseScanAngleFocus : Scanning() { override val commandString = "Scanning.IncreaseScanAngleFocus" } // MWU
        data object DecreaseScanAngleFocus : Scanning() { override val commandString = "Scanning.DecreaseScanAngleFocus" } // MWD
    }

    sealed class GeneralCockpit : Command() {
        data object FlightReady : GeneralCockpit() { override val commandString = "GeneralCockpit.FlightReady" } // R
        data object ExitSeat : GeneralCockpit() { override val commandString = "GeneralCockpit.ExitSeat" } // Hold Y
        data object Eject : GeneralCockpit() { override val commandString = "GeneralCockpit.Eject" } // Right Alt + Y
        data object EmergencyExitSeat : GeneralCockpit() { override val commandString = "GeneralCockpit.EmergencyExitSeat" } // U + Left Shift (Rarely used on panels)
        data object SelfDestruct : GeneralCockpit() { override val commandString = "GeneralCockpit.SelfDestruct" } // Hold Backspace
        data object TogglePortLockAll : GeneralCockpit() { override val commandString = "GeneralCockpit.TogglePortLockAll" } // Right Alt + K (Lock Vehicle)
        data object ToggleAllDoors : GeneralCockpit() { override val commandString = "GeneralCockpit.ToggleAllDoors" } // Requires manual binding (Often just 'Doors')
        data object ToggleLockAllDoors : GeneralCockpit() { override val commandString = "GeneralCockpit.ToggleLockAllDoors" } // Requires manual binding (Often just 'Lock')
        data object OpenAllDoors : GeneralCockpit() { override val commandString = "GeneralCockpit.OpenAllDoors" } // Map to ToggleAllDoors?
        data object CloseAllDoors : GeneralCockpit() { override val commandString = "GeneralCockpit.CloseAllDoors" } // Map to ToggleAllDoors?
        data object LockDoors : GeneralCockpit() { override val commandString = "GeneralCockpit.LockDoors" } // Map to TogglePortLockAll?
        data object UnlockDoors : GeneralCockpit() { override val commandString = "GeneralCockpit.UnlockDoors" } // Map to TogglePortLockAll?
    }

    sealed class ShipSalvage : Command() {
        data object ToggleSalvageMode : ShipSalvage() { override val commandString = "ShipSalvage.ToggleSalvageMode" } // M
        data object ToggleSalvageGimbal : ShipSalvage() { override val commandString = "ShipSalvage.ToggleSalvageGimbal" } // G
        data object ResetSalvageGimbal : ShipSalvage() { override val commandString = "ShipSalvage.ResetSalvageGimbal" } // Left Alt + G
        data object ToggleSalvageBeamFocused : ShipSalvage() { override val commandString = "ShipSalvage.ToggleSalvageBeamFocused" } // LMB (Toggle)
        data object ToggleSalvageBeamLeft : ShipSalvage() { override val commandString = "ShipSalvage.ToggleSalvageBeamLeft" } // Right Alt + A (Toggle)
        data object ToggleSalvageBeamRight : ShipSalvage() { override val commandString = "ShipSalvage.ToggleSalvageBeamRight" } // Right Alt + D (Toggle)
        data object ToggleSalvageBeamFracture : ShipSalvage() { override val commandString = "ShipSalvage.ToggleSalvageBeamFracture" } // Right Alt + W (Toggle)
        data object ToggleSalvageBeamDisintegrate : ShipSalvage() { override val commandString = "ShipSalvage.ToggleSalvageBeamDisintegrate" } // Right Alt + S (Toggle)
        data object CycleSalvageModifiers : ShipSalvage() { override val commandString = "ShipSalvage.CycleSalvageModifiers" } // RMB
        data object AdjustSalvageBeamSpacingUp : ShipSalvage() { override val commandString = "ShipSalvage.AdjustSalvageBeamSpacingUp" } // MWU
        data object AdjustSalvageBeamSpacingDown : ShipSalvage() { override val commandString = "ShipSalvage.AdjustSalvageBeamSpacingDown" } // MWD
        data object ToggleSalvageBeamAxis : ShipSalvage() { override val commandString = "ShipSalvage.ToggleSalvageBeamAxis" } // Left Alt + RMB
        data object FocusSalvageHeadsAll : ShipSalvage() { override val commandString = "ShipSalvage.FocusSalvageHeadsAll" } // Left Alt + S
        data object FocusSalvageHeadLeft : ShipSalvage() { override val commandString = "ShipSalvage.FocusSalvageHeadLeft" } // Left Alt + A
        data object FocusSalvageHeadRight : ShipSalvage() { override val commandString = "ShipSalvage.FocusSalvageHeadRight" } // Left Alt + D
        data object FocusSalvageToolFracture : ShipSalvage() { override val commandString = "ShipSalvage.FocusSalvageToolFracture" } // Left Alt + W
        data object IncreaseSalvageBeamSpacing : ShipSalvage() { override val commandString = "ShipSalvage.IncreaseSalvageBeamSpacing" } // Requires manual binding
        data object DecreaseSalvageBeamSpacing : ShipSalvage() { override val commandString = "ShipSalvage.DecreaseSalvageBeamSpacing" } // Requires manual binding
        data object SetAbsoluteSalvageBeamSpacing : ShipSalvage() { override val commandString = "ShipSalvage.SetAbsoluteSalvageBeamSpacing" } // Requires manual binding
    }

    sealed class ShipMining : Command() {
        data object ToggleMiningMode : ShipMining() { override val commandString = "ShipMining.ToggleMiningMode" } // M
        data object FireMiningLaser : ShipMining() { override val commandString = "ShipMining.FireMiningLaser" } // LMB
        data object SwitchMiningLaser : ShipMining() { override val commandString = "ShipMining.SwitchMiningLaser" } // Left Alt + LMB
        data object IncreaseMiningLaserPower : ShipMining() { override val commandString = "ShipMining.IncreaseMiningLaserPower" } // MWU
        data object DecreaseMiningLaserPower : ShipMining() { override val commandString = "ShipMining.DecreaseMiningLaserPower" } // MWD
        data object CycleMiningLaserGimbal : ShipMining() { override val commandString = "ShipMining.CycleMiningLaserGimbal" } // G
        data object ActivateMiningConsumable1 : ShipMining() { override val commandString = "ShipMining.ActivateMiningConsumable1" } // Left Alt + 1
        data object ActivateMiningConsumable2 : ShipMining() { override val commandString = "ShipMining.ActivateMiningConsumable2" } // Left Alt + 2
        data object ActivateMiningConsumable3 : ShipMining() { override val commandString = "ShipMining.ActivateMiningConsumable3" } // Left Alt + 3
        data object JettisonCargo : ShipMining() { override val commandString = "ShipMining.JettisonCargo" } // Left Alt + J
    }

    sealed class Turret : Command() {
        data object ToggleTurretAimMode : Turret() { override val commandString = "Turret.ToggleTurretAimMode" } // Q
        data object RecenterTurret : Turret() { override val commandString = "Turret.RecenterTurret" } // Hold C
        data object ChangeTurretPosition : Turret() { override val commandString = "Turret.ChangeTurretPosition" } // S
        data object AdjustTurretSpeedLimiterUp : Turret() { override val commandString = "Turret.AdjustTurretSpeedLimiterUp" } // Left Alt + MWU
        data object AdjustTurretSpeedLimiterDown : Turret() { override val commandString = "Turret.AdjustTurretSpeedLimiterDown" } // Left Alt + MWD
        data object FireTurretWeapons : Turret() { override val commandString = "Turret.FireTurretWeapons" } // LMB
        data object CycleTurretFireMode : Turret() { override val commandString = "Turret.CycleTurretFireMode" } // Requires manual binding
        data object ToggleTurretPrecisionTargeting : Turret() { override val commandString = "Turret.ToggleTurretPrecisionTargeting" } // Tap RMB
        data object ZoomTurretPrecisionTargeting : Turret() { override val commandString = "Turret.ZoomTurretPrecisionTargeting" } // Hold RMB
        data object CycleTurretPrecisionMode : Turret() { override val commandString = "Turret.CycleTurretPrecisionMode" } // Right Alt + RMB
        data object ToggleTurretGyroStabilization : Turret() { override val commandString = "Turret.ToggleTurretGyroStabilization" } // E
        data object SwitchToNextRemoteTurret : Turret() { override val commandString = "Turret.SwitchToNextRemoteTurret" } // D
        data object SwitchToPreviousRemoteTurret : Turret() { override val commandString = "Turret.SwitchToPreviousRemoteTurret" } // A
        data object ExitTurret : Turret() { override val commandString = "Turret.ExitTurret" } // Hold Y
        data object ToggleTurretESP : Turret() { override val commandString = "Turret.ToggleTurretESP" } // Requires manual binding
    }
}

// --- Top-level Helper Function for Lookup ---
// (This function remains the same as before, but now uses the overridden commandString)
/**
 * Finds and returns a Command object based on its unique commandString.
 * Uses a when statement for lookup, avoiding static initialization issues.
 *
 * @param commandId The unique string identifier for the command (e.g., "Flight.Boost").
 * @return The corresponding Command object instance, or null if not found.
 */
fun getCommandFromString(commandId: String): Command? {
    return when (commandId) {
        // Flight
        Command.Flight.Boost.commandString -> Command.Flight.Boost
        Command.Flight.Spacebrake.commandString -> Command.Flight.Spacebrake
        Command.Flight.ToggleDecoupledMode.commandString -> Command.Flight.ToggleDecoupledMode
        Command.Flight.ToggleCruiseControl.commandString -> Command.Flight.ToggleCruiseControl
        Command.Flight.AdjustSpeedLimiterUp.commandString -> Command.Flight.AdjustSpeedLimiterUp
        Command.Flight.AdjustSpeedLimiterDown.commandString -> Command.Flight.AdjustSpeedLimiterDown
        Command.Flight.ToggleVTOLMode.commandString -> Command.Flight.ToggleVTOLMode
        Command.Flight.ToggleLockPitchYaw.commandString -> Command.Flight.ToggleLockPitchYaw
        Command.Flight.ToggleHeadLight.commandString -> Command.Flight.ToggleHeadLight
        Command.Flight.ToggleSpeedLimiter.commandString -> Command.Flight.ToggleSpeedLimiter
        Command.Flight.StrafeUp.commandString -> Command.Flight.StrafeUp
        Command.Flight.StrafeDown.commandString -> Command.Flight.StrafeDown
        Command.Flight.StrafeLeft.commandString -> Command.Flight.StrafeLeft
        Command.Flight.StrafeRight.commandString -> Command.Flight.StrafeRight
        Command.Flight.StrafeForward.commandString -> Command.Flight.StrafeForward
        Command.Flight.StrafeBackward.commandString -> Command.Flight.StrafeBackward
        Command.Flight.RollLeft.commandString -> Command.Flight.RollLeft
        Command.Flight.RollRight.commandString -> Command.Flight.RollRight
        // QuantumTravel
        Command.QuantumTravel.ToggleQuantumMode.commandString -> Command.QuantumTravel.ToggleQuantumMode
        Command.QuantumTravel.ActivateQuantumTravel.commandString -> Command.QuantumTravel.ActivateQuantumTravel
        Command.QuantumTravel.CalibrateQuantumDrive.commandString -> Command.QuantumTravel.CalibrateQuantumDrive
        Command.QuantumTravel.SetQuantumRoute.commandString -> Command.QuantumTravel.SetQuantumRoute
        // LandingAndDocking
        Command.LandingAndDocking.ToggleLandingGear.commandString -> Command.LandingAndDocking.ToggleLandingGear
        Command.LandingAndDocking.AutoLand.commandString -> Command.LandingAndDocking.AutoLand
        Command.LandingAndDocking.RequestLandingTakeoff.commandString -> Command.LandingAndDocking.RequestLandingTakeoff
        Command.LandingAndDocking.RequestDocking.commandString -> Command.LandingAndDocking.RequestDocking
        Command.LandingAndDocking.ToggleDockingCamera.commandString -> Command.LandingAndDocking.ToggleDockingCamera
        // PowerManagement
        Command.PowerManagement.TogglePowerWeapons.commandString -> Command.PowerManagement.TogglePowerWeapons
        Command.PowerManagement.TogglePowerShields.commandString -> Command.PowerManagement.TogglePowerShields
        Command.PowerManagement.TogglePowerEngines.commandString -> Command.PowerManagement.TogglePowerEngines
        Command.PowerManagement.TogglePowerAll.commandString -> Command.PowerManagement.TogglePowerAll
        Command.PowerManagement.IncreasePowerWeapons.commandString -> Command.PowerManagement.IncreasePowerWeapons
        Command.PowerManagement.MaxPowerWeapons.commandString -> Command.PowerManagement.MaxPowerWeapons
        Command.PowerManagement.IncreasePowerEngines.commandString -> Command.PowerManagement.IncreasePowerEngines
        Command.PowerManagement.MaxPowerEngines.commandString -> Command.PowerManagement.MaxPowerEngines
        Command.PowerManagement.IncreasePowerShields.commandString -> Command.PowerManagement.IncreasePowerShields
        Command.PowerManagement.MaxPowerShields.commandString -> Command.PowerManagement.MaxPowerShields
        Command.PowerManagement.ResetPowerDistribution.commandString -> Command.PowerManagement.ResetPowerDistribution
        Command.PowerManagement.DecreasePowerWeapons.commandString -> Command.PowerManagement.DecreasePowerWeapons
        Command.PowerManagement.MinPowerWeapons.commandString -> Command.PowerManagement.MinPowerWeapons
        Command.PowerManagement.DecreasePowerEngines.commandString -> Command.PowerManagement.DecreasePowerEngines
        Command.PowerManagement.MinPowerEngines.commandString -> Command.PowerManagement.MinPowerEngines
        Command.PowerManagement.DecreasePowerShields.commandString -> Command.PowerManagement.DecreasePowerShields
        Command.PowerManagement.MinPowerShields.commandString -> Command.PowerManagement.MinPowerShields
        Command.PowerManagement.PowerTrianglePresetWeapons.commandString -> Command.PowerManagement.PowerTrianglePresetWeapons
        Command.PowerManagement.PowerTrianglePresetEngines.commandString -> Command.PowerManagement.PowerTrianglePresetEngines
        Command.PowerManagement.PowerTrianglePresetShields.commandString -> Command.PowerManagement.PowerTrianglePresetShields
        Command.PowerManagement.PowerTriangleReset.commandString -> Command.PowerManagement.PowerTriangleReset
        // Targeting
        Command.Targeting.LockSelectedTarget.commandString -> Command.Targeting.LockSelectedTarget
        Command.Targeting.UnlockLockedTarget.commandString -> Command.Targeting.UnlockLockedTarget
        Command.Targeting.CycleLockHostilesNext.commandString -> Command.Targeting.CycleLockHostilesNext
        Command.Targeting.CycleLockHostilesClosest.commandString -> Command.Targeting.CycleLockHostilesClosest
        Command.Targeting.CycleLockFriendliesNext.commandString -> Command.Targeting.CycleLockFriendliesNext
        Command.Targeting.CycleLockFriendliesClosest.commandString -> Command.Targeting.CycleLockFriendliesClosest
        Command.Targeting.CycleLockAllNext.commandString -> Command.Targeting.CycleLockAllNext
        Command.Targeting.CycleLockAllClosest.commandString -> Command.Targeting.CycleLockAllClosest
        Command.Targeting.CycleLockAttackersNext.commandString -> Command.Targeting.CycleLockAttackersNext
        Command.Targeting.CycleLockAttackersClosest.commandString -> Command.Targeting.CycleLockAttackersClosest
        Command.Targeting.CycleLockSubtargetsNext.commandString -> Command.Targeting.CycleLockSubtargetsNext
        Command.Targeting.ResetSubtargetToMain.commandString -> Command.Targeting.ResetSubtargetToMain
        Command.Targeting.PinTarget1.commandString -> Command.Targeting.PinTarget1
        Command.Targeting.PinTarget2.commandString -> Command.Targeting.PinTarget2
        Command.Targeting.PinTarget3.commandString -> Command.Targeting.PinTarget3
        Command.Targeting.RemoveAllPinnedTargets.commandString -> Command.Targeting.RemoveAllPinnedTargets
        Command.Targeting.ToggleLookAhead.commandString -> Command.Targeting.ToggleLookAhead
        Command.Targeting.TargetNearestHostile.commandString -> Command.Targeting.TargetNearestHostile
        Command.Targeting.CycleTargetsForward.commandString -> Command.Targeting.CycleTargetsForward
        Command.Targeting.CycleTargetsBackward.commandString -> Command.Targeting.CycleTargetsBackward
        Command.Targeting.CycleSubtargetsForward.commandString -> Command.Targeting.CycleSubtargetsForward
        Command.Targeting.CycleSubtargetsBackward.commandString -> Command.Targeting.CycleSubtargetsBackward
        Command.Targeting.PinSelectedTarget.commandString -> Command.Targeting.PinSelectedTarget
        Command.Targeting.UnpinSelectedTarget.commandString -> Command.Targeting.UnpinSelectedTarget
        // CombatPilot
        Command.CombatPilot.FireWeaponGroup1.commandString -> Command.CombatPilot.FireWeaponGroup1
        Command.CombatPilot.FireWeaponGroup2.commandString -> Command.CombatPilot.FireWeaponGroup2
        Command.CombatPilot.CycleGimbalMode.commandString -> Command.CombatPilot.CycleGimbalMode
        Command.CombatPilot.ToggleMissileOperatorMode.commandString -> Command.CombatPilot.ToggleMissileOperatorMode
        Command.CombatPilot.LaunchMissile.commandString -> Command.CombatPilot.LaunchMissile
        Command.CombatPilot.CycleMissileType.commandString -> Command.CombatPilot.CycleMissileType
        Command.CombatPilot.IncreaseArmedMissiles.commandString -> Command.CombatPilot.IncreaseArmedMissiles
        Command.CombatPilot.ResetArmedMissiles.commandString -> Command.CombatPilot.ResetArmedMissiles
        Command.CombatPilot.CycleFireMode.commandString -> Command.CombatPilot.CycleFireMode
        // Countermeasures
        Command.Countermeasures.DeployDecoyPanic.commandString -> Command.Countermeasures.DeployDecoyPanic
        Command.Countermeasures.DeployDecoyBurst.commandString -> Command.Countermeasures.DeployDecoyBurst
        Command.Countermeasures.SetLaunchDecoyBurst.commandString -> Command.Countermeasures.SetLaunchDecoyBurst
        Command.Countermeasures.IncreaseDecoyBurstSize.commandString -> Command.Countermeasures.IncreaseDecoyBurstSize
        Command.Countermeasures.DecreaseDecoyBurstSize.commandString -> Command.Countermeasures.DecreaseDecoyBurstSize
        Command.Countermeasures.DeployNoise.commandString -> Command.Countermeasures.DeployNoise
        Command.Countermeasures.LaunchDecoy.commandString -> Command.Countermeasures.LaunchDecoy
        Command.Countermeasures.LaunchNoise.commandString -> Command.Countermeasures.LaunchNoise
        // Scanning
        Command.Scanning.ToggleScanningMode.commandString -> Command.Scanning.ToggleScanningMode
        Command.Scanning.ActivatePing.commandString -> Command.Scanning.ActivatePing
        Command.Scanning.ActivateScanTargeted.commandString -> Command.Scanning.ActivateScanTargeted
        Command.Scanning.IncreaseScanAngleFocus.commandString -> Command.Scanning.IncreaseScanAngleFocus
        Command.Scanning.DecreaseScanAngleFocus.commandString -> Command.Scanning.DecreaseScanAngleFocus
        // GeneralCockpit
        Command.GeneralCockpit.FlightReady.commandString -> Command.GeneralCockpit.FlightReady
        Command.GeneralCockpit.ExitSeat.commandString -> Command.GeneralCockpit.ExitSeat
        Command.GeneralCockpit.Eject.commandString -> Command.GeneralCockpit.Eject
        Command.GeneralCockpit.EmergencyExitSeat.commandString -> Command.GeneralCockpit.EmergencyExitSeat
        Command.GeneralCockpit.SelfDestruct.commandString -> Command.GeneralCockpit.SelfDestruct
        Command.GeneralCockpit.TogglePortLockAll.commandString -> Command.GeneralCockpit.TogglePortLockAll
        Command.GeneralCockpit.ToggleAllDoors.commandString -> Command.GeneralCockpit.ToggleAllDoors
        Command.GeneralCockpit.ToggleLockAllDoors.commandString -> Command.GeneralCockpit.ToggleLockAllDoors
        Command.GeneralCockpit.OpenAllDoors.commandString -> Command.GeneralCockpit.OpenAllDoors
        Command.GeneralCockpit.CloseAllDoors.commandString -> Command.GeneralCockpit.CloseAllDoors
        Command.GeneralCockpit.LockDoors.commandString -> Command.GeneralCockpit.LockDoors
        Command.GeneralCockpit.UnlockDoors.commandString -> Command.GeneralCockpit.UnlockDoors
        // ShipSalvage
        Command.ShipSalvage.ToggleSalvageMode.commandString -> Command.ShipSalvage.ToggleSalvageMode
        Command.ShipSalvage.ToggleSalvageGimbal.commandString -> Command.ShipSalvage.ToggleSalvageGimbal
        Command.ShipSalvage.ResetSalvageGimbal.commandString -> Command.ShipSalvage.ResetSalvageGimbal
        Command.ShipSalvage.ToggleSalvageBeamFocused.commandString -> Command.ShipSalvage.ToggleSalvageBeamFocused
        Command.ShipSalvage.ToggleSalvageBeamLeft.commandString -> Command.ShipSalvage.ToggleSalvageBeamLeft
        Command.ShipSalvage.ToggleSalvageBeamRight.commandString -> Command.ShipSalvage.ToggleSalvageBeamRight
        Command.ShipSalvage.ToggleSalvageBeamFracture.commandString -> Command.ShipSalvage.ToggleSalvageBeamFracture
        Command.ShipSalvage.ToggleSalvageBeamDisintegrate.commandString -> Command.ShipSalvage.ToggleSalvageBeamDisintegrate
        Command.ShipSalvage.CycleSalvageModifiers.commandString -> Command.ShipSalvage.CycleSalvageModifiers
        Command.ShipSalvage.AdjustSalvageBeamSpacingUp.commandString -> Command.ShipSalvage.AdjustSalvageBeamSpacingUp
        Command.ShipSalvage.AdjustSalvageBeamSpacingDown.commandString -> Command.ShipSalvage.AdjustSalvageBeamSpacingDown
        Command.ShipSalvage.ToggleSalvageBeamAxis.commandString -> Command.ShipSalvage.ToggleSalvageBeamAxis
        Command.ShipSalvage.FocusSalvageHeadsAll.commandString -> Command.ShipSalvage.FocusSalvageHeadsAll
        Command.ShipSalvage.FocusSalvageHeadLeft.commandString -> Command.ShipSalvage.FocusSalvageHeadLeft
        Command.ShipSalvage.FocusSalvageHeadRight.commandString -> Command.ShipSalvage.FocusSalvageHeadRight
        Command.ShipSalvage.FocusSalvageToolFracture.commandString -> Command.ShipSalvage.FocusSalvageToolFracture
        Command.ShipSalvage.IncreaseSalvageBeamSpacing.commandString -> Command.ShipSalvage.IncreaseSalvageBeamSpacing
        Command.ShipSalvage.DecreaseSalvageBeamSpacing.commandString -> Command.ShipSalvage.DecreaseSalvageBeamSpacing
        Command.ShipSalvage.SetAbsoluteSalvageBeamSpacing.commandString -> Command.ShipSalvage.SetAbsoluteSalvageBeamSpacing
        // ShipMining
        Command.ShipMining.ToggleMiningMode.commandString -> Command.ShipMining.ToggleMiningMode
        Command.ShipMining.FireMiningLaser.commandString -> Command.ShipMining.FireMiningLaser
        Command.ShipMining.SwitchMiningLaser.commandString -> Command.ShipMining.SwitchMiningLaser
        Command.ShipMining.IncreaseMiningLaserPower.commandString -> Command.ShipMining.IncreaseMiningLaserPower
        Command.ShipMining.DecreaseMiningLaserPower.commandString -> Command.ShipMining.DecreaseMiningLaserPower
        Command.ShipMining.CycleMiningLaserGimbal.commandString -> Command.ShipMining.CycleMiningLaserGimbal
        Command.ShipMining.ActivateMiningConsumable1.commandString -> Command.ShipMining.ActivateMiningConsumable1
        Command.ShipMining.ActivateMiningConsumable2.commandString -> Command.ShipMining.ActivateMiningConsumable2
        Command.ShipMining.ActivateMiningConsumable3.commandString -> Command.ShipMining.ActivateMiningConsumable3
        Command.ShipMining.JettisonCargo.commandString -> Command.ShipMining.JettisonCargo
        // Turret
        Command.Turret.ToggleTurretAimMode.commandString -> Command.Turret.ToggleTurretAimMode
        Command.Turret.RecenterTurret.commandString -> Command.Turret.RecenterTurret
        Command.Turret.ChangeTurretPosition.commandString -> Command.Turret.ChangeTurretPosition
        Command.Turret.AdjustTurretSpeedLimiterUp.commandString -> Command.Turret.AdjustTurretSpeedLimiterUp
        Command.Turret.AdjustTurretSpeedLimiterDown.commandString -> Command.Turret.AdjustTurretSpeedLimiterDown
        Command.Turret.FireTurretWeapons.commandString -> Command.Turret.FireTurretWeapons
        Command.Turret.CycleTurretFireMode.commandString -> Command.Turret.CycleTurretFireMode
        Command.Turret.ToggleTurretPrecisionTargeting.commandString -> Command.Turret.ToggleTurretPrecisionTargeting
        Command.Turret.ZoomTurretPrecisionTargeting.commandString -> Command.Turret.ZoomTurretPrecisionTargeting
        Command.Turret.CycleTurretPrecisionMode.commandString -> Command.Turret.CycleTurretPrecisionMode
        Command.Turret.ToggleTurretGyroStabilization.commandString -> Command.Turret.ToggleTurretGyroStabilization
        Command.Turret.SwitchToNextRemoteTurret.commandString -> Command.Turret.SwitchToNextRemoteTurret
        Command.Turret.SwitchToPreviousRemoteTurret.commandString -> Command.Turret.SwitchToPreviousRemoteTurret
        Command.Turret.ExitTurret.commandString -> Command.Turret.ExitTurret
        Command.Turret.ToggleTurretESP.commandString -> Command.Turret.ToggleTurretESP
        // Add mappings for any new Command objects here...
        else -> null // Return null if commandId doesn't match any known command
    }
}
