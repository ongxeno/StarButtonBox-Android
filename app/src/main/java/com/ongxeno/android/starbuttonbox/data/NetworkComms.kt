package com.ongxeno.android.starbuttonbox.data

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Defines the different types of UDP packets that can be exchanged
 * between the Android app and the PC server.
 */
@Serializable // Make enum serializable if it's part of UdpPacket directly
enum class UdpPacketType {
    HEALTH_CHECK_PING, // App to Server
    HEALTH_CHECK_PONG, // Server to App
    MACRO_COMMAND,     // App to Server
    MACRO_ACK,         // Server to App
    TRIGGER_IMPORT_BROWSER, // App to Server
    CAPTURE_MOUSE_POSITION, // App to Server - New for Auto Drag
    AUTO_DRAG_LOOP_COMMAND  // App to Server - New for Auto Drag
    // Optional: AUTO_DRAG_STATUS_UPDATE (Server to App) - Can be added later
}

/**
 * Represents a generic UDP packet structure for communication.
 *
 * @param packetId A unique identifier for this specific packet (e.g., UUID string).
 * @param timestamp The time the packet was created/sent (e.g., System.currentTimeMillis()).
 * @param type The type of the packet, indicating its purpose.
 * @param payload The actual data being sent, typically a JSON string.
 * For MACRO_COMMAND, this will be the serialized InputAction.
 * For TRIGGER_IMPORT_BROWSER, this will be the serialized TriggerImportPayload.
 * For CAPTURE_MOUSE_POSITION, this will be the serialized CaptureMousePayload.
 * For AUTO_DRAG_LOOP_COMMAND, this will be the serialized AutoDragLoopPayload.
 * For PING/PONG/ACK, this might be empty or contain minimal info.
 */
@Serializable
data class UdpPacket(
    val packetId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: UdpPacketType,
    val payload: String? = null // Payload can be nullable
)

/**
 * Represents the various states of the network connection to the PC server.
 * This will be used to update the UI (e.g., connection indicator).
 */
enum class ConnectionStatus {
    /** No network configuration (IP/Port) has been saved by the user. */
    NO_CONFIG,

    /** Actively trying to establish or re-establish a connection (e.g., during initial health checks or after a loss). */
    CONNECTING,

    /** Connection is healthy, server is responsive, and no outstanding packets. */
    CONNECTED,

    /** Connection is healthy, but there are macro commands sent for which acknowledgements are pending. */
    SENDING_PENDING_ACK,

    /** Connection to the server has been lost (e.g., multiple failed health checks or lost macro packets). */
    CONNECTION_LOST
}

/**
 * Payload specific for the TRIGGER_IMPORT_BROWSER packet.
 * Contains the URL of the Ktor server running on the Android device.
 */
@Serializable
data class TriggerImportPayload(
    val url: String
)

// --- New Payloads for Auto Drag and Drop ---

/**
 * Payload for the CAPTURE_MOUSE_POSITION packet.
 * Specifies whether the captured position is for the source (SRC) or destination (DES).
 *
 * @param purpose A string indicating the purpose, e.g., "SRC" or "DES".
 */
@Serializable
data class CaptureMousePayload(
    val purpose: String // "SRC" or "DES"
)

/**
 * Payload for the AUTO_DRAG_LOOP_COMMAND packet.
 * Specifies whether to start or stop the auto drag loop on the PC server.
 *
 * @param action A string indicating the action, e.g., "START" or "STOP".
 */
@Serializable
data class AutoDragLoopPayload(
    val action: String // "START" or "STOP"
)
