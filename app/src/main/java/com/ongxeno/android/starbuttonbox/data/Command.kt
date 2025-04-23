package com.ongxeno.android.starbuttonbox.data

/**
 * Defines unique command identifier strings for Star Citizen actions
 * using compile-time constants within a singleton object.
 * These strings are used for serialization, lookup, and logging.
 * The format is "Category_SubCommand".
 */
object Command {
    // --- Flight ---
    const val Flight_Boost = "Flight_Boost" // HOLD Left Shift
    const val Flight_Spacebrake = "Flight_Spacebrake" // X
    const val Flight_ToggleDecoupledMode = "Flight_ToggleDecoupledMode" // Left Alt + C
    const val Flight_ToggleCruiseControl = "Flight_ToggleCruiseControl" // C
    const val Flight_AdjustSpeedLimiterUp = "Flight_AdjustSpeedLimiterUp" // MWU
    const val Flight_AdjustSpeedLimiterDown = "Flight_AdjustSpeedLimiterDown" // MWD
    const val Flight_ToggleVTOLMode = "Flight_ToggleVTOLMode" // K
    const val Flight_ToggleLockPitchYaw = "Flight_ToggleLockPitchYaw" // Right Shift (Often ESP Toggle)
    const val Flight_ToggleHeadLight = "Flight_ToggleHeadLight" // L
    const val Flight_ToggleSpeedLimiter = "Flight_ToggleSpeedLimiter" // middle click
    const val Flight_StrafeUp = "Flight_StrafeUp" // Space
    const val Flight_StrafeDown = "Flight_StrafeDown" // Left Ctrl
    const val Flight_StrafeLeft = "Flight_StrafeLeft" // A
    const val Flight_StrafeRight = "Flight_StrafeRight" // D
    const val Flight_StrafeForward = "Flight_StrafeForward" // W
    const val Flight_StrafeBackward = "Flight_StrafeBackward" // S
    const val Flight_RollLeft = "Flight_RollLeft" // Q
    const val Flight_RollRight = "Flight_RollRight" // E

    // --- QuantumTravel ---
    const val QuantumTravel_ToggleQuantumMode = "QuantumTravel_ToggleQuantumMode" // Tap B (Spool)
    const val QuantumTravel_ActivateQuantumTravel = "QuantumTravel_ActivateQuantumTravel" // HOLD B (Engage)
    const val QuantumTravel_CalibrateQuantumDrive = "QuantumTravel_CalibrateQuantumDrive" // Tap B (While Spooled)
    const val QuantumTravel_SetQuantumRoute = "QuantumTravel_SetQuantumRoute" // Requires manual binding

    // --- LandingAndDocking ---
    const val LandingAndDocking_ToggleLandingGear = "LandingAndDocking_ToggleLandingGear" // N
    const val LandingAndDocking_AutoLand = "LandingAndDocking_AutoLand" // HOLD N
    const val LandingAndDocking_RequestLandingTakeoff = "LandingAndDocking_RequestLandingTakeoff" // Left Alt + N (ATC)
    const val LandingAndDocking_RequestDocking = "LandingAndDocking_RequestDocking" // N (while in Docking Mode)
    const val LandingAndDocking_ToggleDockingCamera = "LandingAndDocking_ToggleDockingCamera" // 0

