# StarButtonBox

<p align="center">
  <img src="./app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp" alt="App Icon">
</p>

## 1. Project Overview

StarButtonBox is a two-part application designed to provide a customizable virtual button box experience on an Android device for controlling the game Star Citizen running on a PC. It aims to offer a more immersive and efficient way to manage complex in-game actions.

The system consists of:

* **Android Application (Client):** Built with Kotlin and Jetpack Compose, this app serves as the user interface. It displays various buttons organized into customizable layouts (tabs). When a button is pressed, the app sends a corresponding command over the local network using UDP to the PC server.
* **Python Server (PC Host):** A Python script that runs on the PC where Star Citizen is played. It listens for UDP packets from the Android app. Upon receiving a command, it parses the data and uses the `pydirectinput` library to simulate the appropriate keyboard or mouse inputs on the PC.

## 2. Core Components & Architecture

### 2.1. Android Application (Client)

* **Technology Stack:** Kotlin, Jetpack Compose (Material 3), Hilt (Dependency Injection), Kotlinx Serialization, Room (Database), Jetpack DataStore (Preferences), Jetpack Navigation.
* **User Interface (UI):**
    * **Layouts/Tabs:** Users can switch between different layouts (e.g., "Normal Flight," "Mining," "Combat").
    * **Button Types:**
        * `MomentaryButton`: Standard press-and-release action.
        * `SafetyButton`: Requires a cover to be slid open before the underlying action button can be pressed, preventing accidental activation.
    * **Free-Form Layouts:** Users can create custom layouts where buttons can be freely placed, resized, and customized (label, color, associated macro) on a grid.
    * **Layout Management:**
        * Add, edit, and delete user-defined layouts.
        * Reorder layouts/tabs.
        * Enable/disable layouts (show/hide tabs).
        * Import/Export layout configurations (JSON format).
    * **Settings:** Configuration for PC server connection (IP/Port), "Keep Screen On" preference.
* **Command & Macro System:**
    * **`Macro`:** Represents a game action (e.g., "Toggle Landing Gear"). Each macro is associated with an `InputAction`. Macros are stored in a Room database and can be pre-defined or user-customized.
    * **`InputAction`:** A sealed interface defining the specific input to be simulated on the PC (e.g., `KeyEvent`, `MouseEvent`, `MouseScroll`), including details like key, modifiers, press type (tap/hold), duration, mouse button, scroll direction/amount. Serialized to JSON for network transmission.
    * **Default Macros:** The app is pre-populated with a comprehensive list of default macros for Star Citizen, loaded from a JSON resource file (`default_macros.json`).
* **Networking & Connection Management (`ConnectionManager`):**
    * **Communication Protocol:** UDP.
    * **`UdpPacket` Structure:** All communication uses a standardized `UdpPacket` data class containing:
        * `packetId` (String, UUID): Unique ID for the packet.
        * `timestamp` (Long): Client or server timestamp depending on the packet.
        * `type` (`UdpPacketType` enum): `HEALTH_CHECK_PING`, `HEALTH_CHECK_PONG`, `MACRO_COMMAND`, `MACRO_ACK`.
        * `payload` (String?): JSON string of the `InputAction` for `MACRO_COMMAND`, or null/minimal data for others.
    * **Server Discovery (mDNS/NSD):**
        * The Android app uses Android's Network Service Discovery (NSD) to find the Python server on the local network.
        * It searches for services of type `_starbuttonbox._udp.local.`.
    * **Health Checks:**
        * The app periodically (every 10 seconds) sends `HEALTH_CHECK_PING` packets to the server.
        * The server responds with `HEALTH_CHECK_PONG` (echoing `packetId` and its own timestamp).
        * This mechanism is used to monitor server responsiveness and calculate one-way latency.
    * **Macro Command Acknowledgement:**
        * When the app sends a `MACRO_COMMAND` packet (containing the `InputAction` as payload), the server responds with a `MACRO_ACK` packet (echoing the `packetId` and its own timestamp).
        * This confirms that the server received the command.
    * **Response Time Calculation:** One-way latency (client to server) is calculated using `serverResponseTimestamp - clientSendTimestamp` from both PONG and ACK packets. This is displayed in the UI.
    * **Connection Status (`ConnectionStatus` enum):**
        * `NO_CONFIG`: No server IP/Port is configured.
        * `CONNECTING`: Actively trying to establish/verify connection (initial state, after loss, or during health checks).
        * `CONNECTED`: Server is responsive (2+ consecutive health checks successful or a MACRO_ACK received), and no macro ACKs are pending.
        * `SENDING_PENDING_ACK`: Server is responsive, but waiting for one or more `MACRO_ACK`s.
        * `CONNECTION_LOST`: Server is unresponsive (3+ consecutive health check failures or a `MACRO_COMMAND` ACK times out after 2 seconds).
    * **UI Indicators:** The `MainScreen` displays:
        * An animated icon representing the current `ConnectionStatus`.
        * The calculated server response time (e.g., "50ms").
