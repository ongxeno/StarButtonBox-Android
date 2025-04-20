package com.ongxeno.android.starbuttonbox.data // Or your preferred package

/**
 * Extension function to map a logical Command object to a specific physical InputAction.
 * This determines the actual key/mouse press details based on default bindings
 * from the Star Citizen Keybinding Compilation document (Alpha 4.1) and user additions.
 * Updated MouseScroll to include modifiers where applicable.
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
        is Command.Flight.Boost -> InputAction.KeyEvent(key = ModifierKeys.SHIFT_LEFT, pressType = defaultHoldInfo)
        is Command.Flight.Spacebrake -> InputAction.KeyEvent(key = "x")
        is Command.Flight.ToggleDecoupledMode -> InputAction.KeyEvent(key = "c", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.Flight.ToggleCruiseControl -> InputAction.KeyEvent(key = "c")
        is Command.Flight.AdjustSpeedLimiterUp -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.UP)
        is Command.Flight.AdjustSpeedLimiterDown -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.DOWN)
        is Command.Flight.ToggleVTOLMode -> InputAction.KeyEvent(key = "k")
        is Command.Flight.ToggleLockPitchYaw -> InputAction.KeyEvent(key = ModifierKeys.SHIFT_RIGHT)
        is Command.Flight.ToggleHeadLight -> InputAction.KeyEvent(key = "l")
        is Command.Flight.ToggleSpeedLimiter -> InputAction.MouseEvent(MouseButton.MIDDLE)

        // --- Quantum Travel ---
        is Command.QuantumTravel.ToggleQuantumMode -> InputAction.KeyEvent(key = "b")
        is Command.QuantumTravel.ActivateQuantumTravel -> InputAction.KeyEvent(key = "b", pressType = quantumEngageHoldInfo)

        // --- Landing & Docking ---
        is Command.LandingAndDocking.ToggleLandingGear -> InputAction.KeyEvent(key = "n")
        is Command.LandingAndDocking.AutoLand -> InputAction.KeyEvent(key = "n", pressType = autoLandHoldInfo)
        is Command.LandingAndDocking.RequestLandingTakeoff -> InputAction.KeyEvent(key = "n", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.LandingAndDocking.RequestDocking -> InputAction.KeyEvent(key = "n") // Context dependent
        is Command.LandingAndDocking.ToggleDockingCamera -> InputAction.KeyEvent(key = "0")

        // --- Power Management ---
        is Command.PowerManagement.TogglePowerWeapons -> InputAction.KeyEvent(key = "p")
        is Command.PowerManagement.TogglePowerShields -> InputAction.KeyEvent(key = "o")
        is Command.PowerManagement.TogglePowerEngines -> InputAction.KeyEvent(key = "i")
        is Command.PowerManagement.TogglePowerAll -> InputAction.KeyEvent(key = "u")
        is Command.PowerManagement.IncreasePowerWeapons -> InputAction.KeyEvent(key = "f5")
        is Command.PowerManagement.MaxPowerWeapons -> InputAction.KeyEvent(key = "f5", pressType = defaultHoldInfo)
        is Command.PowerManagement.IncreasePowerEngines -> InputAction.KeyEvent(key = "f6")
        is Command.PowerManagement.MaxPowerEngines -> InputAction.KeyEvent(key = "f6", pressType = defaultHoldInfo)
        is Command.PowerManagement.IncreasePowerShields -> InputAction.KeyEvent(key = "f7")
        is Command.PowerManagement.MaxPowerShields -> InputAction.KeyEvent(key = "f7", pressType = defaultHoldInfo)
        is Command.PowerManagement.ResetPowerDistribution -> InputAction.KeyEvent(key = "f8")
        is Command.PowerManagement.DecreasePowerWeapons -> InputAction.KeyEvent(key = "f5", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.PowerManagement.MinPowerWeapons -> InputAction.KeyEvent(key = "f5", modifiers = listOf(ModifierKeys.ALT_LEFT), pressType = defaultHoldInfo)
        is Command.PowerManagement.DecreasePowerEngines -> InputAction.KeyEvent(key = "f6", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.PowerManagement.MinPowerEngines -> InputAction.KeyEvent(key = "f6", modifiers = listOf(ModifierKeys.ALT_LEFT), pressType = defaultHoldInfo)
        is Command.PowerManagement.DecreasePowerShields -> InputAction.KeyEvent(key = "f7", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.PowerManagement.MinPowerShields -> InputAction.KeyEvent(key = "f7", modifiers = listOf(ModifierKeys.ALT_LEFT), pressType = defaultHoldInfo)

        // --- Targeting ---
        is Command.Targeting.LockSelectedTarget -> InputAction.KeyEvent(key = "t")
        is Command.Targeting.UnlockLockedTarget -> InputAction.KeyEvent(key = "t", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.Targeting.CycleLockHostilesNext -> InputAction.KeyEvent(key = "5")
        is Command.Targeting.CycleLockHostilesClosest -> InputAction.KeyEvent(key = "5", pressType = defaultHoldInfo)
        is Command.Targeting.CycleLockFriendliesNext -> InputAction.KeyEvent(key = "6")
        is Command.Targeting.CycleLockFriendliesClosest -> InputAction.KeyEvent(key = "6", pressType = defaultHoldInfo)
        is Command.Targeting.CycleLockAllNext -> InputAction.KeyEvent(key = "7")
        is Command.Targeting.CycleLockAllClosest -> InputAction.KeyEvent(key = "7", pressType = defaultHoldInfo)
        is Command.Targeting.CycleLockAttackersNext -> InputAction.KeyEvent(key = "4")
        is Command.Targeting.CycleLockAttackersClosest -> InputAction.KeyEvent(key = "4", pressType = defaultHoldInfo)
        is Command.Targeting.CycleLockSubtargetsNext -> InputAction.KeyEvent(key = "r")
        is Command.Targeting.ResetSubtargetToMain -> InputAction.KeyEvent(key = "r", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.Targeting.PinTarget1 -> InputAction.KeyEvent(key = "1", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.Targeting.PinTarget2 -> InputAction.KeyEvent(key = "2", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.Targeting.PinTarget3 -> InputAction.KeyEvent(key = "3", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.Targeting.RemoveAllPinnedTargets -> InputAction.KeyEvent(key = "0")
        is Command.Targeting.ToggleLookAhead -> InputAction.KeyEvent(key = "l", modifiers = listOf(ModifierKeys.ALT_LEFT))

        // --- Combat (Pilot Weapons) ---
        is Command.CombatPilot.FireWeaponGroup1 -> InputAction.MouseEvent(MouseButton.LEFT)
        is Command.CombatPilot.FireWeaponGroup2 -> InputAction.MouseEvent(MouseButton.RIGHT)
        is Command.CombatPilot.CycleGimbalMode -> InputAction.KeyEvent(key = "g", pressType = cycleGimbalHoldInfo)
        is Command.CombatPilot.ToggleMissileOperatorMode -> InputAction.MouseEvent(MouseButton.MIDDLE)
        is Command.CombatPilot.LaunchMissile -> InputAction.MouseEvent(MouseButton.LEFT)
        is Command.CombatPilot.CycleMissileType -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.UP)
        is Command.CombatPilot.IncreaseArmedMissiles -> InputAction.KeyEvent(key = "g")
        is Command.CombatPilot.ResetArmedMissiles -> InputAction.KeyEvent(key = "g", modifiers = listOf(ModifierKeys.ALT_LEFT))

        // --- Countermeasures ---
        is Command.Countermeasures.DeployDecoyPanic -> InputAction.KeyEvent(key = "h")
        is Command.Countermeasures.DeployDecoyBurst -> InputAction.KeyEvent(key = "h")
        is Command.Countermeasures.SetLaunchDecoyBurst -> InputAction.KeyEvent(key = "h", pressType = setLaunchDecoyHoldInfo)
        is Command.Countermeasures.IncreaseDecoyBurstSize -> InputAction.KeyEvent(key = "h", modifiers = listOf(ModifierKeys.ALT_RIGHT))
        is Command.Countermeasures.DecreaseDecoyBurstSize -> InputAction.KeyEvent(key = "h", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.Countermeasures.DeployNoise -> InputAction.KeyEvent(key = "j")

        // --- Scanning ---
        is Command.Scanning.ToggleScanningMode -> InputAction.KeyEvent(key = "v")
        is Command.Scanning.ActivatePing -> InputAction.KeyEvent(key = "tab")
        is Command.Scanning.ActivateScanTargeted -> InputAction.MouseEvent(MouseButton.LEFT)
        is Command.Scanning.IncreaseScanAngleFocus -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.UP)
        is Command.Scanning.DecreaseScanAngleFocus -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.DOWN)

        // --- General Cockpit ---
        is Command.GeneralCockpit.FlightReady -> InputAction.KeyEvent(key = "r")
        is Command.GeneralCockpit.ExitSeat -> InputAction.KeyEvent(key = "y", pressType = exitSeatHoldInfo)
        is Command.GeneralCockpit.Eject -> InputAction.KeyEvent(key = "y", modifiers = listOf(ModifierKeys.ALT_RIGHT))
        is Command.GeneralCockpit.SelfDestruct -> InputAction.KeyEvent(key = "backspace", pressType = selfDestructHoldInfo)
        is Command.GeneralCockpit.EmergencyExitSeat -> InputAction.KeyEvent(key = "u", modifiers = listOf(ModifierKeys.SHIFT_LEFT))
        is Command.GeneralCockpit.TogglePortLockAll -> InputAction.KeyEvent(key = "k", modifiers = listOf(ModifierKeys.ALT_RIGHT))
        is Command.GeneralCockpit.ToggleAllDoors -> null
        is Command.GeneralCockpit.ToggleLockAllDoors -> null

        // --- Ship Salvage ---
        is Command.ShipSalvage.ToggleSalvageMode -> InputAction.KeyEvent(key = "m")
        is Command.ShipSalvage.ToggleSalvageGimbal -> InputAction.KeyEvent(key = "g")
        is Command.ShipSalvage.ResetSalvageGimbal -> InputAction.KeyEvent(key = "g", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.ShipSalvage.ToggleSalvageBeamFocused -> InputAction.MouseEvent(MouseButton.LEFT)
        is Command.ShipSalvage.ToggleSalvageBeamLeft -> InputAction.KeyEvent(key = "a", modifiers = listOf(ModifierKeys.ALT_RIGHT))
        is Command.ShipSalvage.ToggleSalvageBeamRight -> InputAction.KeyEvent(key = "d", modifiers = listOf(ModifierKeys.ALT_RIGHT))
        is Command.ShipSalvage.ToggleSalvageBeamFracture -> InputAction.KeyEvent(key = "w", modifiers = listOf(ModifierKeys.ALT_RIGHT))
        is Command.ShipSalvage.ToggleSalvageBeamDisintegrate -> InputAction.KeyEvent(key = "s", modifiers = listOf(ModifierKeys.ALT_RIGHT))
        is Command.ShipSalvage.CycleSalvageModifiers -> InputAction.MouseEvent(MouseButton.RIGHT)
        is Command.ShipSalvage.AdjustSalvageBeamSpacingUp -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.UP)
        is Command.ShipSalvage.AdjustSalvageBeamSpacingDown -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.DOWN)
        is Command.ShipSalvage.ToggleSalvageBeamAxis -> InputAction.MouseEvent(button = MouseButton.RIGHT, modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.ShipSalvage.FocusSalvageHeadsAll -> InputAction.KeyEvent(key = "s", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.ShipSalvage.FocusSalvageHeadLeft -> InputAction.KeyEvent(key = "a", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.ShipSalvage.FocusSalvageHeadRight -> InputAction.KeyEvent(key = "d", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.ShipSalvage.FocusSalvageToolFracture -> InputAction.KeyEvent(key = "w", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.ShipSalvage.IncreaseSalvageBeamSpacing -> null
        is Command.ShipSalvage.DecreaseSalvageBeamSpacing -> null
        is Command.ShipSalvage.SetAbsoluteSalvageBeamSpacing -> null

        // --- Ship Mining ---
        is Command.ShipMining.ToggleMiningMode -> InputAction.KeyEvent(key = "m")
        is Command.ShipMining.FireMiningLaser -> InputAction.MouseEvent(MouseButton.LEFT)
        is Command.ShipMining.SwitchMiningLaser -> InputAction.MouseEvent(button = MouseButton.LEFT, modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.ShipMining.IncreaseMiningLaserPower -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.UP)
        is Command.ShipMining.DecreaseMiningLaserPower -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.DOWN)
        is Command.ShipMining.CycleMiningLaserGimbal -> InputAction.KeyEvent(key = "g")
        is Command.ShipMining.ActivateMiningConsumable1 -> InputAction.KeyEvent(key = "1", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.ShipMining.ActivateMiningConsumable2 -> InputAction.KeyEvent(key = "2", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.ShipMining.ActivateMiningConsumable3 -> InputAction.KeyEvent(key = "3", modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.ShipMining.JettisonCargo -> InputAction.KeyEvent(key = "j", modifiers = listOf(ModifierKeys.ALT_LEFT))

        // --- Turret Gunner Systems ---
        is Command.Turret.ToggleTurretAimMode -> InputAction.KeyEvent(key = "q")
        is Command.Turret.RecenterTurret -> InputAction.KeyEvent(key = "c", pressType = recenterTurretHoldInfo)
        is Command.Turret.ChangeTurretPosition -> InputAction.KeyEvent(key = "s")
        is Command.Turret.AdjustTurretSpeedLimiterUp -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.UP, modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.Turret.AdjustTurretSpeedLimiterDown -> InputAction.MouseScroll(direction = InputAction.MouseScroll.Direction.DOWN, modifiers = listOf(ModifierKeys.ALT_LEFT))
        is Command.Turret.FireTurretWeapons -> InputAction.MouseEvent(MouseButton.LEFT)
        is Command.Turret.ToggleTurretPrecisionTargeting -> InputAction.MouseEvent(MouseButton.RIGHT)
        is Command.Turret.ZoomTurretPrecisionTargeting -> InputAction.MouseEvent(button = MouseButton.RIGHT, pressType = zoomTurretHoldInfo)
        is Command.Turret.CycleTurretPrecisionMode -> InputAction.MouseEvent(button = MouseButton.RIGHT, modifiers = listOf(ModifierKeys.ALT_RIGHT))
        is Command.Turret.ToggleTurretGyroStabilization -> InputAction.KeyEvent(key = "e")
        is Command.Turret.SwitchToNextRemoteTurret -> InputAction.KeyEvent(key = "d")
        is Command.Turret.SwitchToPreviousRemoteTurret -> InputAction.KeyEvent(key = "a")
        is Command.Turret.ExitTurret -> InputAction.KeyEvent(key = "y", pressType = exitSeatHoldInfo)
        is Command.Turret.CycleTurretFireMode -> null
        is Command.Turret.ToggleTurretESP -> null

        // else -> null // Optional default case
    }
}