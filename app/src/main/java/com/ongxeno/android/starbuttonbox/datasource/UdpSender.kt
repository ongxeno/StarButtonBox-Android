package com.ongxeno.android.starbuttonbox.datasource // Or your preferred package

import android.util.Log
// No longer need Command import here
import com.ongxeno.android.starbuttonbox.data.InputAction
import com.ongxeno.android.starbuttonbox.data.mapCommandIdentifierToAction // Import the mapper function
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel // Import cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Handles serializing InputActions (obtained by mapping command identifier strings)
 * and sending them as JSON strings via UDP packets.
 * Creates a new socket for each send operation.
 *
 * @param targetIpAddress The IP address of the receiving PC server.
 * @param targetPort The port number the receiving PC server is listening on.
 */
class UdpSender(
    private val targetIpAddress: String,
    private val targetPort: Int
) {

    companion object {
        private const val TAG = "UdpSender" // Tag for logging
    }

    // Create a dedicated scope for this sender's operations
    // Using Dispatchers.IO for network operations
    private val senderScope = CoroutineScope(Dispatchers.IO)

    // Initialize Json instance internally
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    /**
     * Maps the given command identifier string to its corresponding InputAction using the
     * mapCommandIdentifierToAction() function and sends it.
     * Logs a warning if no mapping is found for the command identifier.
     *
     * @param commandIdentifier The unique string identifying the command (e.g., "Flight.Boost").
     */
    fun sendCommandAction(commandIdentifier: String) {
        val inputAction: InputAction? = mapCommandIdentifierToAction(commandIdentifier)

        if (inputAction != null) {
            sendActionInternal(inputAction, commandIdentifier)
        } else {
            Log.w(TAG, "No InputAction mapped for command identifier: $commandIdentifier. Nothing sent.")
        }
    }

    /**
     * Internal function to serialize and send a resolved InputAction via UDP.
     *
     * @param inputAction The InputAction object to send.
     * @param originalCommandIdentifier The string identifier of the original command for logging.
     */
    private fun sendActionInternal(inputAction: InputAction, originalCommandIdentifier: String) {
        // Launch network operation in the sender's scope
        senderScope.launch {
            var socket: DatagramSocket? = null
            val jsonString = try {
                json.encodeToString(inputAction)
            } catch (e: Exception) {
                Log.e(TAG, "Error serializing action for command '$originalCommandIdentifier': $inputAction", e)
                return@launch // Stop if serialization fails
            }

            try {
                // Prepare and Send UDP Packet
                socket = DatagramSocket() // Create socket for sending THIS packet
                val address: InetAddress = InetAddress.getByName(targetIpAddress)
                val data: ByteArray = jsonString.toByteArray(Charsets.UTF_8)
                val packet = DatagramPacket(data, data.size, address, targetPort)

                socket.send(packet)

                Log.d(TAG, "Sent JSON for '$originalCommandIdentifier': $jsonString to $targetIpAddress:$targetPort")

            } catch (e: Exception) {
                Log.e(TAG, "Error sending UDP packet for command '$originalCommandIdentifier'", e)
            } finally {
                // Ensure the socket created for THIS send operation is closed
                try {
                    socket?.close()
                    Log.d(TAG, "Socket closed for command '$originalCommandIdentifier'.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing socket for command '$originalCommandIdentifier'.", e)
                }
            }
        }
    }

    /**
     * Overload to directly send a pre-formatted JSON string.
     * Useful for testing or specific scenarios.
     */
    fun sendJsonString(jsonString: String) {
        senderScope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket() // Create socket for sending THIS packet
                val address: InetAddress = InetAddress.getByName(targetIpAddress)
                val data: ByteArray = jsonString.toByteArray(Charsets.UTF_8)
                val packet = DatagramPacket(data, data.size, address, targetPort)
                socket.send(packet)
                Log.d(TAG, "Sent raw JSON string: $jsonString to $targetIpAddress:$targetPort")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending raw JSON string", e)
            } finally {
                // Ensure the socket created for THIS send operation is closed
                try {
                    socket?.close()
                    Log.d(TAG, "Socket closed for raw JSON string send.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error closing socket for raw JSON string send.", e)
                }
            }
        }
    }

    /**
     * Cleans up resources used by the UdpSender.
     * In this implementation, it cancels any ongoing coroutines launched by the senderScope.
     * Sockets are closed individually after each send operation.
     */
    fun close() {
        Log.d(TAG, "Closing UdpSender. Cancelling senderScope.")
        // Cancel the scope to stop any ongoing or pending send operations
        try {
            senderScope.cancel() // Cancels all coroutines launched in this scope
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling senderScope", e)
        }
        // No persistent socket to close here, as they are closed in finally blocks.
    }
}
