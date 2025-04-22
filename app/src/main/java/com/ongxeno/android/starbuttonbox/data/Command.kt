package com.ongxeno.android.starbuttonbox.data

/**
 * Defines command strings for Star Citizen actions using a sealed class hierarchy.
 * Each nested data object represents a specific command and holds its string value.
 * Based on Star Citizen Alpha 3.18+ default KBM keybindings and common unbound actions.
 */
sealed class Command(val commandString: String) {

    sealed class Flight(commandString: String) : Command(commandString) {
        data object Boost : Flight("Boost") // HOLD Left Shift
        data object Spacebrake : Flight("Spacebrake") // X
        data object ToggleDecoupledMode : Flight("ToggleDecoupledMode") // Left Alt + C
        data object ToggleCruiseControl : Flight("ToggleCruiseControl") // C
        data object AdjustSpeedLimiterUp : Flight("AdjustSpeedLimiterUp") // MWU
        data object AdjustSpeedLimiterDown : Flight("AdjustSpeedLimiterDown") // MWD
        data object ToggleVTOLMode : Flight("ToggleVTOLMode") // K
        data object ToggleLockPitchYaw : Flight("ToggleLockPitchYaw") // Right Shift (Often ESP Toggle)
        data object ToggleHeadLight : Flight("ToggleHeadLight") // L
        data object ToggleSpeedLimiter : Flight("ToggleSpeedLimiter") // middle click
        data object StrafeUp : Flight("StrafeUp") // Space
        data object StrafeDown : Flight("StrafeDown") // Left Ctrl
        data object StrafeLeft : Flight("StrafeLeft") // A
        data object StrafeRight : Flight("StrafeRight") // D
        data object StrafeForward : Flight("StrafeForward") // W
        data object StrafeBackward : Flight("StrafeBackward") // S
        data object RollLeft : Flight("RollLeft") // Q
        data object RollRight : Flight("RollRight") // E
    }

    sealed class QuantumTravel(commandString: String) : Command(commandString) {
        data object ToggleQuantumMode : QuantumTravel("ToggleQuantumMode") // Tap B (Spool)
        data object ActivateQuantumTravel : QuantumTravel("ActivateQuantumTravel") // HOLD B (Engage)
        data object CalibrateQuantumDrive : QuantumTravel("CalibrateQuantumDrive") // Tap B (While Spooled) - Same as Toggle? Verify in-game. Using Toggle for now.
        data object SetQuantumRoute : QuantumTravel("SetQuantumRoute") // Requires manual binding or interaction mode
    }

    sealed class LandingAndDocking(commandString: String) : Command(commandString) {
        data object ToggleLandingGear : LandingAndDocking("ToggleLandingGear") // N
        data object AutoLand : LandingAndDocking("AutoLand") // HOLD N
        data object RequestLandingTakeoff :
            LandingAndDocking("RequestLandingTakeoff") // Left Alt + N (ATC)

        data object RequestDocking :
            LandingAndDocking("RequestDocking") // N (while in Docking Mode) - Often same as Gear toggle contextually

        data object ToggleDockingCamera : LandingAndDocking("ToggleDockingCamera") // 0
    }

    sealed class PowerManagement(commandString: String) : Command(commandString) {
        data object TogglePowerWeapons : PowerManagement("TogglePowerWeapons") // P or 1
        data object TogglePowerShields : PowerManagement("TogglePowerShields") // O or 2
        data object TogglePowerEngines :
            PowerManagement("TogglePowerEngines") // I or 3 (Previously Thrusters)

        data object TogglePowerAll : PowerManagement("TogglePowerAll") // U (Power On/Off)
        data object IncreasePowerWeapons : PowerManagement("IncreasePowerWeapons") // Tap F5
        data object MaxPowerWeapons : PowerManagement("MaxPowerWeapons") // HOLD F5
        data object IncreasePowerEngines : PowerManagement("IncreasePowerEngines") // Tap F6
        data object MaxPowerEngines : PowerManagement("MaxPowerEngines") // HOLD F6
        data object IncreasePowerShields : PowerManagement("IncreasePowerShields") // Tap F7
        data object MaxPowerShields : PowerManagement("MaxPowerShields") // HOLD F7
        data object ResetPowerDistribution : PowerManagement("ResetPowerDistribution") // F8
        data object DecreasePowerWeapons :
            PowerManagement("DecreasePowerWeapons") // Tap Left Alt + F5

