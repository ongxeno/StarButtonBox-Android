# StarButtonBox

<p align="center">
  <img src="./app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp" alt="App Icon">
</p>

## Project Overview

This project creates a virtual button box application for the game Star Citizen. It consists of two main parts:

1. **`Android Application:`** An app built with `Kotlin` and `Jetpack Compose` that serves as the user interface. It displays various buttons organized into tabs (like "Normal Flight") corresponding to in-game actions. When a button is pressed, the app sends the corresponding command over the network using UDP.

2. **`Python Server:`** A Python script intended to run on the PC where Star Citizen is played. It listens for UDP packets from the Android app. Upon receiving a command, it parses the JSON data and uses the `pydirectinput` library to simulate the appropriate keyboard or mouse inputs (including key presses, holds, and modifiers) on the PC.

## Key Features & Implementation Details

* **Command System:** Game commands are defined using `Kotlin` sealed classes (`Command.kt`). These are mapped to specific keyboard/mouse actions (`InputAction.kt`, `KeyMapper.kt`), which are then serialized to `JSON` for transmission.

* **UI:** The Android app uses `Jetpack Compose` for its user interface. It features different types of buttons suitable for a button box, including momentary buttons, buttons with timed visual feedback, and safety buttons (requiring a sliding gesture). Network settings (IP/Port) are configurable via a dialog and saved using Android's `DataStore`.

* **Networking:** Communication happens via `UDP`. The Android app sends `JSON` commands to the Python server's configured IP address and port.

* **Input Simulation:** The Python server (`server.py`) uses the `pydirectinput` library to translate the received `JSON` commands into actual keyboard and mouse inputs on the host PC.