    // --- PowerManagement ---
    const val PowerManagement_TogglePowerWeapons = "PowerManagement_TogglePowerWeapons" // P or 1
    const val PowerManagement_TogglePowerShields = "PowerManagement_TogglePowerShields" // O or 2
    const val PowerManagement_TogglePowerEngines = "PowerManagement_TogglePowerEngines" // I or 3
    const val PowerManagement_TogglePowerAll = "PowerManagement_TogglePowerAll" // U
    const val PowerManagement_IncreasePowerWeapons = "PowerManagement_IncreasePowerWeapons" // Tap F5
    const val PowerManagement_MaxPowerWeapons = "PowerManagement_MaxPowerWeapons" // HOLD F5
    const val PowerManagement_IncreasePowerEngines = "PowerManagement_IncreasePowerEngines" // Tap F6
    const val PowerManagement_MaxPowerEngines = "PowerManagement_MaxPowerEngines" // HOLD F6
    const val PowerManagement_IncreasePowerShields = "PowerManagement_IncreasePowerShields" // Tap F7
    const val PowerManagement_MaxPowerShields = "PowerManagement_MaxPowerShields" // HOLD F7
    const val PowerManagement_ResetPowerDistribution = "PowerManagement_ResetPowerDistribution" // F8
    const val PowerManagement_DecreasePowerWeapons = "PowerManagement_DecreasePowerWeapons" // Tap Left Alt + F5
    const val PowerManagement_MinPowerWeapons = "PowerManagement_MinPowerWeapons" // HOLD Left Alt + F5
    const val PowerManagement_DecreasePowerEngines = "PowerManagement_DecreasePowerEngines" // Tap Left Alt + F6
    const val PowerManagement_MinPowerEngines = "PowerManagement_MinPowerEngines" // HOLD Left Alt + F6
    const val PowerManagement_DecreasePowerShields = "PowerManagement_DecreasePowerShields" // Tap Left Alt + F7
    const val PowerManagement_MinPowerShields = "PowerManagement_MinPowerShields" // HOLD Left Alt + F7
    const val PowerManagement_PowerTrianglePresetWeapons = "PowerManagement_PowerTrianglePresetWeapons" // F5
    const val PowerManagement_PowerTrianglePresetEngines = "PowerManagement_PowerTrianglePresetEngines" // F6
    const val PowerManagement_PowerTrianglePresetShields = "PowerManagement_PowerTrianglePresetShields" // F7
    const val PowerManagement_PowerTriangleReset = "PowerManagement_PowerTriangleReset" // F8

    // --- Targeting ---
    const val Targeting_LockSelectedTarget = "Targeting_LockSelectedTarget" // Tap T
    const val Targeting_UnlockLockedTarget = "Targeting_UnlockLockedTarget" // Left Alt + T
    const val Targeting_CycleLockHostilesNext = "Targeting_CycleLockHostilesNext" // Tap 5
    const val Targeting_CycleLockHostilesClosest = "Targeting_CycleLockHostilesClosest" // Hold 5
    const val Targeting_CycleLockFriendliesNext = "Targeting_CycleLockFriendliesNext" // Tap 6
    const val Targeting_CycleLockFriendliesClosest = "Targeting_CycleLockFriendliesClosest" // Hold 6
    const val Targeting_CycleLockAllNext = "Targeting_CycleLockAllNext" // Tap 7
    const val Targeting_CycleLockAllClosest = "Targeting_CycleLockAllClosest" // Hold 7
    const val Targeting_CycleLockAttackersNext = "Targeting_CycleLockAttackersNext" // Tap 4
    const val Targeting_CycleLockAttackersClosest = "Targeting_CycleLockAttackersClosest" // Hold 4
    const val Targeting_CycleLockSubtargetsNext = "Targeting_CycleLockSubtargetsNext" // Tap R
    const val Targeting_ResetSubtargetToMain = "Targeting_ResetSubtargetToMain" // Left Alt + R
    const val Targeting_PinTarget1 = "Targeting_PinTarget1" // Left Alt + 1
    const val Targeting_PinTarget2 = "Targeting_PinTarget2" // Left Alt + 2
    const val Targeting_PinTarget3 = "Targeting_PinTarget3" // Left Alt + 3
    const val Targeting_RemoveAllPinnedTargets = "Targeting_RemoveAllPinnedTargets" // 0 (or Left Alt + 0)
    const val Targeting_ToggleLookAhead = "Targeting_ToggleLookAhead" // Left Alt + L
    const val Targeting_TargetNearestHostile = "Targeting_TargetNearestHostile" // Tap 5
    const val Targeting_CycleTargetsForward = "Targeting_CycleTargetsForward" // T
    const val Targeting_CycleTargetsBackward = "Targeting_CycleTargetsBackward" // Y
    const val Targeting_CycleSubtargetsForward = "Targeting_CycleSubtargetsForward" // R
    const val Targeting_CycleSubtargetsBackward = "Targeting_CycleSubtargetsBackward" // Left Alt + R
    const val Targeting_PinSelectedTarget = "Targeting_PinSelectedTarget" // Left Alt + 1
    const val Targeting_UnpinSelectedTarget = "Targeting_UnpinSelectedTarget" // Left Alt + 0

