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
    MACRO_ACK          // Server to App
}

/**
 * Represents a generic UDP packet structure for communication.
 *
 * @param packetId A unique identifier for this specific packet (e.g., UUID string).
 * @param timestamp The time the packet was created/sent (e.g., System.currentTimeMillis()).
 * @param type The type of the packet, indicating its purpose.
 * @param payload The actual data being sent, typically a JSON string.
 * For MACRO_COMMAND, this will be the serialized InputAction.
 * For PING/PONG/ACK, this might be empty or contain minimal info.
 */
@Serializable
data class UdpPacket(
    val packetId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: UdpPacketType,
    val payload: String? = null // Payload can be nullable, e.g., for simple PONG/ACK
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
    SENDING_PENDING_ACK, // Renamed for clarity

    /** Connection to the server has been lost (e.g., multiple failed health checks or lost macro packets). */
    CONNECTION_LOST
}