        data object MinPowerWeapons : PowerManagement("MinPowerWeapons") // HOLD Left Alt + F5
        data object DecreasePowerEngines :
            PowerManagement("DecreasePowerEngines") // Tap Left Alt + F6

        data object MinPowerEngines : PowerManagement("MinPowerEngines") // HOLD Left Alt + F6
        data object DecreasePowerShields :
            PowerManagement("DecreasePowerShields") // Tap Left Alt + F7

        data object MinPowerShields : PowerManagement("MinPowerShields") // HOLD Left Alt + F7

        data object PowerTrianglePresetWeapons : PowerManagement("PowerTrianglePresetWeapons") // F5 (Map to MaxPowerWeapons?)
        data object PowerTrianglePresetEngines : PowerManagement("PowerTrianglePresetEngines") // F6 (Map to MaxPowerEngines?)
        data object PowerTrianglePresetShields : PowerManagement("PowerTrianglePresetShields") // F7 (Map to MaxPowerShields?)
        data object PowerTriangleReset : PowerManagement("PowerTriangleReset") // F8 (Map to ResetPowerDistribution)

    }

    sealed class Targeting(commandString: String) : Command(commandString) {
        data object LockSelectedTarget : Targeting("LockSelectedTarget") // Tap T
        data object UnlockLockedTarget : Targeting("UnlockLockedTarget") // Left Alt + T
        data object CycleLockHostilesNext : Targeting("CycleLockHostilesNext") // Tap 5
        data object CycleLockHostilesClosest : Targeting("CycleLockHostilesClosest") // Hold 5
        data object CycleLockFriendliesNext : Targeting("CycleLockFriendliesNext") // Tap 6
        data object CycleLockFriendliesClosest : Targeting("CycleLockFriendliesClosest") // Hold 6
        data object CycleLockAllNext : Targeting("CycleLockAllNext") // Tap 7
        data object CycleLockAllClosest : Targeting("CycleLockAllClosest") // Hold 7
        data object CycleLockAttackersNext : Targeting("CycleLockAttackersNext") // Tap 4
        data object CycleLockAttackersClosest : Targeting("CycleLockAttackersClosest") // Hold 4
        data object CycleLockSubtargetsNext : Targeting("CycleLockSubtargetsNext") // Tap R (Often bound to TGT Cycle Fwd)
        data object ResetSubtargetToMain : Targeting("ResetSubtargetToMain") // Left Alt + R (Often bound to TGT Cycle Reset)
        data object PinTarget1 : Targeting("PinTarget1") // Left Alt + 1
        data object PinTarget2 : Targeting("PinTarget2") // Left Alt + 2
        data object PinTarget3 : Targeting("PinTarget3") // Left Alt + 3
        data object RemoveAllPinnedTargets : Targeting("RemoveAllPinnedTargets") // 0 (or Left Alt + 0)
        data object ToggleLookAhead : Targeting("ToggleLookAhead") // Left Alt + L (Often Look Behind?) - Verify

        data object TargetNearestHostile : Targeting("TargetNearestHostile") // Tap 5 (Map to CycleLockHostilesNext?)
        data object CycleTargetsForward : Targeting("CycleTargetsForward") // T (Map to LockSelectedTarget?)
        data object CycleTargetsBackward : Targeting("CycleTargetsBackward") // Y (Often unbound, requires manual binding)
        data object CycleSubtargetsForward : Targeting("CycleSubtargetsForward") // R (Map to CycleLockSubtargetsNext)
        data object CycleSubtargetsBackward : Targeting("CycleSubtargetsBackward") // Left Alt + R (Map to ResetSubtargetToMain?) - Verify
        data object PinSelectedTarget : Targeting("PinSelectedTarget") // Left Alt + 1 (Map to PinTarget1)
        data object UnpinSelectedTarget : Targeting("UnpinSelectedTarget") // Left Alt + 0 (Map to RemoveAllPinnedTargets?)

    }

    sealed class CombatPilot(commandString: String) : Command(commandString) {
        data object FireWeaponGroup1 : CombatPilot("FireWeaponGroup1") // LMB
        data object FireWeaponGroup2 : CombatPilot("FireWeaponGroup2") // RMB
        data object CycleGimbalMode : CombatPilot("CycleGimbalMode") // Long Press G (or Tap G)
        data object ToggleMissileOperatorMode : CombatPilot("ToggleMissileOperatorMode") // MMB
        data object LaunchMissile : CombatPilot("LaunchMissile") // LMB (in Missile Mode)
        data object CycleMissileType :
            CombatPilot("CycleMissileType") // Mouse Wheel (in Missile Mode) or G