    // --- CombatPilot ---
    const val CombatPilot_FireWeaponGroup1 = "CombatPilot_FireWeaponGroup1" // LMB
    const val CombatPilot_FireWeaponGroup2 = "CombatPilot_FireWeaponGroup2" // RMB
    const val CombatPilot_CycleGimbalMode = "CombatPilot_CycleGimbalMode" // G
    const val CombatPilot_ToggleMissileOperatorMode = "CombatPilot_ToggleMissileOperatorMode" // MMB
    const val CombatPilot_LaunchMissile = "CombatPilot_LaunchMissile" // LMB (in Missile Mode)
    const val CombatPilot_CycleMissileType = "CombatPilot_CycleMissileType" // Mouse Wheel / G
    const val CombatPilot_IncreaseArmedMissiles = "CombatPilot_IncreaseArmedMissiles" // Tap G
    const val CombatPilot_ResetArmedMissiles = "CombatPilot_ResetArmedMissiles" // Left Alt + G
    const val CombatPilot_CycleFireMode = "CombatPilot_CycleFireMode" // V

    // --- Countermeasures ---
    const val Countermeasures_DeployDecoyPanic = "Countermeasures_DeployDecoyPanic" // H
    const val Countermeasures_DeployDecoyBurst = "Countermeasures_DeployDecoyBurst" // H
    const val Countermeasures_SetLaunchDecoyBurst = "Countermeasures_SetLaunchDecoyBurst" // Hold H
    const val Countermeasures_IncreaseDecoyBurstSize = "Countermeasures_IncreaseDecoyBurstSize" // Right Alt + H
    const val Countermeasures_DecreaseDecoyBurstSize = "Countermeasures_DecreaseDecoyBurstSize" // Left Alt + H
    const val Countermeasures_DeployNoise = "Countermeasures_DeployNoise" // J
    const val Countermeasures_LaunchDecoy = "Countermeasures_LaunchDecoy" // H
    const val Countermeasures_LaunchNoise = "Countermeasures_LaunchNoise" // J

    // --- Scanning ---
    const val Scanning_ToggleScanningMode = "Scanning_ToggleScanningMode" // V
    const val Scanning_ActivatePing = "Scanning_ActivatePing" // TAB
    const val Scanning_ActivateScanTargeted = "Scanning_ActivateScanTargeted" // LMB (in Scan Mode)
    const val Scanning_IncreaseScanAngleFocus = "Scanning_IncreaseScanAngleFocus" // MWU
    const val Scanning_DecreaseScanAngleFocus = "Scanning_DecreaseScanAngleFocus" // MWD

    // --- GeneralCockpit ---
    const val GeneralCockpit_FlightReady = "GeneralCockpit_FlightReady" // R
    const val GeneralCockpit_ExitSeat = "GeneralCockpit_ExitSeat" // Hold Y
    const val GeneralCockpit_Eject = "GeneralCockpit_Eject" // Right Alt + Y
    const val GeneralCockpit_EmergencyExitSeat = "GeneralCockpit_EmergencyExitSeat" // U + Left Shift
    const val GeneralCockpit_SelfDestruct = "GeneralCockpit_SelfDestruct" // Hold Backspace
    const val GeneralCockpit_TogglePortLockAll = "GeneralCockpit_TogglePortLockAll" // Right Alt + K
    const val GeneralCockpit_ToggleAllDoors = "GeneralCockpit_ToggleAllDoors" // Doors
    const val GeneralCockpit_ToggleLockAllDoors = "GeneralCockpit_ToggleLockAllDoors" // Lock
    const val GeneralCockpit_OpenAllDoors = "GeneralCockpit_OpenAllDoors"
    const val GeneralCockpit_CloseAllDoors = "GeneralCockpit_CloseAllDoors"
    const val GeneralCockpit_LockDoors = "GeneralCockpit_LockDoors"
    const val GeneralCockpit_UnlockDoors = "GeneralCockpit_UnlockDoors"