* **User Feedback:**
    * **Sound Effects:** Plays sounds for button presses, cover interactions (using `SoundPlayer` and `SoundPool`).
    * **Haptic Feedback:** Provides vibration for button presses (using `VibratorManagerUtils`).
* **Persistence:**
    * `SettingDatasource` (Jetpack DataStore): Stores network configuration (IP/Port), "Keep Screen On" preference.
    * `LayoutRepository` (Jetpack DataStore): Stores layout definitions, order, and FreeForm item configurations (as JSON within layout definitions).
    * `MacroRepository` (Room DB): Stores `MacroEntity` objects.
* **Key Classes & Modules (Android):**
    * **DI (`AppModule`, `DatabaseModule`, `DataBindingModule`):** Hilt for dependency injection.
    * **Navigation (`AppScreenRoute`, `MainActivity`):** Jetpack Navigation for screen transitions.
    * **ViewModels:**
        * `MainViewModel`: Orchestrates main screen UI state, layout selection, and delegates connection/command sending.
        * `SettingViewModel`: Manages settings screen, server discovery (NSD), and manual connection configuration.
        * `SendMacroViewModel`: Responsible for fetching macro details and initiating the send process via `ConnectionManager`.
        * `ManageLayoutsViewModel`: Handles logic for the layout management screen.
        * `FeedbackViewModel`: Centralizes sound and vibration feedback.
    * **Data (`data` package):** `InputAction.kt`, `Macro.kt`, `LayoutDefinition.kt`, `FreeFormItemState.kt`, `NetworkComms.kt` (defines `UdpPacket`, `UdpPacketType`, `ConnectionStatus`).
    * **Datasource (`datasource` package):** `ConnectionManager.kt`, `LayoutRepository.kt`, `SettingDatasource.kt`, Room components (`AppDatabase.kt`, `MacroDao.kt`, etc.).
    * **UI (`ui` package):** Screens, dialogs, custom button composables (`MomentaryButton.kt`, `SafetyButton.kt`), layout composables (`FreeFormLayout.kt`, `NormalFlightLayout.kt`).
    * **Utilities (`utils` package):** `IconMapper.kt`, `ColorUtils.kt`, `SoundPlayer.kt`, `VibratorManagerUtils.kt`.

### 2.2. Python Server (PC Host)

* **Technology Stack:** Python 3.
* **Core Libraries:**
    * `socket`: For UDP network communication.
    * `pydirectinput`: For simulating keyboard and mouse inputs on the PC.
    * `json`: For parsing incoming command data and serializing responses.
    * `threading`: Used for mDNS service registration (though the main loop is single-threaded for command processing).
    * `zeroconf`: For mDNS service registration and discovery.
    * `ipaddress`: For network calculations (broadcast address).
    * `signal`: For graceful shutdown.