        data object IncreaseArmedMissiles : CombatPilot("IncreaseArmedMissiles") // Tap G
        data object ResetArmedMissiles : CombatPilot("ResetArmedMissiles") // Left Alt + G

        data object CycleFireMode : CombatPilot("CycleFireMode") // V (Often unbound, requires manual binding)
    }

    sealed class Countermeasures(commandString: String) : Command(commandString) {
        data object DeployDecoyPanic : Countermeasures("DeployDecoyPanic") // Tap H (Often just Launch Decoy)
        data object DeployDecoyBurst : Countermeasures("DeployDecoyBurst") // Tap H (Often just Launch Decoy)
        data object SetLaunchDecoyBurst : Countermeasures("SetLaunchDecoyBurst") // Hold H (Often just Launch Decoy)
        data object IncreaseDecoyBurstSize :
            Countermeasures("IncreaseDecoyBurstSize") // Right Alt + H

        data object DecreaseDecoyBurstSize :
            Countermeasures("DecreaseDecoyBurstSize") // Left Alt + H

        data object DeployNoise : Countermeasures("DeployNoise") // J

        data object LaunchDecoy : Countermeasures("LaunchDecoy") // H
        data object LaunchNoise : Countermeasures("LaunchNoise") // J (Map to DeployNoise)
    }

    sealed class Scanning(commandString: String) : Command(commandString) {
        data object ToggleScanningMode : Scanning("ToggleScanningMode") // V
        data object ActivatePing : Scanning("ActivatePing") // TAB
        data object ActivateScanTargeted : Scanning("ActivateScanTargeted") // LMB (in Scan Mode)
        data object IncreaseScanAngleFocus : Scanning("IncreaseScanAngleFocus") // MWU
        data object DecreaseScanAngleFocus : Scanning("DecreaseScanAngleFocus") // MWD
    }

    sealed class GeneralCockpit(commandString: String) : Command(commandString) {
        data object FlightReady : GeneralCockpit("FlightReady") // R
        data object ExitSeat : GeneralCockpit("ExitSeat") // Hold Y
        data object Eject : GeneralCockpit("Eject") // Right Alt + Y
        data object EmergencyExitSeat : GeneralCockpit("EmergencyExitSeat") // U + Left Shift (Rarely used on panels)
        data object SelfDestruct : GeneralCockpit("SelfDestruct") // Hold Backspace
        data object TogglePortLockAll : GeneralCockpit("TogglePortLockAll") // Right Alt + K (Lock Vehicle)
        data object ToggleAllDoors : GeneralCockpit("ToggleAllDoors") // Requires manual binding (Often just 'Doors')
        data object ToggleLockAllDoors :
            GeneralCockpit("ToggleLockAllDoors") // Requires manual binding (Often just 'Lock')

        data object OpenAllDoors : GeneralCockpit("OpenAllDoors") // Map to ToggleAllDoors?
        data object CloseAllDoors : GeneralCockpit("CloseAllDoors") // Map to ToggleAllDoors?
        data object LockDoors : GeneralCockpit("LockDoors") // Map to TogglePortLockAll?
        data object UnlockDoors : GeneralCockpit("UnlockDoors") // Map to TogglePortLockAll?
    }

