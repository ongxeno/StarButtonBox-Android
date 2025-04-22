package com.ongxeno.android.starbuttonbox.data // Or your preferred package

/**
 * Extension function to map a logical Command object to a specific physical InputAction.
 * This determines the actual key/mouse press details based on default bindings
 * from the Star Citizen Keybinding Compilation document (Alpha 3.18+) and common usage.
 *
 * Returns null if the command has no default binding or isn't mapped here.
 */
fun Command.toInputAction(): InputAction? {
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


    return when (this) {
        // --- Flight & Driving Systems ---
        is Command.Flight.Boost -> InputAction.KeyEvent(
            key = ModifierKeys.SHIFT_LEFT,
            pressType = PressType.HOLD(0) // Hold indefinitely until released by user
        )

        is Command.Flight.Spacebrake -> InputAction.KeyEvent(key = "x")
        is Command.Flight.ToggleDecoupledMode -> InputAction.KeyEvent(
            key = "c",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )

        is Command.Flight.ToggleCruiseControl -> InputAction.KeyEvent(key = "c")
        is Command.Flight.AdjustSpeedLimiterUp -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.UP)
        is Command.Flight.AdjustSpeedLimiterDown -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.DOWN)
        is Command.Flight.ToggleVTOLMode -> InputAction.KeyEvent(key = "k")
        is Command.Flight.ToggleLockPitchYaw -> InputAction.KeyEvent(key = ModifierKeys.SHIFT_RIGHT) // ESP Toggle
        is Command.Flight.ToggleHeadLight -> InputAction.KeyEvent(key = "l")
        is Command.Flight.ToggleSpeedLimiter -> InputAction.MouseEvent(MouseButton.MIDDLE)
        // New Flight Commands
        is Command.Flight.StrafeUp -> InputAction.KeyEvent(key = "space")
        is Command.Flight.StrafeDown -> InputAction.KeyEvent(key = ModifierKeys.CTRL_LEFT)
        is Command.Flight.StrafeLeft -> InputAction.KeyEvent(key = "a")
        is Command.Flight.StrafeRight -> InputAction.KeyEvent(key = "d")
        is Command.Flight.StrafeForward -> InputAction.KeyEvent(key = "w")
        is Command.Flight.StrafeBackward -> InputAction.KeyEvent(key = "s")
        is Command.Flight.RollLeft -> InputAction.KeyEvent(key = "q")
        is Command.Flight.RollRight -> InputAction.KeyEvent(key = "e")


        // --- Quantum Travel ---
        is Command.QuantumTravel.ToggleQuantumMode -> InputAction.KeyEvent(key = "b") // Spool / Calibrate / Cancel
        is Command.QuantumTravel.ActivateQuantumTravel -> InputAction.KeyEvent(
            key = "b",
            pressType = quantumEngageHoldInfo
        ) // Engage
        is Command.QuantumTravel.CalibrateQuantumDrive -> InputAction.KeyEvent(key = "b") // Same as ToggleQuantumMode
        is Command.QuantumTravel.SetQuantumRoute -> null // Requires manual binding / interaction

        // --- Landing & Docking ---
        is Command.LandingAndDocking.ToggleLandingGear -> InputAction.KeyEvent(key = "n")
        is Command.LandingAndDocking.AutoLand -> InputAction.KeyEvent(
            key = "n",
            pressType = autoLandHoldInfo
        )

        is Command.LandingAndDocking.RequestLandingTakeoff -> InputAction.KeyEvent(
            key = "n",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // ATC

        is Command.LandingAndDocking.RequestDocking -> InputAction.KeyEvent(key = "n") // Context dependent
        is Command.LandingAndDocking.ToggleDockingCamera -> InputAction.KeyEvent(key = "0")

        // --- Power Management ---
        is Command.PowerManagement.TogglePowerWeapons -> InputAction.KeyEvent(key = "p") // Or "1" if preferred
        is Command.PowerManagement.TogglePowerShields -> InputAction.KeyEvent(key = "o") // Or "2" if preferred
        is Command.PowerManagement.TogglePowerEngines -> InputAction.KeyEvent(key = "i") // Or "3" if preferred
        is Command.PowerManagement.TogglePowerAll -> InputAction.KeyEvent(key = "u")
        is Command.PowerManagement.IncreasePowerWeapons -> InputAction.KeyEvent(key = "f5")
        is Command.PowerManagement.MaxPowerWeapons -> InputAction.KeyEvent(
            key = "f5",
            pressType = defaultHoldInfo
        )

        is Command.PowerManagement.IncreasePowerEngines -> InputAction.KeyEvent(key = "f6")
        is Command.PowerManagement.MaxPowerEngines -> InputAction.KeyEvent(
            key = "f6",
            pressType = defaultHoldInfo
        )

        is Command.PowerManagement.IncreasePowerShields -> InputAction.KeyEvent(key = "f7")
        is Command.PowerManagement.MaxPowerShields -> InputAction.KeyEvent(
            key = "f7",
            pressType = defaultHoldInfo
        )

        is Command.PowerManagement.ResetPowerDistribution -> InputAction.KeyEvent(key = "f8")
        is Command.PowerManagement.DecreasePowerWeapons -> InputAction.KeyEvent(
            key = "f5",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )

        is Command.PowerManagement.MinPowerWeapons -> InputAction.KeyEvent(
            key = "f5",
            modifiers = listOf(ModifierKeys.ALT_LEFT),
            pressType = defaultHoldInfo
        )

        is Command.PowerManagement.DecreasePowerEngines -> InputAction.KeyEvent(
            key = "f6",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )

        is Command.PowerManagement.MinPowerEngines -> InputAction.KeyEvent(
            key = "f6",
            modifiers = listOf(ModifierKeys.ALT_LEFT),
            pressType = defaultHoldInfo
        )

        is Command.PowerManagement.DecreasePowerShields -> InputAction.KeyEvent(
            key = "f7",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )

        is Command.PowerManagement.MinPowerShields -> InputAction.KeyEvent(
            key = "f7",
            modifiers = listOf(ModifierKeys.ALT_LEFT),
            pressType = defaultHoldInfo
        )
        // New Power Commands (mapped to existing where appropriate)
        is Command.PowerManagement.PowerTrianglePresetWeapons -> InputAction.KeyEvent(key = "f5") // Tap F5
        is Command.PowerManagement.PowerTrianglePresetEngines -> InputAction.KeyEvent(key = "f6") // Tap F6
        is Command.PowerManagement.PowerTrianglePresetShields -> InputAction.KeyEvent(key = "f7") // Tap F7
        is Command.PowerManagement.PowerTriangleReset -> InputAction.KeyEvent(key = "f8") // Tap F8


        // --- Targeting ---
        is Command.Targeting.LockSelectedTarget -> InputAction.KeyEvent(key = "t") // Tap T
        is Command.Targeting.UnlockLockedTarget -> InputAction.KeyEvent(
            key = "t",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+T

        is Command.Targeting.CycleLockHostilesNext -> InputAction.KeyEvent(key = "5") // Tap 5
        is Command.Targeting.CycleLockHostilesClosest -> InputAction.KeyEvent(
            key = "5",
            pressType = defaultHoldInfo
        ) // Hold 5

        is Command.Targeting.CycleLockFriendliesNext -> InputAction.KeyEvent(key = "6") // Tap 6
        is Command.Targeting.CycleLockFriendliesClosest -> InputAction.KeyEvent(
            key = "6",
            pressType = defaultHoldInfo
        ) // Hold 6

        is Command.Targeting.CycleLockAllNext -> InputAction.KeyEvent(key = "7") // Tap 7
        is Command.Targeting.CycleLockAllClosest -> InputAction.KeyEvent(
            key = "7",
            pressType = defaultHoldInfo
        ) // Hold 7

        is Command.Targeting.CycleLockAttackersNext -> InputAction.KeyEvent(key = "4") // Tap 4
        is Command.Targeting.CycleLockAttackersClosest -> InputAction.KeyEvent(
            key = "4",
            pressType = defaultHoldInfo
        ) // Hold 4

        is Command.Targeting.CycleLockSubtargetsNext -> InputAction.KeyEvent(key = "r") // Tap R
        is Command.Targeting.ResetSubtargetToMain -> InputAction.KeyEvent(
            key = "r",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+R

        is Command.Targeting.PinTarget1 -> InputAction.KeyEvent(
            key = "1",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+1

        is Command.Targeting.PinTarget2 -> InputAction.KeyEvent(
            key = "2",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+2

        is Command.Targeting.PinTarget3 -> InputAction.KeyEvent(
            key = "3",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+3

        is Command.Targeting.RemoveAllPinnedTargets -> InputAction.KeyEvent(
            key = "0",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+0
        is Command.Targeting.ToggleLookAhead -> InputAction.KeyEvent(
            key = "l",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+L (Look behind?)

        // New Targeting Commands (mapped to existing where appropriate)
        is Command.Targeting.TargetNearestHostile -> InputAction.KeyEvent(key = "5") // Tap 5
        is Command.Targeting.CycleTargetsForward -> InputAction.KeyEvent(key = "t") // Tap T
        is Command.Targeting.CycleTargetsBackward -> InputAction.KeyEvent(key = "y") // Tap Y (needs binding)
        is Command.Targeting.CycleSubtargetsForward -> InputAction.KeyEvent(key = "r") // Tap R
        is Command.Targeting.CycleSubtargetsBackward -> InputAction.KeyEvent(
            key = "r",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+R
        is Command.Targeting.PinSelectedTarget -> InputAction.KeyEvent(
            key = "1",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+1
        is Command.Targeting.UnpinSelectedTarget -> InputAction.KeyEvent(
            key = "0",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+0


        // --- Combat (Pilot Weapons) ---
        is Command.CombatPilot.FireWeaponGroup1 -> InputAction.MouseEvent(MouseButton.LEFT)
        is Command.CombatPilot.FireWeaponGroup2 -> InputAction.MouseEvent(MouseButton.RIGHT)
        is Command.CombatPilot.CycleGimbalMode -> InputAction.KeyEvent(key = "g") // Tap G (Long press handled differently if needed)
        is Command.CombatPilot.ToggleMissileOperatorMode -> InputAction.MouseEvent(MouseButton.MIDDLE)
        is Command.CombatPilot.LaunchMissile -> InputAction.MouseEvent(MouseButton.LEFT) // In Missile Mode
        is Command.CombatPilot.CycleMissileType -> InputAction.KeyEvent(key = "g") // Tap G cycles missiles too
        is Command.CombatPilot.IncreaseArmedMissiles -> InputAction.KeyEvent(key = "g") // Tap G
        is Command.CombatPilot.ResetArmedMissiles -> InputAction.KeyEvent(
            key = "g",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // Alt+G
        // New Combat Commands
        is Command.CombatPilot.CycleFireMode -> InputAction.KeyEvent(key = "v") // Tap V (needs binding)


        // --- Countermeasures ---
        is Command.Countermeasures.DeployDecoyPanic -> InputAction.KeyEvent(key = "h") // Tap H
        is Command.Countermeasures.DeployDecoyBurst -> InputAction.KeyEvent(key = "h") // Tap H
        is Command.Countermeasures.SetLaunchDecoyBurst -> InputAction.KeyEvent(
            key = "h",
            pressType = setLaunchDecoyHoldInfo
        ) // Hold H

        is Command.Countermeasures.IncreaseDecoyBurstSize -> InputAction.KeyEvent(
            key = "h",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        ) // RAlt+H

        is Command.Countermeasures.DecreaseDecoyBurstSize -> InputAction.KeyEvent(
            key = "h",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        ) // LAlt+H

        is Command.Countermeasures.DeployNoise -> InputAction.KeyEvent(key = "j") // Tap J
        // New CM Commands (mapped)
        is Command.Countermeasures.LaunchDecoy -> InputAction.KeyEvent(key = "h") // Tap H
        is Command.Countermeasures.LaunchNoise -> InputAction.KeyEvent(key = "j") // Tap J


        // --- Scanning ---
        is Command.Scanning.ToggleScanningMode -> InputAction.KeyEvent(key = "v")
        is Command.Scanning.ActivatePing -> InputAction.KeyEvent(key = "tab")
        is Command.Scanning.ActivateScanTargeted -> InputAction.MouseEvent(MouseButton.LEFT) // In Scan Mode
        is Command.Scanning.IncreaseScanAngleFocus -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.UP)
        is Command.Scanning.DecreaseScanAngleFocus -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.DOWN)

        // --- General Cockpit ---
        is Command.GeneralCockpit.FlightReady -> InputAction.KeyEvent(key = "r")
        is Command.GeneralCockpit.ExitSeat -> InputAction.KeyEvent(
            key = "y",
            pressType = exitSeatHoldInfo
        ) // Hold Y

        is Command.GeneralCockpit.Eject -> InputAction.KeyEvent(
            key = "y",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        ) // RAlt+Y

        is Command.GeneralCockpit.SelfDestruct -> InputAction.KeyEvent(
            key = "backspace",
            pressType = selfDestructHoldInfo
        ) // Hold Backspace

        is Command.GeneralCockpit.EmergencyExitSeat -> InputAction.KeyEvent(
            key = "u",
            modifiers = listOf(ModifierKeys.SHIFT_LEFT)
        ) // U+LShift

        is Command.GeneralCockpit.TogglePortLockAll -> InputAction.KeyEvent(
            key = "k",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        ) // RAlt+K (Lock Vehicle)

        is Command.GeneralCockpit.ToggleAllDoors -> null // Requires manual binding
        is Command.GeneralCockpit.ToggleLockAllDoors -> null // Requires manual binding
        // New General Commands (mapped where possible)
        is Command.GeneralCockpit.OpenAllDoors -> null // Map to ToggleAllDoors if bound
        is Command.GeneralCockpit.CloseAllDoors -> null // Map to ToggleAllDoors if bound
        is Command.GeneralCockpit.LockDoors -> InputAction.KeyEvent(
            key = "k",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        ) // Map to TogglePortLockAll
        is Command.GeneralCockpit.UnlockDoors -> InputAction.KeyEvent(
            key = "k",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        ) // Map to TogglePortLockAll


        // --- Ship Salvage --- (Keep existing mappings)
        is Command.ShipSalvage.ToggleSalvageMode -> InputAction.KeyEvent(key = "m")
        is Command.ShipSalvage.ToggleSalvageGimbal -> InputAction.KeyEvent(key = "g")
        is Command.ShipSalvage.ResetSalvageGimbal -> InputAction.KeyEvent(
            key = "g",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        is Command.ShipSalvage.ToggleSalvageBeamFocused -> InputAction.MouseEvent(MouseButton.LEFT)
        is Command.ShipSalvage.ToggleSalvageBeamLeft -> InputAction.KeyEvent(
            key = "a",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        )
        is Command.ShipSalvage.ToggleSalvageBeamRight -> InputAction.KeyEvent(
            key = "d",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        )
        is Command.ShipSalvage.ToggleSalvageBeamFracture -> InputAction.KeyEvent(
            key = "w",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        )
        is Command.ShipSalvage.ToggleSalvageBeamDisintegrate -> InputAction.KeyEvent(
            key = "s",
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        )
        is Command.ShipSalvage.CycleSalvageModifiers -> InputAction.MouseEvent(MouseButton.RIGHT)
        is Command.ShipSalvage.AdjustSalvageBeamSpacingUp -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.UP)
        is Command.ShipSalvage.AdjustSalvageBeamSpacingDown -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.DOWN)
        is Command.ShipSalvage.ToggleSalvageBeamAxis -> InputAction.MouseEvent(
            button = MouseButton.RIGHT,
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        is Command.ShipSalvage.FocusSalvageHeadsAll -> InputAction.KeyEvent(
            key = "s",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        is Command.ShipSalvage.FocusSalvageHeadLeft -> InputAction.KeyEvent(
            key = "a",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        is Command.ShipSalvage.FocusSalvageHeadRight -> InputAction.KeyEvent(
            key = "d",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        is Command.ShipSalvage.FocusSalvageToolFracture -> InputAction.KeyEvent(
            key = "w",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        is Command.ShipSalvage.IncreaseSalvageBeamSpacing -> null
        is Command.ShipSalvage.DecreaseSalvageBeamSpacing -> null
        is Command.ShipSalvage.SetAbsoluteSalvageBeamSpacing -> null

        // --- Ship Mining --- (Keep existing mappings)
        is Command.ShipMining.ToggleMiningMode -> InputAction.KeyEvent(key = "m")
        is Command.ShipMining.FireMiningLaser -> InputAction.MouseEvent(MouseButton.LEFT)
        is Command.ShipMining.SwitchMiningLaser -> InputAction.MouseEvent(
            button = MouseButton.LEFT,
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        is Command.ShipMining.IncreaseMiningLaserPower -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.UP)
        is Command.ShipMining.DecreaseMiningLaserPower -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.DOWN)
        is Command.ShipMining.CycleMiningLaserGimbal -> InputAction.KeyEvent(key = "g")
        is Command.ShipMining.ActivateMiningConsumable1 -> InputAction.KeyEvent(
            key = "1",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        is Command.ShipMining.ActivateMiningConsumable2 -> InputAction.KeyEvent(
            key = "2",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        is Command.ShipMining.ActivateMiningConsumable3 -> InputAction.KeyEvent(
            key = "3",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        is Command.ShipMining.JettisonCargo -> InputAction.KeyEvent(
            key = "j",
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )

        // --- Turret Gunner Systems --- (Keep existing mappings)
        is Command.Turret.ToggleTurretAimMode -> InputAction.KeyEvent(key = "q")
        is Command.Turret.RecenterTurret -> InputAction.KeyEvent(
            key = "c",
            pressType = recenterTurretHoldInfo
        )
        is Command.Turret.ChangeTurretPosition -> InputAction.KeyEvent(key = "s")
        is Command.Turret.AdjustTurretSpeedLimiterUp -> InputAction.MouseScroll(
            direction = InputAction.MouseScroll.Direction.UP,
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        is Command.Turret.AdjustTurretSpeedLimiterDown -> InputAction.MouseScroll(
            direction = InputAction.MouseScroll.Direction.DOWN,
            modifiers = listOf(ModifierKeys.ALT_LEFT)
        )
        is Command.Turret.FireTurretWeapons -> InputAction.MouseEvent(MouseButton.LEFT)
        is Command.Turret.ToggleTurretPrecisionTargeting -> InputAction.MouseEvent(MouseButton.RIGHT)
        is Command.Turret.ZoomTurretPrecisionTargeting -> InputAction.MouseEvent(
            button = MouseButton.RIGHT,
            pressType = zoomTurretHoldInfo
        )
        is Command.Turret.CycleTurretPrecisionMode -> InputAction.MouseEvent(
            button = MouseButton.RIGHT,
            modifiers = listOf(ModifierKeys.ALT_RIGHT)
        )
        is Command.Turret.ToggleTurretGyroStabilization -> InputAction.KeyEvent(key = "e")
        is Command.Turret.SwitchToNextRemoteTurret -> InputAction.KeyEvent(key = "d")
        is Command.Turret.SwitchToPreviousRemoteTurret -> InputAction.KeyEvent(key = "a")
        is Command.Turret.ExitTurret -> InputAction.KeyEvent(
            key = "y",
            pressType = exitSeatHoldInfo
        )
        is Command.Turret.CycleTurretFireMode -> null
        is Command.Turret.ToggleTurretESP -> null

        // else -> null // Optional default case if you want to explicitly handle unmapped commands
    }
}
