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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private const val HEALTH_CHECK_INTERVAL_MS = 5000L
private const val PING_TIMEOUT_MS = 2000L
private const val MACRO_ACK_TIMEOUT_MS = 2000L // Timeout for macro acknowledgements
private const val MAX_FAILED_HEALTH_CHECKS = 3
private const val MIN_SUCCESSFUL_HEALTH_CHECKS_FOR_CONNECTED = 2 // Still used for PONG-based connection
private const val UDP_RECEIVE_BUFFER_SIZE = 2048

@Singleton
class ConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingDatasource: SettingDatasource,
    private val json: Json,
    @ApplicationScope private val appScope: CoroutineScope
) {
    private val TAG = "ConnectionManager"

    private var udpSocket: DatagramSocket? = null
    private var listenerJob: Job? = null
    private var healthCheckJob: Job? = null
    private var pingTimeoutJob: Job? = null
    private var ackTimeoutJob: Job? = null

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.NO_CONFIG)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private var currentNetworkConfig: NetworkConfig? = null

    private val _lastSuccessfulHealthCheckTime = MutableStateFlow<Long?>(null)
    private val _consecutiveSuccessfulHealthChecks = MutableStateFlow(0)
    private val _consecutiveFailedHealthChecks = MutableStateFlow(0)

    private val pendingPings = ConcurrentHashMap<String, Long>()
    private val pendingMacroAcks = ConcurrentHashMap<String, Long>()

    init {
        Log.d(TAG, "Initializing ConnectionManager.")
        appScope.launch {
            settingDatasource.networkConfigFlow.collectLatest { config ->
                Log.d(TAG, "Network config updated: $config")
                val oldConfig = currentNetworkConfig
                currentNetworkConfig = config

                if (config?.ip != null && config.port != null) {
                    if (oldConfig?.ip != config.ip || oldConfig?.port != config.port || udpSocket == null || udpSocket!!.isClosed) {
                        Log.i(TAG, "Network config changed or socket not ready. Restarting connection components.")
                        _connectionStatus.value = ConnectionStatus.CONNECTING
                        restartSocketAndJobs()
                    } else if (_connectionStatus.value == ConnectionStatus.NO_CONFIG || _connectionStatus.value == ConnectionStatus.CONNECTION_LOST) {
                        Log.i(TAG, "Valid network config present, attempting to connect/reconnect. Status -> CONNECTING")
                        _connectionStatus.value = ConnectionStatus.CONNECTING
                    }
                } else {
                    Log.w(TAG, "Invalid or missing network config. Setting status to NO_CONFIG.")
                    _connectionStatus.value = ConnectionStatus.NO_CONFIG
                    stopSocketAndJobs()
                }
            }
        }
    }

    private fun restartSocketAndJobs() {
        Log.d(TAG, "Restarting socket and all jobs...")
        stopSocketAndJobs()

        val config = currentNetworkConfig
        if (config?.ip == null || config.port == null) {
            Log.w(TAG, "Cannot restart socket and jobs, network config is invalid.")
            _connectionStatus.value = ConnectionStatus.NO_CONFIG
            return
        }

        try {
            udpSocket = DatagramSocket()
            Log.i(TAG, "UDP Socket created and bound to local port: ${udpSocket?.localPort}")

            startListener()
            startHealthChecks()
            startPingTimeoutChecker()
            startAckTimeoutChecker()

            if (_connectionStatus.value != ConnectionStatus.CONNECTING && _connectionStatus.value != ConnectionStatus.SENDING_PENDING_ACK) {
                _connectionStatus.value = ConnectionStatus.CONNECTING
            }
        } catch (e: SocketException) {
            Log.e(TAG, "Error creating UDP socket: ${e.message}", e)
            _connectionStatus.value = ConnectionStatus.CONNECTION_LOST
            stopSocketAndJobs()
        }
    }

    private fun stopSocketAndJobs() {
        Log.d(TAG, "Stopping socket and all jobs...")
        listenerJob?.cancel()
        healthCheckJob?.cancel()
        pingTimeoutJob?.cancel()
        ackTimeoutJob?.cancel()
        listenerJob = null
        healthCheckJob = null
        pingTimeoutJob = null
        ackTimeoutJob = null

        pendingPings.clear()
        pendingMacroAcks.clear()
        _consecutiveFailedHealthChecks.value = 0
        _consecutiveSuccessfulHealthChecks.value = 0
        _lastSuccessfulHealthCheckTime.value = null

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
        if (listenerJob?.isActive == true) return
        Log.d(TAG, "Starting UDP listener job.")
        listenerJob = appScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(UDP_RECEIVE_BUFFER_SIZE)
            while (isActive) {
                try {
                    val datagramPacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(datagramPacket)
                    val jsonData = String(datagramPacket.data, 0, datagramPacket.length)
                    Log.v(TAG, "Received UDP packet: $jsonData from ${datagramPacket.address.hostAddress}:${datagramPacket.port}")
                    try {
                        val receivedPacket = json.decodeFromString<UdpPacket>(jsonData)
                        processReceivedPacket(receivedPacket)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing received UDP packet: $jsonData", e)
                    }
                } catch (e: SocketTimeoutException) {
                    Log.v(TAG, "Socket receive timeout.")
                } catch (e: SocketException) {
                    if (isActive) Log.e(TAG, "SocketException in listener: ${e.message}")
                    break
                } catch (e: Exception) {
                    if (isActive) Log.e(TAG, "Error in UDP listener: ${e.message}", e)
                }
            }
            Log.d(TAG, "UDP listener job ended.")
        }
    }

    private fun processReceivedPacket(packet: UdpPacket) {
        Log.d(TAG, "Processing packet: ID=${packet.packetId}, Type=${packet.type}")
        when (packet.type) {
            UdpPacketType.HEALTH_CHECK_PONG -> {
                if (pendingPings.remove(packet.packetId) != null) {
                    Log.i(TAG, "HEALTH_CHECK_PONG received for ID: ${packet.packetId}")
                    _lastSuccessfulHealthCheckTime.value = System.currentTimeMillis()
                    _consecutiveFailedHealthChecks.value = 0
                    _consecutiveSuccessfulHealthChecks.value = (_consecutiveSuccessfulHealthChecks.value + 1).coerceAtMost(MIN_SUCCESSFUL_HEALTH_CHECKS_FOR_CONNECTED + 1)

                    // Transition to CONNECTED if enough PONGs received and not currently sending a macro
                    if (_consecutiveSuccessfulHealthChecks.value >= MIN_SUCCESSFUL_HEALTH_CHECKS_FOR_CONNECTED &&
                        _connectionStatus.value != ConnectionStatus.CONNECTED &&
                        _connectionStatus.value != ConnectionStatus.SENDING_PENDING_ACK) {
                        Log.i(TAG, "Connection established (PONG received). Status -> CONNECTED")
                        _connectionStatus.value = ConnectionStatus.CONNECTED
                    }
                } else {
                    Log.w(TAG, "Received PONG for unknown or timed-out PING ID: ${packet.packetId}")
                }
            }
            UdpPacketType.MACRO_ACK -> {
                if (pendingMacroAcks.remove(packet.packetId) != null) {
                    Log.i(TAG, "MACRO_ACK received for ID: ${packet.packetId}")
                    // A successful ACK implies the server is responsive.
                    _lastSuccessfulHealthCheckTime.value = System.currentTimeMillis()
                    _consecutiveFailedHealthChecks.value = 0
                    // Consider an ACK as a strong sign of health, can contribute to successful checks count
                    _consecutiveSuccessfulHealthChecks.value = (_consecutiveSuccessfulHealthChecks.value + 1).coerceAtMost(MIN_SUCCESSFUL_HEALTH_CHECKS_FOR_CONNECTED + 1)


                    if (pendingMacroAcks.isEmpty()) {
                        // If all ACKs are received, and we were either sending or trying to connect,
                        // consider the connection restored.
                        if (_connectionStatus.value == ConnectionStatus.SENDING_PENDING_ACK || _connectionStatus.value == ConnectionStatus.CONNECTING) {
                            Log.i(TAG, "All MACRO_ACKs received or single ACK restored connection. Status -> CONNECTED")
                            _connectionStatus.value = ConnectionStatus.CONNECTED
                        }
                    } else {
                        // Still more ACKs pending, remain in (or switch to) SENDING_PENDING_ACK if we were connecting
                        if (_connectionStatus.value == ConnectionStatus.CONNECTING) {
                            _connectionStatus.value = ConnectionStatus.SENDING_PENDING_ACK
                        }
                        Log.d(TAG, "${pendingMacroAcks.size} MACRO_ACKs still pending.")
                    }
                } else {
                    Log.w(TAG, "Received ACK for unknown or timed-out MACRO_COMMAND ID: ${packet.packetId}")
                }
            }
            else -> Log.w(TAG, "Received unhandled packet type: ${packet.type}")
        }
    }

    private fun startHealthChecks() {
        if (healthCheckJob?.isActive == true) return
        Log.d(TAG, "Starting health check job.")
        healthCheckJob = appScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(HEALTH_CHECK_INTERVAL_MS)
                val config = currentNetworkConfig
                if (config?.ip != null && config.port != null && udpSocket != null && !udpSocket!!.isClosed) {
                    sendHealthCheckPing()
                } else if (udpSocket == null || udpSocket!!.isClosed) {
                    Log.w(TAG, "Health check: UDP socket is null or closed. Attempting to restart connection components.")
                    if (config?.ip != null && config.port != null){
                        restartSocketAndJobs()
                    }
                }
            }
            Log.d(TAG, "Health check job ended.")
        }
    }

    private fun sendHealthCheckPing() {
        val config = currentNetworkConfig ?: return
        val socket = udpSocket ?: return

        val pingPacket = UdpPacket(type = UdpPacketType.HEALTH_CHECK_PING)
        val packetId = pingPacket.packetId
        val sendTime = pingPacket.timestamp
        val ip = config.ip ?: return
        val port = config.port ?: return

        try {
            val jsonData = json.encodeToString(pingPacket)
            val dataBytes = jsonData.toByteArray(Charsets.UTF_8)
            val datagramPacket = DatagramPacket(dataBytes, dataBytes.size, InetAddress.getByName(ip), port)
            socket.send(datagramPacket)
            pendingPings[packetId] = sendTime
            Log.i(TAG, "Sent HEALTH_CHECK_PING (ID: $packetId) to ${config.ip}:${config.port}")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending HEALTH_CHECK_PING (ID: $packetId): ${e.message}", e)
            handlePingTimeout(packetId, isSendFailure = true)
        }
    }

    private fun startPingTimeoutChecker() {
        if (pingTimeoutJob?.isActive == true) return
        Log.d(TAG, "Starting PING timeout checker job.")
        pingTimeoutJob = appScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(PING_TIMEOUT_MS / 2)
                val currentTime = System.currentTimeMillis()
                val pingsToRemove = mutableListOf<String>()
                pendingPings.forEach { (id, sendTime) ->
                    if ((currentTime - sendTime) > PING_TIMEOUT_MS) {
                        pingsToRemove.add(id)
                    }
                }
                pingsToRemove.forEach { id -> handlePingTimeout(id) }
            }
            Log.d(TAG, "PING timeout checker job ended.")
        }
    }

    private fun handlePingTimeout(packetId: String, isSendFailure: Boolean = false) {
        if (pendingPings.remove(packetId) != null || isSendFailure) {
            Log.w(TAG, "PING (ID: $packetId) timed out or send failed.")
            _consecutiveSuccessfulHealthChecks.value = 0 // Reset success counter on any PING failure/timeout
            _consecutiveFailedHealthChecks.value = (_consecutiveFailedHealthChecks.value + 1)

            if (_consecutiveFailedHealthChecks.value >= MAX_FAILED_HEALTH_CHECKS &&
                _connectionStatus.value != ConnectionStatus.CONNECTION_LOST) {
                Log.e(TAG, "Max failed health checks reached. Status -> CONNECTION_LOST")
                _connectionStatus.value = ConnectionStatus.CONNECTION_LOST
            } else if (_connectionStatus.value != ConnectionStatus.CONNECTION_LOST &&
                _connectionStatus.value != ConnectionStatus.NO_CONFIG) {
                if (_connectionStatus.value != ConnectionStatus.CONNECTING) {
                    Log.d(TAG, "Ping timeout/failure, status -> CONNECTING (was ${_connectionStatus.value})")
                    _connectionStatus.value = ConnectionStatus.CONNECTING
                }
            }
        }
    }

    private fun startAckTimeoutChecker() {
        if (ackTimeoutJob?.isActive == true) return
        Log.d(TAG, "Starting MACRO_ACK timeout checker job.")
        ackTimeoutJob = appScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(MACRO_ACK_TIMEOUT_MS / 2)
                val currentTime = System.currentTimeMillis()
                val acksToRemove = mutableListOf<String>()
                pendingMacroAcks.forEach { (id, sendTime) ->
                    if ((currentTime - sendTime) > MACRO_ACK_TIMEOUT_MS) {
                        acksToRemove.add(id)
                    }
                }
                acksToRemove.forEach { id -> handleMacroAckTimeout(id) }
            }
            Log.d(TAG, "MACRO_ACK timeout checker job ended.")
        }
    }

    private fun handleMacroAckTimeout(packetId: String, isSendFailure: Boolean = false) {
        if (pendingMacroAcks.remove(packetId) != null || isSendFailure) {
            Log.e(TAG, "MACRO_COMMAND (ID: $packetId) ACK timed out or send failed. Status -> CONNECTION_LOST")
            _connectionStatus.value = ConnectionStatus.CONNECTION_LOST
            _consecutiveFailedHealthChecks.value = MAX_FAILED_HEALTH_CHECKS // Treat as critical failure
            _consecutiveSuccessfulHealthChecks.value = 0
        }
    }

    fun sendMacroCommand(inputActionJson: String, macroTitle: String) {
        val config = currentNetworkConfig ?: run {
            Log.w(TAG, "Cannot send macro '$macroTitle', network config missing.")
            _connectionStatus.value = ConnectionStatus.NO_CONFIG
            return
        }
        val socket = udpSocket ?: run {
            Log.e(TAG, "Cannot send macro '$macroTitle', UDP socket is null.")
            _connectionStatus.value = ConnectionStatus.CONNECTION_LOST
            if (config.ip != null && config.port != null) restartSocketAndJobs()
            return
        }

        val commandPacket = UdpPacket(
            type = UdpPacketType.MACRO_COMMAND,
            payload = inputActionJson
        )
        val packetId = commandPacket.packetId
        val sendTime = commandPacket.timestamp
        val ip = config.ip ?: return
        val port = config.port ?: return

        appScope.launch(Dispatchers.IO) {
            try {
                val jsonData = json.encodeToString(commandPacket)
                val dataBytes = jsonData.toByteArray(Charsets.UTF_8)
                val datagramPacket = DatagramPacket(
                    dataBytes, dataBytes.size,
                    InetAddress.getByName(ip), port
                )
                socket.send(datagramPacket)
                pendingMacroAcks[packetId] = sendTime
                Log.i(TAG, "Sent MACRO_COMMAND (ID: $packetId, Title: $macroTitle) to ${config.ip}:${config.port}")

                if (_connectionStatus.value == ConnectionStatus.CONNECTED || _connectionStatus.value == ConnectionStatus.CONNECTING) {
                    _connectionStatus.value = ConnectionStatus.SENDING_PENDING_ACK
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending MACRO_COMMAND (ID: $packetId, Title: $macroTitle): ${e.message}", e)
                handleMacroAckTimeout(packetId, isSendFailure = true)
            }
        }
    }

    fun getCurrentConnectionStatus(): ConnectionStatus = _connectionStatus.value

    override fun toString(): String {
        return "ConnectionManager(status=${_connectionStatus.value}, config=$currentNetworkConfig, pP=${pendingPings.size}, pA=${pendingMacroAcks.size})"
    }

    fun onCleared() {
        Log.d(TAG, "ConnectionManager cleared. Stopping socket and jobs.")
        stopSocketAndJobs()
    }
}
