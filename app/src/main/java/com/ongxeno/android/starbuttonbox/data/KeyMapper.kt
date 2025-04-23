package com.ongxeno.android.starbuttonbox.data // Or your preferred package

import android.util.Log // Import Log for warnings

/**
 * Maps a unique command identifier string (e.g., "Flight.Boost") to a specific
 * physical InputAction. This determines the actual key/mouse press details based
 * on default bindings from the Star Citizen Keybinding Compilation document (Alpha 3.18+)
 * and common usage.
 *
 * Returns null if the command identifier has no default binding or isn't mapped here.
 *
 * @param commandIdentifier The unique string identifying the command.
 */
fun mapCommandIdentifierToAction(commandIdentifier: String): InputAction? {
    // Default hold duration if not specified otherwise (e.g., for Max Power)
    val defaultHoldDuration = 500L
    // Specific hold durations based on common usage or document notes
    val quantumEngageHoldDuration = 1500L
    val exitSeatHoldDuration = 3000L // Estimate, adjust as needed
    val selfDestructHoldDuration = 3000L // Estimate, adjust as needed
    val autoLandHoldDuration = 1500L // Estimate, adjust as needed
    val setLaunchDecoyHoldDuration = 1000L // Estimate, adjust as needed
    val recenterTurretHoldDuration = 500L // Estimate, adjust as needed
    val zoomTurretHoldDuration = 500L // Estimate, adjust as needed
    val cycleGimbalHoldDuration = 1000L // Estimate for Long Press G

    // Create HOLD instances directly using the top-level type
    val defaultHoldInfo = PressType.HOLD(defaultHoldDuration)
    val quantumEngageHoldInfo = PressType.HOLD(quantumEngageHoldDuration)
    val exitSeatHoldInfo = PressType.HOLD(exitSeatHoldDuration)
    val selfDestructHoldInfo = PressType.HOLD(selfDestructHoldDuration)
    val autoLandHoldInfo = PressType.HOLD(autoLandHoldDuration)
    val setLaunchDecoyHoldInfo = PressType.HOLD(setLaunchDecoyHoldDuration)
    val recenterTurretHoldInfo = PressType.HOLD(recenterTurretHoldDuration)
    val zoomTurretHoldInfo = PressType.HOLD(zoomTurretHoldDuration)
    val cycleGimbalHoldInfo = PressType.HOLD(cycleGimbalHoldDuration)


    // Use the Command object constants for comparison
    return when (commandIdentifier) {
        // --- Flight & Driving Systems ---
        Command.Flight_Boost -> InputAction.KeyEvent(
            key = ModifierKeys.SHIFT_LEFT,
            pressType = PressType.HOLD(0) // Hold indefinitely until released by user
        )
        Command.Flight_Spacebrake -> InputAction.KeyEvent(key = "x")
        Command.Flight_ToggleDecoupledMode -> InputAction.KeyEvent(
            key = "c",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        Command.Flight_ToggleCruiseControl -> InputAction.KeyEvent(key = "c")
        Command.Flight_AdjustSpeedLimiterUp -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.UP)
        Command.Flight_AdjustSpeedLimiterDown -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.DOWN)
        Command.Flight_ToggleVTOLMode -> InputAction.KeyEvent(key = "k")
        Command.Flight_ToggleLockPitchYaw -> InputAction.KeyEvent(key = ModifierKeys.SHIFT_RIGHT) // ESP Toggle
        Command.Flight_ToggleHeadLight -> InputAction.KeyEvent(key = "l")
        Command.Flight_ToggleSpeedLimiter -> InputAction.MouseEvent(MouseButton.MIDDLE)
        Command.Flight_StrafeUp -> InputAction.KeyEvent(key = "space")
        Command.Flight_StrafeDown -> InputAction.KeyEvent(key = ModifierKeys.CTRL_LEFT)
        Command.Flight_StrafeLeft -> InputAction.KeyEvent(key = "a")
        Command.Flight_StrafeRight -> InputAction.KeyEvent(key = "d")
        Command.Flight_StrafeForward -> InputAction.KeyEvent(key = "w")
        Command.Flight_StrafeBackward -> InputAction.KeyEvent(key = "s")
        Command.Flight_RollLeft -> InputAction.KeyEvent(key = "q")
        Command.Flight_RollRight -> InputAction.KeyEvent(key = "e")

        // --- Quantum Travel ---
        Command.QuantumTravel_ToggleQuantumMode -> InputAction.KeyEvent(key = "b") // Spool / Calibrate / Cancel
        Command.QuantumTravel_ActivateQuantumTravel -> InputAction.KeyEvent(
            key = "b",
            pressType = quantumEngageHoldInfo
        ) // Engage
        Command.QuantumTravel_CalibrateQuantumDrive -> InputAction.KeyEvent(key = "b") // Same as ToggleQuantumMode
        Command.QuantumTravel_SetQuantumRoute -> null // Requires manual binding / interaction

        // --- Landing & Docking ---
        Command.LandingAndDocking_ToggleLandingGear -> InputAction.KeyEvent(key = "n")
        Command.LandingAndDocking_AutoLand -> InputAction.KeyEvent(
            key = "n",
            pressType = autoLandHoldInfo
        )
        Command.LandingAndDocking_RequestLandingTakeoff -> InputAction.KeyEvent(
            key = "n",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // ATC
        Command.LandingAndDocking_RequestDocking -> InputAction.KeyEvent(key = "n") // Context dependent
        Command.LandingAndDocking_ToggleDockingCamera -> InputAction.KeyEvent(key = "0")

        // --- Power Management ---
        Command.PowerManagement_TogglePowerWeapons -> InputAction.KeyEvent(key = "p") // Or "1" if preferred
        Command.PowerManagement_TogglePowerShields -> InputAction.KeyEvent(key = "o") // Or "2" if preferred
        Command.PowerManagement_TogglePowerEngines -> InputAction.KeyEvent(key = "i") // Or "3" if preferred
        Command.PowerManagement_TogglePowerAll -> InputAction.KeyEvent(key = "u")
        Command.PowerManagement_IncreasePowerWeapons -> InputAction.KeyEvent(key = "f5")
        Command.PowerManagement_MaxPowerWeapons -> InputAction.KeyEvent(
            key = "f5",
            pressType = defaultHoldInfo
        )
        Command.PowerManagement_IncreasePowerEngines -> InputAction.KeyEvent(key = "f6")
        Command.PowerManagement_MaxPowerEngines -> InputAction.KeyEvent(
            key = "f6",
            pressType = defaultHoldInfo
        )
        Command.PowerManagement_IncreasePowerShields -> InputAction.KeyEvent(key = "f7")
        Command.PowerManagement_MaxPowerShields -> InputAction.KeyEvent(
            key = "f7",
            pressType = defaultHoldInfo
        )
        Command.PowerManagement_ResetPowerDistribution -> InputAction.KeyEvent(key = "f8")
        Command.PowerManagement_DecreasePowerWeapons -> InputAction.KeyEvent(
            key = "f5",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        Command.PowerManagement_MinPowerWeapons -> InputAction.KeyEvent(
            key = "f5",
            modifiers = listOf(ModifierKeys.ALT_LEFT),
            pressType = defaultHoldInfo
        )
        Command.PowerManagement_DecreasePowerEngines -> InputAction.KeyEvent(
            key = "f6",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        Command.PowerManagement_MinPowerEngines -> InputAction.KeyEvent(
            key = "f6",
            modifiers = listOf(ModifierKeys.ALT_LEFT),
            pressType = defaultHoldInfo
        )
        Command.PowerManagement_DecreasePowerShields -> InputAction.KeyEvent(
            key = "f7",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        Command.PowerManagement_MinPowerShields -> InputAction.KeyEvent(
            key = "f7",
            modifiers = listOf(ModifierKeys.ALT_LEFT),
            pressType = defaultHoldInfo
        )
        Command.PowerManagement_PowerTrianglePresetWeapons -> InputAction.KeyEvent(key = "f5") // Tap F5
        Command.PowerManagement_PowerTrianglePresetEngines -> InputAction.KeyEvent(key = "f6") // Tap F6
        Command.PowerManagement_PowerTrianglePresetShields -> InputAction.KeyEvent(key = "f7") // Tap F7
        Command.PowerManagement_PowerTriangleReset -> InputAction.KeyEvent(key = "f8") // Tap F8

        // --- Targeting ---
        Command.Targeting_LockSelectedTarget -> InputAction.KeyEvent(key = "t") // Tap T
        Command.Targeting_UnlockLockedTarget -> InputAction.KeyEvent(
            key = "t",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+T
        Command.Targeting_CycleLockHostilesNext -> InputAction.KeyEvent(key = "5") // Tap 5
        Command.Targeting_CycleLockHostilesClosest -> InputAction.KeyEvent(
            key = "5",
            pressType = defaultHoldInfo
        ) // Hold 5
        Command.Targeting_CycleLockFriendliesNext -> InputAction.KeyEvent(key = "6") // Tap 6
        Command.Targeting_CycleLockFriendliesClosest -> InputAction.KeyEvent(
            key = "6",
            pressType = defaultHoldInfo
        ) // Hold 6
        Command.Targeting_CycleLockAllNext -> InputAction.KeyEvent(key = "7") // Tap 7
        Command.Targeting_CycleLockAllClosest -> InputAction.KeyEvent(
            key = "7",
            pressType = defaultHoldInfo
        ) // Hold 7
        Command.Targeting_CycleLockAttackersNext -> InputAction.KeyEvent(key = "4") // Tap 4
        Command.Targeting_CycleLockAttackersClosest -> InputAction.KeyEvent(
            key = "4",
            pressType = defaultHoldInfo
        ) // Hold 4
        Command.Targeting_CycleLockSubtargetsNext -> InputAction.KeyEvent(key = "r") // Tap R
        Command.Targeting_ResetSubtargetToMain -> InputAction.KeyEvent(
            key = "r",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+R
        Command.Targeting_PinTarget1 -> InputAction.KeyEvent(
            key = "1",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+1
        Command.Targeting_PinTarget2 -> InputAction.KeyEvent(
            key = "2",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+2
        Command.Targeting_PinTarget3 -> InputAction.KeyEvent(
            key = "3",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+3
        Command.Targeting_RemoveAllPinnedTargets -> InputAction.KeyEvent(
            key = "0",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+0
        Command.Targeting_ToggleLookAhead -> InputAction.KeyEvent(
            key = "l",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+L (Look behind?)
        Command.Targeting_TargetNearestHostile -> InputAction.KeyEvent(key = "5") // Tap 5
        Command.Targeting_CycleTargetsForward -> InputAction.KeyEvent(key = "t") // Tap T
        Command.Targeting_CycleTargetsBackward -> InputAction.KeyEvent(key = "y") // Tap Y (needs binding)
        Command.Targeting_CycleSubtargetsForward -> InputAction.KeyEvent(key = "r") // Tap R
        Command.Targeting_CycleSubtargetsBackward -> InputAction.KeyEvent(
            key = "r",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+R
        Command.Targeting_PinSelectedTarget -> InputAction.KeyEvent(
            key = "1",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+1
        Command.Targeting_UnpinSelectedTarget -> InputAction.KeyEvent(
            key = "0",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+0

        // --- Combat (Pilot Weapons) ---
        Command.CombatPilot_FireWeaponGroup1 -> InputAction.MouseEvent(MouseButton.LEFT)
        Command.CombatPilot_FireWeaponGroup2 -> InputAction.MouseEvent(MouseButton.RIGHT)
        Command.CombatPilot_CycleGimbalMode -> InputAction.KeyEvent(key = "g") // Tap G (Long press handled differently if needed)
        Command.CombatPilot_ToggleMissileOperatorMode -> InputAction.MouseEvent(MouseButton.MIDDLE)
        Command.CombatPilot_LaunchMissile -> InputAction.MouseEvent(MouseButton.LEFT) // In Missile Mode
        Command.CombatPilot_CycleMissileType -> InputAction.KeyEvent(key = "g") // Tap G cycles missiles too
        Command.CombatPilot_IncreaseArmedMissiles -> InputAction.KeyEvent(key = "g") // Tap G
        Command.CombatPilot_ResetArmedMissiles -> InputAction.KeyEvent(
            key = "g",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+G
        Command.CombatPilot_CycleFireMode -> InputAction.KeyEvent(key = "v") // Tap V (needs binding)

        // --- Countermeasures ---
        Command.Countermeasures_DeployDecoyPanic -> InputAction.KeyEvent(key = "h") // Tap H
        Command.Countermeasures_DeployDecoyBurst -> InputAction.KeyEvent(key = "h") // Tap H
        Command.Countermeasures_SetLaunchDecoyBurst -> InputAction.KeyEvent(
            key = "h",
            pressType = setLaunchDecoyHoldInfo
        ) // Hold H
        Command.Countermeasures_IncreaseDecoyBurstSize -> InputAction.KeyEvent(
            key = "h",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        ) // RAlt+H
        Command.Countermeasures_DecreaseDecoyBurstSize -> InputAction.KeyEvent(
            key = "h",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // LAlt+H
        Command.Countermeasures_DeployNoise -> InputAction.KeyEvent(key = "j") // Tap J
        Command.Countermeasures_LaunchDecoy -> InputAction.KeyEvent(key = "h") // Tap H
        Command.Countermeasures_LaunchNoise -> InputAction.KeyEvent(key = "j") // Tap J

        // --- Scanning ---
        Command.Scanning_ToggleScanningMode -> InputAction.KeyEvent(key = "v")
        Command.Scanning_ActivatePing -> InputAction.KeyEvent(key = "tab")
        Command.Scanning_ActivateScanTargeted -> InputAction.MouseEvent(MouseButton.LEFT) // In Scan Mode
        Command.Scanning_IncreaseScanAngleFocus -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.UP)
        Command.Scanning_DecreaseScanAngleFocus -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.DOWN)

        // --- General Cockpit ---
        Command.GeneralCockpit_FlightReady -> InputAction.KeyEvent(key = "r")
        Command.GeneralCockpit_ExitSeat -> InputAction.KeyEvent(
            key = "y",
            pressType = exitSeatHoldInfo
        ) // Hold Y
        Command.GeneralCockpit_Eject -> InputAction.KeyEvent(
            key = "y",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        ) // RAlt+Y
        Command.GeneralCockpit_SelfDestruct -> InputAction.KeyEvent(
            key = "backspace",
            pressType = selfDestructHoldInfo
        ) // Hold Backspace
        Command.GeneralCockpit_EmergencyExitSeat -> InputAction.KeyEvent(
            key = "u",
            modifiers = listOf(ModifierKeys.SHIFT_LEFT)
        ) // U+LShift
        Command.GeneralCockpit_TogglePortLockAll -> InputAction.KeyEvent(
            key = "k",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        ) // RAlt+K (Lock Vehicle)
        Command.GeneralCockpit_ToggleAllDoors -> null // Requires manual binding
        Command.GeneralCockpit_ToggleLockAllDoors -> null // Requires manual binding
        Command.GeneralCockpit_OpenAllDoors -> null // Map to ToggleAllDoors if bound
        Command.GeneralCockpit_CloseAllDoors -> null // Map to ToggleAllDoors if bound
        Command.GeneralCockpit_LockDoors -> InputAction.KeyEvent(
            key = "k",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        ) // Map to TogglePortLockAll
        Command.GeneralCockpit_UnlockDoors -> InputAction.KeyEvent(
            key = "k",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        ) // Map to TogglePortLockAll

        // --- Ship Salvage ---
        Command.ShipSalvage_ToggleSalvageMode -> InputAction.KeyEvent(key = "m")
        Command.ShipSalvage_ToggleSalvageGimbal -> InputAction.KeyEvent(key = "g")
        Command.ShipSalvage_ResetSalvageGimbal -> InputAction.KeyEvent(
            key = "g",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        Command.ShipSalvage_ToggleSalvageBeamFocused -> InputAction.MouseEvent(MouseButton.LEFT)
        Command.ShipSalvage_ToggleSalvageBeamLeft -> InputAction.KeyEvent(
            key = "a",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        )
        Command.ShipSalvage_ToggleSalvageBeamRight -> InputAction.KeyEvent(
            key = "d",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        )
        Command.ShipSalvage_ToggleSalvageBeamFracture -> InputAction.KeyEvent(
            key = "w",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        )
        Command.ShipSalvage_ToggleSalvageBeamDisintegrate -> InputAction.KeyEvent(
            key = "s",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        )
        Command.ShipSalvage_CycleSalvageModifiers -> InputAction.MouseEvent(MouseButton.RIGHT)
        Command.ShipSalvage_AdjustSalvageBeamSpacingUp -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.UP)
        Command.ShipSalvage_AdjustSalvageBeamSpacingDown -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.DOWN)
        Command.ShipSalvage_ToggleSalvageBeamAxis -> InputAction.MouseEvent(
            button = MouseButton.RIGHT,
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        Command.ShipSalvage_FocusSalvageHeadsAll -> InputAction.KeyEvent(
            key = "s",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        Command.ShipSalvage_FocusSalvageHeadLeft -> InputAction.KeyEvent(
            key = "a",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        Command.ShipSalvage_FocusSalvageHeadRight -> InputAction.KeyEvent(
            key = "d",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        Command.ShipSalvage_FocusSalvageToolFracture -> InputAction.KeyEvent(
            key = "w",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        Command.ShipSalvage_IncreaseSalvageBeamSpacing -> null
        Command.ShipSalvage_DecreaseSalvageBeamSpacing -> null
        Command.ShipSalvage_SetAbsoluteSalvageBeamSpacing -> null

        // --- Ship Mining ---
        Command.ShipMining_ToggleMiningMode -> InputAction.KeyEvent(key = "m")
        Command.ShipMining_FireMiningLaser -> InputAction.MouseEvent(MouseButton.LEFT)
        Command.ShipMining_SwitchMiningLaser -> InputAction.MouseEvent(
            button = MouseButton.LEFT,
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        Command.ShipMining_IncreaseMiningLaserPower -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.UP)
        Command.ShipMining_DecreaseMiningLaserPower -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.DOWN)
        Command.ShipMining_CycleMiningLaserGimbal -> InputAction.KeyEvent(key = "g")
        Command.ShipMining_ActivateMiningConsumable1 -> InputAction.KeyEvent(
            key = "1",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        Command.ShipMining_ActivateMiningConsumable2 -> InputAction.KeyEvent(
            key = "2",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        Command.ShipMining_ActivateMiningConsumable3 -> InputAction.KeyEvent(
            key = "3",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        Command.ShipMining_JettisonCargo -> InputAction.KeyEvent(
            key = "j",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )

        // --- Turret Gunner Systems ---
        Command.Turret_ToggleTurretAimMode -> InputAction.KeyEvent(key = "q")
        Command.Turret_RecenterTurret -> InputAction.KeyEvent(
            key = "c",
            pressType = recenterTurretHoldInfo
        )
        Command.Turret_ChangeTurretPosition -> InputAction.KeyEvent(key = "s")
        Command.Turret_AdjustTurretSpeedLimiterUp -> InputAction.MouseScroll(
            direction = InputAction.MouseScroll.Direction.UP,
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        Command.Turret_AdjustTurretSpeedLimiterDown -> InputAction.MouseScroll(
            direction = InputAction.MouseScroll.Direction.DOWN,
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        Command.Turret_FireTurretWeapons -> InputAction.MouseEvent(MouseButton.LEFT)
        Command.Turret_ToggleTurretPrecisionTargeting -> InputAction.MouseEvent(MouseButton.RIGHT)
        Command.Turret_ZoomTurretPrecisionTargeting -> InputAction.MouseEvent(
            button = MouseButton.RIGHT,
            pressType = zoomTurretHoldInfo
        )
        Command.Turret_CycleTurretPrecisionMode -> InputAction.MouseEvent(
            button = MouseButton.RIGHT,
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        )
        Command.Turret_ToggleTurretGyroStabilization -> InputAction.KeyEvent(key = "e")
        Command.Turret_SwitchToNextRemoteTurret -> InputAction.KeyEvent(key = "d")
        Command.Turret_SwitchToPreviousRemoteTurret -> InputAction.KeyEvent(key = "a")
        Command.Turret_ExitTurret -> InputAction.KeyEvent(
            key = "y",
            pressType = exitSeatHoldInfo
        )
        Command.Turret_CycleTurretFireMode -> null
        Command.Turret_ToggleTurretESP -> null

        // Default case for unmapped identifiers
        else -> {
            Log.w("KeyMapper", "No InputAction mapped for command identifier: $commandIdentifier")
            null
        }
    }
}
