package com.ongxeno.android.starbuttonbox.datasource

import android.content.Context
import android.util.Log
import com.ongxeno.android.starbuttonbox.data.ConnectionStatus
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import com.ongxeno.android.starbuttonbox.data.UdpPacket
import com.ongxeno.android.starbuttonbox.data.UdpPacketType
import com.ongxeno.android.starbuttonbox.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

private const val HEALTH_CHECK_INTERVAL_MS = 5000L // 5 seconds
private const val PING_TIMEOUT_MS = 2000L // 2 seconds
private const val MAX_FAILED_HEALTH_CHECKS = 3
private const val MIN_SUCCESSFUL_HEALTH_CHECKS_FOR_CONNECTED = 2
private const val UDP_RECEIVE_BUFFER_SIZE = 2048 // Increased buffer for UdpPacket JSON

@Singleton
class ConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingDatasource: SettingDatasource,
    private val json: Json,
    @ApplicationScope private val appScope: CoroutineScope // Use application-level scope
) {
    private val TAG = "ConnectionManager"

    private var udpSocket: DatagramSocket? = null
    private var listenerJob: Job? = null
    private var healthCheckJob: Job? = null
    private var pingTimeoutJob: Job? = null

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.NO_CONFIG)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private var currentNetworkConfig: NetworkConfig? = null

    // Tracking for health checks
    private val _lastSuccessfulHealthCheckTime = MutableStateFlow<Long?>(null)
    // val lastSuccessfulHealthCheckTime: StateFlow<Long?> = _lastSuccessfulHealthCheckTime.asStateFlow()

    private val _consecutiveSuccessfulHealthChecks = MutableStateFlow(0)
    private val _consecutiveFailedHealthChecks = MutableStateFlow(0)

    // Map to store packetId and send timestamp of PINGs awaiting PONGs
    private val pendingPings = mutableMapOf<String, Long>()

    init {
        Log.d(TAG, "Initializing ConnectionManager.")
        // Observe network configuration changes
        appScope.launch {
            settingDatasource.networkConfigFlow.collectLatest { config ->
                Log.d(TAG, "Network config updated: $config")
                currentNetworkConfig = config
                if (config?.ip != null && config.port != null) {
                    if (_connectionStatus.value == ConnectionStatus.NO_CONFIG || _connectionStatus.value == ConnectionStatus.CONNECTION_LOST) {
                        Log.i(TAG, "Valid network config found. Current status: ${_connectionStatus.value}. Transitioning to CONNECTING.")
                        _connectionStatus.value = ConnectionStatus.CONNECTING
                    }
                    restartSocketAndJobs() // Restart with new config
                } else {
                    Log.w(TAG, "Invalid or missing network config. Setting status to NO_CONFIG.")
                    _connectionStatus.value = ConnectionStatus.NO_CONFIG
                    stopSocketAndJobs()
                }
            }
        }
    }

    private fun restartSocketAndJobs() {
        Log.d(TAG, "Restarting socket and jobs...")
        stopSocketAndJobs() // Ensure previous instances are closed

        val config = currentNetworkConfig
        if (config?.ip == null || config.port == null) {
            Log.w(TAG, "Cannot restart socket and jobs, network config is invalid.")
            _connectionStatus.value = ConnectionStatus.NO_CONFIG
            return
        }

        try {
            // Initialize and bind the UDP socket for sending and receiving
            // Binding to port 0 lets the OS pick an available ephemeral port
            udpSocket = DatagramSocket() // Let OS pick a port for sending
            Log.i(TAG, "UDP Socket created and bound to local port: ${udpSocket?.localPort}")

            // Start the listener for incoming packets (PONGs, ACKs)
            startListener()
            // Start periodic health checks
            startHealthChecks()
            // Start job to check for PING timeouts
            startPingTimeoutChecker()

            // Initial status if config is present
            if (_connectionStatus.value == ConnectionStatus.NO_CONFIG || _connectionStatus.value == ConnectionStatus.CONNECTION_LOST) {
                _connectionStatus.value = ConnectionStatus.CONNECTING
            }

        } catch (e: SocketException) {
            Log.e(TAG, "Error creating UDP socket: ${e.message}", e)
            _connectionStatus.value = ConnectionStatus.CONNECTION_LOST // Or a specific error state
            stopSocketAndJobs()
        }
    }

    private fun stopSocketAndJobs() {
        Log.d(TAG, "Stopping socket and jobs...")
        listenerJob?.cancel()
        healthCheckJob?.cancel()
        pingTimeoutJob?.cancel()
        listenerJob = null
        healthCheckJob = null
        pingTimeoutJob = null

        pendingPings.clear()
        _consecutiveFailedHealthChecks.value = 0
        _consecutiveSuccessfulHealthChecks.value = 0

        try {
            udpSocket?.close()
            Log.d(TAG, "UDP Socket closed.")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing UDP socket: ${e.message}")
        }
        udpSocket = null
    }

    private fun startListener() {
        val socket = udpSocket ?: return
        if (listenerJob?.isActive == true) {
            Log.d(TAG, "Listener job already active.")
            return
        }
        Log.d(TAG, "Starting UDP listener job.")
        listenerJob = appScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(UDP_RECEIVE_BUFFER_SIZE)
            while (isActive) {
                try {
                    val datagramPacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(datagramPacket) // Blocking call

                    val jsonData = String(datagramPacket.data, 0, datagramPacket.length)
                    Log.v(TAG, "Received UDP packet: $jsonData from ${datagramPacket.address.hostAddress}:${datagramPacket.port}")

                    try {
                        val receivedPacket = json.decodeFromString<UdpPacket>(jsonData)
                        processReceivedPacket(receivedPacket)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing received UDP packet: $jsonData", e)
                    }
                } catch (e: SocketTimeoutException) {
                    // Expected if socket has a timeout, can be ignored or used for keep-alive logic
                    Log.v(TAG, "Socket receive timeout.")
                } catch (e: SocketException) {
                    if (isActive) { // Only log if not intentionally closing
                        Log.e(TAG, "SocketException in listener (socket likely closed): ${e.message}")
                    }
                    break // Exit loop if socket is closed
                } catch (e: Exception) {
                    if (isActive) {
                        Log.e(TAG, "Error in UDP listener: ${e.message}", e)
                    }
                }
            }
            Log.d(TAG, "UDP listener job ended.")
        }
    }

    private fun processReceivedPacket(packet: UdpPacket) {
        Log.d(TAG, "Processing packet: ID=${packet.packetId}, Type=${packet.type}")
        when (packet.type) {
            UdpPacketType.HEALTH_CHECK_PONG -> {
                synchronized(pendingPings) {
                    if (pendingPings.containsKey(packet.packetId)) {
                        pendingPings.remove(packet.packetId)
                        Log.i(TAG, "HEALTH_CHECK_PONG received for ID: ${packet.packetId}")
                        _lastSuccessfulHealthCheckTime.value = System.currentTimeMillis()
                        _consecutiveFailedHealthChecks.value = 0
                        _consecutiveSuccessfulHealthChecks.value = (_consecutiveSuccessfulHealthChecks.value + 1).coerceAtMost(MIN_SUCCESSFUL_HEALTH_CHECKS_FOR_CONNECTED + 1)

                        if (_consecutiveSuccessfulHealthChecks.value >= MIN_SUCCESSFUL_HEALTH_CHECKS_FOR_CONNECTED &&
                            _connectionStatus.value != ConnectionStatus.CONNECTED &&
                            _connectionStatus.value != ConnectionStatus.SENDING_PENDING_ACK) { // Don't switch if sending
                            Log.i(TAG, "Connection established (PONG received). Status -> CONNECTED")
                            _connectionStatus.value = ConnectionStatus.CONNECTED
                        }
                    } else {
                        Log.w(TAG, "Received PONG for unknown or timed-out PING ID: ${packet.packetId}")
                    }
                }
            }
            UdpPacketType.MACRO_ACK -> {
                // To be implemented in Phase 3
                Log.d(TAG, "MACRO_ACK received (Phase 3): ID=${packet.packetId}")
            }
            else -> {
                Log.w(TAG, "Received unhandled packet type: ${packet.type}")
            }
        }
    }


    private fun startHealthChecks() {
        if (healthCheckJob?.isActive == true) {
            Log.d(TAG, "Health check job already active.")
            return
        }
        Log.d(TAG, "Starting health check job.")
        healthCheckJob = appScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(HEALTH_CHECK_INTERVAL_MS)
                val config = currentNetworkConfig
                if (config?.ip != null && config.port != null && udpSocket != null) {
                    sendHealthCheckPing()
                } else if (udpSocket == null) {
                    Log.w(TAG, "Health check: UDP socket is null, attempting to restart.")
                    // Attempt to restart socket if it's null but config is valid
                    // This handles cases where socket might have failed to initialize initially
                    // but config became valid later.
                    if (config?.ip != null && config.port != null){
                        withContext(Dispatchers.Main) { // Switch to main for restart logic if it touches UI state directly
                            Log.d(TAG, "Attempting to re-initialize socket for health check.")
                            restartSocketAndJobs()
                        }
                    }
                }
            }
            Log.d(TAG, "Health check job ended.")
        }
    }

    private fun sendHealthCheckPing() {
        val config = currentNetworkConfig ?: return
        val ip = config.ip ?: return
        val port = config.port ?: return
        val socket = udpSocket ?: return

        val pingPacket = UdpPacket(type = UdpPacketType.HEALTH_CHECK_PING, payload = null)
        val packetId = pingPacket.packetId // Get the generated ID
        val sendTime = pingPacket.timestamp

        try {
            val jsonData = json.encodeToString(pingPacket)
            val dataBytes = jsonData.toByteArray(Charsets.UTF_8)
            val datagramPacket = DatagramPacket(
                /* buf = */ dataBytes,
                /* length = */ dataBytes.size,
                /* address = */ InetAddress.getByName(ip),
                /* port = */ port
            )
            socket.send(datagramPacket)
            synchronized(pendingPings) {
                pendingPings[packetId] = sendTime
            }
            Log.i(TAG, "Sent HEALTH_CHECK_PING (ID: $packetId) to ${config.ip}:${config.port}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending HEALTH_CHECK_PING (ID: $packetId): ${e.message}", e)
            // Consider this a failed health check attempt immediately
            handlePingTimeout(packetId) // Reuse timeout logic for send failure
        }
    }

    private fun startPingTimeoutChecker() {
        if (pingTimeoutJob?.isActive == true) {
            Log.d(TAG, "Ping timeout checker job already active.")
            return
        }
        Log.d(TAG, "Starting PING timeout checker job.")
        pingTimeoutJob = appScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(PING_TIMEOUT_MS / 2) // Check more frequently than the timeout itself
                val currentTime = System.currentTimeMillis()
                val pingsToRemove = mutableListOf<String>()

                synchronized(pendingPings) {
                    pendingPings.forEach { (id, sendTime) ->
                        if ((currentTime - sendTime) > PING_TIMEOUT_MS) {
                            pingsToRemove.add(id)
                        }
                    }
                    pingsToRemove.forEach { id ->
                        handlePingTimeout(id)
                    }
                }
            }
            Log.d(TAG, "PING timeout checker job ended.")
        }
    }

    private fun handlePingTimeout(packetId: String) {
        synchronized(pendingPings) {
            if (pendingPings.remove(packetId) != null) { // Ensure it was actually pending
                Log.w(TAG, "PING (ID: $packetId) timed out / send failed.")
                _consecutiveSuccessfulHealthChecks.value = 0
                _consecutiveFailedHealthChecks.value = (_consecutiveFailedHealthChecks.value + 1)

                if (_consecutiveFailedHealthChecks.value >= MAX_FAILED_HEALTH_CHECKS &&
                    _connectionStatus.value != ConnectionStatus.CONNECTION_LOST) {
                    Log.e(TAG, "Max failed health checks reached. Status -> CONNECTION_LOST")
                    _connectionStatus.value = ConnectionStatus.CONNECTION_LOST
                    // Optionally, could try to restart all jobs after a delay if connection is lost
                } else if (_connectionStatus.value != ConnectionStatus.CONNECTION_LOST &&
                    _connectionStatus.value != ConnectionStatus.NO_CONFIG) {
                    // If not completely lost, and not NO_CONFIG, it's effectively in a CONNECTING/retrying state
                    if(_connectionStatus.value != ConnectionStatus.CONNECTING) {
                        Log.d(TAG, "Ping timeout, status -> CONNECTING (was ${_connectionStatus.value})")
                        _connectionStatus.value = ConnectionStatus.CONNECTING
                    }
                }
            }
        }
    }


    // Public method to send a macro command (will be used in Phase 3)
    // This will wrap the InputAction into a UdpPacket
    fun sendMacroCommand(inputActionJson: String, macroTitle: String) {
        val config = currentNetworkConfig ?: run {
            Log.w(TAG, "Cannot send macro '$macroTitle', network config missing.")
            _connectionStatus.value = ConnectionStatus.NO_CONFIG
            return
        }
        val socket = udpSocket ?: run {
            Log.e(TAG, "Cannot send macro '$macroTitle', UDP socket is null.")
            _connectionStatus.value = ConnectionStatus.CONNECTION_LOST // Or attempt restart
            if (config.ip != null && config.port != null) restartSocketAndJobs()
            return
        }

        val ip = config.ip ?: return
        val port = config.port ?: return
        val commandPacket = UdpPacket(
            type = UdpPacketType.MACRO_COMMAND,
            payload = inputActionJson
        )
        // In Phase 3, we'll add this packetId to a pendingAcks map

        appScope.launch(Dispatchers.IO) {
            try {
                val jsonData = json.encodeToString(commandPacket)
                val dataBytes = jsonData.toByteArray(Charsets.UTF_8)
                val datagramPacket = DatagramPacket(
                    /* buf = */ dataBytes,
                    /* length = */ dataBytes.size,
                    /* address = */ InetAddress.getByName(ip),
                    /* port = */ port
                )
                socket.send(datagramPacket)
                Log.i(TAG, "Sent MACRO_COMMAND (ID: ${commandPacket.packetId}, Title: $macroTitle) to ${config.ip}:${config.port}")
                // Update status to SENDING_PENDING_ACK in Phase 3
            } catch (e: Exception) {
                Log.e(TAG, "Error sending MACRO_COMMAND (ID: ${commandPacket.packetId}, Title: $macroTitle): ${e.message}", e)
                // Handle send failure, potentially mark as lost packet in Phase 3
                _connectionStatus.value = ConnectionStatus.CONNECTION_LOST
            }
        }
    }


    fun getCurrentConnectionStatus(): ConnectionStatus = _connectionStatus.value

    override fun toString(): String { // For easier logging from other ViewModels
        return "ConnectionManager(status=${_connectionStatus.value}, config=$currentNetworkConfig)"
    }

    // Called when the ViewModel holding this manager is cleared.
    fun onCleared() {
        Log.d(TAG, "ConnectionManager cleared. Stopping socket and jobs.")
        stopSocketAndJobs()
    }
}