    // --- ShipSalvage ---
    const val ShipSalvage_ToggleSalvageMode = "ShipSalvage_ToggleSalvageMode" // M
    const val ShipSalvage_ToggleSalvageGimbal = "ShipSalvage_ToggleSalvageGimbal" // G
    const val ShipSalvage_ResetSalvageGimbal = "ShipSalvage_ResetSalvageGimbal" // Left Alt + G
    const val ShipSalvage_ToggleSalvageBeamFocused = "ShipSalvage_ToggleSalvageBeamFocused" // LMB
    const val ShipSalvage_ToggleSalvageBeamLeft = "ShipSalvage_ToggleSalvageBeamLeft" // Right Alt + A
    const val ShipSalvage_ToggleSalvageBeamRight = "ShipSalvage_ToggleSalvageBeamRight" // Right Alt + D
    const val ShipSalvage_ToggleSalvageBeamFracture = "ShipSalvage_ToggleSalvageBeamFracture" // Right Alt + W
    const val ShipSalvage_ToggleSalvageBeamDisintegrate = "ShipSalvage_ToggleSalvageBeamDisintegrate" // Right Alt + S
    const val ShipSalvage_CycleSalvageModifiers = "ShipSalvage_CycleSalvageModifiers" // RMB
    const val ShipSalvage_AdjustSalvageBeamSpacingUp = "ShipSalvage_AdjustSalvageBeamSpacingUp" // MWU
    const val ShipSalvage_AdjustSalvageBeamSpacingDown = "ShipSalvage_AdjustSalvageBeamSpacingDown" // MWD
    const val ShipSalvage_ToggleSalvageBeamAxis = "ShipSalvage_ToggleSalvageBeamAxis" // Left Alt + RMB
    const val ShipSalvage_FocusSalvageHeadsAll = "ShipSalvage_FocusSalvageHeadsAll" // Left Alt + S
    const val ShipSalvage_FocusSalvageHeadLeft = "ShipSalvage_FocusSalvageHeadLeft" // Left Alt + A
    const val ShipSalvage_FocusSalvageHeadRight = "ShipSalvage_FocusSalvageHeadRight" // Left Alt + D
    const val ShipSalvage_FocusSalvageToolFracture = "ShipSalvage_FocusSalvageToolFracture" // Left Alt + W
    const val ShipSalvage_IncreaseSalvageBeamSpacing = "ShipSalvage_IncreaseSalvageBeamSpacing"
    const val ShipSalvage_DecreaseSalvageBeamSpacing = "ShipSalvage_DecreaseSalvageBeamSpacing"
    const val ShipSalvage_SetAbsoluteSalvageBeamSpacing = "ShipSalvage_SetAbsoluteSalvageBeamSpacing"

    // --- ShipMining ---
    const val ShipMining_ToggleMiningMode = "ShipMining_ToggleMiningMode" // M
    const val ShipMining_FireMiningLaser = "ShipMining_FireMiningLaser" // LMB
    const val ShipMining_SwitchMiningLaser = "ShipMining_SwitchMiningLaser" // Left Alt + LMB
    const val ShipMining_IncreaseMiningLaserPower = "ShipMining_IncreaseMiningLaserPower" // MWU
    const val ShipMining_DecreaseMiningLaserPower = "ShipMining_DecreaseMiningLaserPower" // MWD
    const val ShipMining_CycleMiningLaserGimbal = "ShipMining_CycleMiningLaserGimbal" // G
    const val ShipMining_ActivateMiningConsumable1 = "ShipMining_ActivateMiningConsumable1" // Left Alt + 1
    const val ShipMining_ActivateMiningConsumable2 = "ShipMining_ActivateMiningConsumable2" // Left Alt + 2
    const val ShipMining_ActivateMiningConsumable3 = "ShipMining_ActivateMiningConsumable3" // Left Alt + 3
    const val ShipMining_JettisonCargo = "ShipMining_JettisonCargo" // Left Alt + J