* **Functionality:**
    * **Command Listener:** Listens on a specific UDP port (e.g., 5005) for incoming `UdpPacket` JSON strings from the Android app.
    * **Packet Parsing:**
        * Deserializes the `UdpPacket` JSON.
        * Identifies `packetId`, `timestamp`, `type`, and `payload`.
    * **mDNS Service Registration:**
        * Registers itself on the local network using mDNS (Zeroconf) with a service type like `_starbuttonbox._udp.local.`.
        * Advertises its hostname, command port, and IP address.
    * **Health Check Handling:**
        * On receiving a `HEALTH_CHECK_PING` packet, it extracts the `packetId`.
        * Responds immediately with a `HEALTH_CHECK_PONG` packet, echoing the `packetId` and including its own current timestamp.
    * **Macro Command Processing:**
        * On receiving a `MACRO_COMMAND` packet:
            * Extracts the `payload`, which is a JSON string representing an `InputAction` (e.g., key press, mouse click).
            * Deserializes the `InputAction` JSON from the payload.
            * Calls the appropriate `execute_key_event`, `execute_mouse_event`, or `execute_mouse_scroll` function based on the `InputAction` type.
            * Sends a `MACRO_ACK` packet (echoing the `packetId` and its own timestamp) back to the client to confirm receipt.
    * **Input Simulation:**
        * `execute_key_event`: Simulates key presses, holds, and releases, including modifier keys (Shift, Ctrl, Alt).
        * `execute_mouse_event`: Simulates mouse button clicks and holds.
        * `execute_mouse_scroll`: Simulates mouse wheel scrolling.
* **Key Files (Python):**
    * `server.py`: The single script containing all server-side logic.

## 3. Setup & Dependencies

### 3.1. Android Application

* Standard Android Studio project.
* Dependencies managed via `build.gradle.kts` (see file for specific versions):
    * Jetpack Compose (UI, Material 3, Animation, Navigation)
    * Hilt (Dependency Injection)
    * Kotlinx Serialization (JSON parsing)
    * Room (Local database for macros)
    * Jetpack DataStore (Preferences for settings)
    * Android Network Service Discovery (NSD) APIs

### 3.2. Python Server

* Python 3 environment.
* Required libraries (install via pip):
    ```bash
    pip install pydirectinput zeroconf
    ```

## 4. Network Communication Details

* **Discovery:** Client uses mDNS (NSD) to find servers advertising `_starbuttonbox._udp.local.`.
* **Main Communication:** UDP packets are used for all subsequent communication (health checks, commands, ACKs).
* **Packet Format:** All UDP messages (after initial discovery) are JSON strings representing the `UdpPacket` data class defined in `NetworkComms.kt`.
    * **`MACRO_COMMAND` payload:** A JSON string representing an `InputAction` object.
    * **`HEALTH_CHECK_PONG` and `MACRO_ACK` payload:** Typically `null`. Their primary purpose is to confirm receipt and carry the echoed `packetId` and server timestamp.
* **Ports:**
    * **Command/Health/ACK Port (e.g., 5005 UDP):** Used by the Python server to listen for PINGs and MACRO_COMMANDs, and to send PONGs and ACKs. The Android client sends to this port.
    * **mDNS Port (5353 UDP):** Standard mDNS port used by Zeroconf/NSD for discovery.

## 5. Current State & Key Considerations for Future Development

* **Robust Networking:** The system now includes mDNS discovery, health checks, macro command acknowledgements, response time calculation, and a UI connection status indicator. This significantly improves reliability and user feedback compared to a simple fire-and-forget UDP mechanism.
* **Error Handling:** Basic error handling is in place for network operations, JSON parsing, and input simulation. This can always be expanded.
* **Extensibility:** The `InputAction` sealed interface and `UdpPacketType` enum are designed to be extensible if new command types or packet interactions are needed.
* **UI/UX:** The Android UI uses Jetpack Compose with Material 3 components. Further refinements to user experience, layout customization, and visual feedback are always possible.
* **Server Security:** The current UDP communication is unencrypted and unauthenticated. For use strictly on a trusted local network, this might be acceptable. If broader network access or security is a concern, implementing encryption (e.g., DTLS) or an authentication mechanism would be a significant undertaking.
* **Emulator Limitations:** mDNS discovery does not reliably work on the Android Emulator due to its network virtualization. Physical devices are required for testing discovery. Manual IP/Port configuration remains essential for emulator-based development.
* **Configuration Synchronization:** Currently, macro definitions are primarily managed on the Android side (with defaults loaded from app resources). There's no direct synchronization of macro *definitions* between the client and a central server store beyond the initial defaults.

