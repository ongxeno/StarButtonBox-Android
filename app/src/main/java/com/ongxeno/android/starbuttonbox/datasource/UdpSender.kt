package com.ongxeno.android.starbuttonbox.datasource // Or your preferred package

import android.util.Log
import com.ongxeno.android.starbuttonbox.data.Command
import com.ongxeno.android.starbuttonbox.data.InputAction
import com.ongxeno.android.starbuttonbox.data.toInputAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * Handles serializing InputActions (obtained by mapping Commands)
 * and sending them as JSON strings via UDP packets.
 *
 * @param targetIpAddress The IP address of the receiving PC server.
 * @param targetPort The port number the receiving PC server is listening on.
 * @param scope The CoroutineScope to launch network operations on (defaults to IO dispatcher).
 */
class UdpSender(
    private val targetIpAddress: String,
    private val targetPort: Int,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO) // Use IO dispatcher for network
) {

    companion object {
        private const val TAG = "UdpSender" // Tag for logging
    }

    // Initialize Json instance internally
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    /**
     * Maps the given Command to its corresponding InputAction using the
     * toInputAction() extension function and sends it.
     * Logs a warning if no mapping is found for the command.
     *
     * @param command The Command object to process and send.
     */
    fun sendCommandAction(command: Command) {
        // Use the Command.toInputAction() extension function directly
        val inputAction: InputAction? = command.toInputAction()

        if (inputAction != null) {
            sendActionInternal(inputAction, command.commandString) // Pass original command string for logging context
        } else {
            Log.w(TAG, "No InputAction mapped for command: ${command.commandString}. Nothing sent.")
            // Optional: Implement callback/state to notify UI about unmapped command
        }
    }

    /**
     * Internal function to serialize and send a resolved InputAction via UDP.
     *
     * @param inputAction The InputAction object to send.
     * @param originalCommandString The string representation of the original command for logging.
     */
    private fun sendActionInternal(inputAction: InputAction, originalCommandString: String) {
        scope.launch { // Launch network operation in the background
            var socket: DatagramSocket? = null
            val jsonString = try {
                json.encodeToString(inputAction)
            } catch (e: Exception) {
                Log.e(TAG, "Error serializing action for command '$originalCommandString': $inputAction", e)
                return@launch // Stop if serialization fails
            }

            try {
                // Prepare and Send UDP Packet
                socket = DatagramSocket() // Create socket for sending
                val address: InetAddress = InetAddress.getByName(targetIpAddress)
                val data: ByteArray = jsonString.toByteArray(Charsets.UTF_8)
                val packet = DatagramPacket(data, data.size, address, targetPort)

                socket.send(packet)

                Log.d(TAG, "Sent JSON for '$originalCommandString': $jsonString to $targetIpAddress:$targetPort")
                // Optional: Implement a callback or state flow here to notify UI of success

            } catch (e: Exception) {
                Log.e(TAG, "Error sending UDP packet for command '$originalCommandString'", e)
                // Optional: Implement a callback or state flow here to notify UI of failure
            } finally {
                socket?.close() // Ensure the socket is always closed
            }
        }
    }

    /**
     * Overload to directly send a pre-formatted JSON string.
     * Useful for testing or specific scenarios.
     */
    fun sendJsonString(jsonString: String) {
        scope.launch {
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                val address: InetAddress = InetAddress.getByName(targetIpAddress)
                val data: ByteArray = jsonString.toByteArray(Charsets.UTF_8)
                val packet = DatagramPacket(data, data.size, address, targetPort)
                socket.send(packet)
                Log.d(TAG, "Sent raw JSON string: $jsonString to $targetIpAddress:$targetPort")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending raw JSON string", e)
            } finally {
                socket?.close()
            }
        }
    }
}