    // --- Turret ---
    const val Turret_ToggleTurretAimMode = "Turret_ToggleTurretAimMode" // Q
    const val Turret_RecenterTurret = "Turret_RecenterTurret" // Hold C
    const val Turret_ChangeTurretPosition = "Turret_ChangeTurretPosition" // S
    const val Turret_AdjustTurretSpeedLimiterUp = "Turret_AdjustTurretSpeedLimiterUp" // Left Alt + MWU
    const val Turret_AdjustTurretSpeedLimiterDown = "Turret_AdjustTurretSpeedLimiterDown" // Left Alt + MWD
    const val Turret_FireTurretWeapons = "Turret_FireTurretWeapons" // LMB
    const val Turret_CycleTurretFireMode = "Turret_CycleTurretFireMode"
    const val Turret_ToggleTurretPrecisionTargeting = "Turret_ToggleTurretPrecisionTargeting" // Tap RMB
    const val Turret_ZoomTurretPrecisionTargeting = "Turret_ZoomTurretPrecisionTargeting" // Hold RMB
    const val Turret_CycleTurretPrecisionMode = "Turret_CycleTurretPrecisionMode" // Right Alt + RMB
    const val Turret_ToggleTurretGyroStabilization = "Turret_ToggleTurretGyroStabilization" // E
    const val Turret_SwitchToNextRemoteTurret = "Turret_SwitchToNextRemoteTurret" // D
    const val Turret_SwitchToPreviousRemoteTurret = "Turret_SwitchToPreviousRemoteTurret" // A
    const val Turret_ExitTurret = "Turret_ExitTurret" // Hold Y
    const val Turret_ToggleTurretESP = "Turret_ToggleTurretESP"