    sealed class ShipSalvage(commandString: String) : Command(commandString) {
        data object ToggleSalvageMode : ShipSalvage("ToggleSalvageMode") // M
        data object ToggleSalvageGimbal : ShipSalvage("ToggleSalvageGimbal") // G
        data object ResetSalvageGimbal : ShipSalvage("ResetSalvageGimbal") // Left Alt + G
        data object ToggleSalvageBeamFocused : ShipSalvage("ToggleSalvageBeamFocused") // LMB (Toggle)
        data object ToggleSalvageBeamLeft : ShipSalvage("ToggleSalvageBeamLeft") // Right Alt + A (Toggle)
        data object ToggleSalvageBeamRight : ShipSalvage("ToggleSalvageBeamRight") // Right Alt + D (Toggle)
        data object ToggleSalvageBeamFracture : ShipSalvage("ToggleSalvageBeamFracture") // Right Alt + W (Toggle)
        data object ToggleSalvageBeamDisintegrate : ShipSalvage("ToggleSalvageBeamDisintegrate") // Right Alt + S (Toggle)
        data object CycleSalvageModifiers : ShipSalvage("CycleSalvageModifiers") // RMB
        data object AdjustSalvageBeamSpacingUp : ShipSalvage("AdjustSalvageBeamSpacingUp") // MWU
        data object AdjustSalvageBeamSpacingDown : ShipSalvage("AdjustSalvageBeamSpacingDown") // MWD
        data object ToggleSalvageBeamAxis : ShipSalvage("ToggleSalvageBeamAxis") // Left Alt + RMB
        data object FocusSalvageHeadsAll : ShipSalvage("FocusSalvageHeadsAll") // Left Alt + S
        data object FocusSalvageHeadLeft : ShipSalvage("FocusSalvageHeadLeft") // Left Alt + A
        data object FocusSalvageHeadRight : ShipSalvage("FocusSalvageHeadRight") // Left Alt + D
        data object FocusSalvageToolFracture : ShipSalvage("FocusSalvageToolFracture") // Left Alt + W
        data object IncreaseSalvageBeamSpacing : ShipSalvage("IncreaseSalvageBeamSpacing") // Requires manual binding
        data object DecreaseSalvageBeamSpacing : ShipSalvage("DecreaseSalvageBeamSpacing") // Requires manual binding
        data object SetAbsoluteSalvageBeamSpacing : ShipSalvage("SetAbsoluteSalvageBeamSpacing") // Requires manual binding
    }

    sealed class ShipMining(commandString: String) : Command(commandString) {
        data object ToggleMiningMode : ShipMining("ToggleMiningMode") // M
        data object FireMiningLaser : ShipMining("FireMiningLaser") // LMB
        data object SwitchMiningLaser : ShipMining("SwitchMiningLaser") // Left Alt + LMB
        data object IncreaseMiningLaserPower : ShipMining("IncreaseMiningLaserPower") // MWU
        data object DecreaseMiningLaserPower : ShipMining("DecreaseMiningLaserPower") // MWD
        data object CycleMiningLaserGimbal : ShipMining("CycleMiningLaserGimbal") // G
        data object ActivateMiningConsumable1 : ShipMining("ActivateMiningConsumable1") // Left Alt + 1
        data object ActivateMiningConsumable2 : ShipMining("ActivateMiningConsumable2") // Left Alt + 2
        data object ActivateMiningConsumable3 : ShipMining("ActivateMiningConsumable3") // Left Alt + 3
        data object JettisonCargo : ShipMining("JettisonCargo") // Left Alt + J
    }

    sealed class Turret(commandString: String) : Command(commandString) {
        data object ToggleTurretAimMode : Turret("ToggleTurretAimMode") // Q
        data object RecenterTurret : Turret("RecenterTurret") // Hold C
        data object ChangeTurretPosition : Turret("ChangeTurretPosition") // S
        data object AdjustTurretSpeedLimiterUp : Turret("AdjustTurretSpeedLimiterUp") // Left Alt + MWU
        data object AdjustTurretSpeedLimiterDown : Turret("AdjustTurretSpeedLimiterDown") // Left Alt + MWD
        data object FireTurretWeapons : Turret("FireTurretWeapons") // LMB
        data object CycleTurretFireMode : Turret("CycleTurretFireMode") // Requires manual binding
        data object ToggleTurretPrecisionTargeting : Turret("ToggleTurretPrecisionTargeting") // Tap RMB
        data object ZoomTurretPrecisionTargeting : Turret("ZoomTurretPrecisionTargeting") // Hold RMB
        data object CycleTurretPrecisionMode : Turret("CycleTurretPrecisionMode") // Right Alt + RMB
        data object ToggleTurretGyroStabilization : Turret("ToggleTurretGyroStabilization") // E
        data object SwitchToNextRemoteTurret : Turret("SwitchToNextRemoteTurret") // D
        data object SwitchToPreviousRemoteTurret : Turret("SwitchToPreviousRemoteTurret") // A
        data object ExitTurret : Turret("ExitTurret") // Hold Y
        data object ToggleTurretESP : Turret("ToggleTurretESP") // Requires manual binding
    }
}