    // Helper function to get all command identifier strings
    fun getAllCommandStrings(): List<String> {
        // Use reflection here to get all const vals if you want it dynamic,
        // but listing manually is safer for Android runtime.
        return listOf(
            Flight_Boost, Flight_Spacebrake, Flight_ToggleDecoupledMode, Flight_ToggleCruiseControl,
            Flight_AdjustSpeedLimiterUp, Flight_AdjustSpeedLimiterDown, Flight_ToggleVTOLMode,
            Flight_ToggleLockPitchYaw, Flight_ToggleHeadLight, Flight_ToggleSpeedLimiter,
            Flight_StrafeUp, Flight_StrafeDown, Flight_StrafeLeft, Flight_StrafeRight,
            Flight_StrafeForward, Flight_StrafeBackward, Flight_RollLeft, Flight_RollRight,
            QuantumTravel_ToggleQuantumMode, QuantumTravel_ActivateQuantumTravel,
            QuantumTravel_CalibrateQuantumDrive, QuantumTravel_SetQuantumRoute,
            LandingAndDocking_ToggleLandingGear, LandingAndDocking_AutoLand,
            LandingAndDocking_RequestLandingTakeoff, LandingAndDocking_RequestDocking,
            LandingAndDocking_ToggleDockingCamera,
            PowerManagement_TogglePowerWeapons, PowerManagement_TogglePowerShields,
            PowerManagement_TogglePowerEngines, PowerManagement_TogglePowerAll,
            PowerManagement_IncreasePowerWeapons, PowerManagement_MaxPowerWeapons,
            PowerManagement_IncreasePowerEngines, PowerManagement_MaxPowerEngines,
            PowerManagement_IncreasePowerShields, PowerManagement_MaxPowerShields,
            PowerManagement_ResetPowerDistribution, PowerManagement_DecreasePowerWeapons,
            PowerManagement_MinPowerWeapons, PowerManagement_DecreasePowerEngines,
            PowerManagement_MinPowerEngines, PowerManagement_DecreasePowerShields,
            PowerManagement_MinPowerShields, PowerManagement_PowerTrianglePresetWeapons,
            PowerManagement_PowerTrianglePresetEngines, PowerManagement_PowerTrianglePresetShields,
            PowerManagement_PowerTriangleReset,
            Targeting_LockSelectedTarget, Targeting_UnlockLockedTarget, Targeting_CycleLockHostilesNext,
            Targeting_CycleLockHostilesClosest, Targeting_CycleLockFriendliesNext,
            Targeting_CycleLockFriendliesClosest, Targeting_CycleLockAllNext, Targeting_CycleLockAllClosest,
            Targeting_CycleLockAttackersNext, Targeting_CycleLockAttackersClosest,
            Targeting_CycleLockSubtargetsNext, Targeting_ResetSubtargetToMain, Targeting_PinTarget1,
            Targeting_PinTarget2, Targeting_PinTarget3, Targeting_RemoveAllPinnedTargets,
            Targeting_ToggleLookAhead, Targeting_TargetNearestHostile, Targeting_CycleTargetsForward,
            Targeting_CycleTargetsBackward, Targeting_CycleSubtargetsForward, Targeting_CycleSubtargetsBackward,
            Targeting_PinSelectedTarget, Targeting_UnpinSelectedTarget,
            CombatPilot_FireWeaponGroup1, CombatPilot_FireWeaponGroup2, CombatPilot_CycleGimbalMode,
            CombatPilot_ToggleMissileOperatorMode, CombatPilot_LaunchMissile, CombatPilot_CycleMissileType,
            CombatPilot_IncreaseArmedMissiles, CombatPilot_ResetArmedMissiles, CombatPilot_CycleFireMode,
            Countermeasures_DeployDecoyPanic, Countermeasures_DeployDecoyBurst, Countermeasures_SetLaunchDecoyBurst,
            Countermeasures_IncreaseDecoyBurstSize, Countermeasures_DecreaseDecoyBurstSize,
            Countermeasures_DeployNoise, Countermeasures_LaunchDecoy, Countermeasures_LaunchNoise,
            Scanning_ToggleScanningMode, Scanning_ActivatePing, Scanning_ActivateScanTargeted,
            Scanning_IncreaseScanAngleFocus, Scanning_DecreaseScanAngleFocus,
            GeneralCockpit_FlightReady, GeneralCockpit_ExitSeat, GeneralCockpit_Eject,
            GeneralCockpit_EmergencyExitSeat, GeneralCockpit_SelfDestruct, GeneralCockpit_TogglePortLockAll,
            GeneralCockpit_ToggleAllDoors, GeneralCockpit_ToggleLockAllDoors, GeneralCockpit_OpenAllDoors,
            GeneralCockpit_CloseAllDoors, GeneralCockpit_LockDoors, GeneralCockpit_UnlockDoors,
            ShipSalvage_ToggleSalvageMode, ShipSalvage_ToggleSalvageGimbal, ShipSalvage_ResetSalvageGimbal,
            ShipSalvage_ToggleSalvageBeamFocused, ShipSalvage_ToggleSalvageBeamLeft, ShipSalvage_ToggleSalvageBeamRight,
            ShipSalvage_ToggleSalvageBeamFracture, ShipSalvage_ToggleSalvageBeamDisintegrate,
            ShipSalvage_CycleSalvageModifiers, ShipSalvage_AdjustSalvageBeamSpacingUp,
            ShipSalvage_AdjustSalvageBeamSpacingDown, ShipSalvage_ToggleSalvageBeamAxis,
            ShipSalvage_FocusSalvageHeadsAll, ShipSalvage_FocusSalvageHeadLeft, ShipSalvage_FocusSalvageHeadRight,
            ShipSalvage_FocusSalvageToolFracture, ShipSalvage_IncreaseSalvageBeamSpacing,
            ShipSalvage_DecreaseSalvageBeamSpacing, ShipSalvage_SetAbsoluteSalvageBeamSpacing,
            ShipMining_ToggleMiningMode, ShipMining_FireMiningLaser, ShipMining_SwitchMiningLaser,
            ShipMining_IncreaseMiningLaserPower, ShipMining_DecreaseMiningLaserPower,
            ShipMining_CycleMiningLaserGimbal, ShipMining_ActivateMiningConsumable1,
            ShipMining_ActivateMiningConsumable2, ShipMining_ActivateMiningConsumable3, ShipMining_JettisonCargo,
            Turret_ToggleTurretAimMode, Turret_RecenterTurret, Turret_ChangeTurretPosition,
            Turret_AdjustTurretSpeedLimiterUp, Turret_AdjustTurretSpeedLimiterDown,
            Turret_FireTurretWeapons, Turret_CycleTurretFireMode, Turret_ToggleTurretPrecisionTargeting,
            Turret_ZoomTurretPrecisionTargeting, Turret_CycleTurretPrecisionMode,
            Turret_ToggleTurretGyroStabilization, Turret_SwitchToNextRemoteTurret,
            Turret_SwitchToPreviousRemoteTurret, Turret_ExitTurret, Turret_ToggleTurretESP
        ).sorted() // Sort alphabetically
    }
}