package com.ongxeno.android.starbuttonbox.datasource

import android.content.Context
import android.util.Log
import com.ongxeno.android.starbuttonbox.data.AutoDragLoopPayload
import com.ongxeno.android.starbuttonbox.data.CaptureMousePayload
import com.ongxeno.android.starbuttonbox.data.ConnectionStatus
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import com.ongxeno.android.starbuttonbox.data.TriggerImportPayload
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
import kotlin.math.roundToLong

private const val DEFAULT_HEALTH_CHECK_INTERVAL_MS = 10000L // Default interval
private const val FAST_HEALTH_CHECK_INTERVAL_MS = 2000L    // Faster interval for reconnecting states
private const val PING_TIMEOUT_MS = 2000L         // Timeout for waiting for PONG
private const val MACRO_ACK_TIMEOUT_MS = 2000L      // Timeout for waiting for MACRO_ACK
private const val MAX_FAILED_HEALTH_CHECKS = 3    // Threshold to declare connection lost
private const val MIN_SUCCESSFUL_HEALTH_CHECKS_FOR_CONNECTED = 2 // Threshold to declare connected
private const val UDP_RECEIVE_BUFFER_SIZE = 2048  // Buffer size for incoming packets
private const val RESPONSE_TIME_WINDOW_SIZE = 5 // Number of latency samples to average

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

    private val _latestResponseTimeMs = MutableStateFlow<Long?>(null)
    val latestResponseTimeMs: StateFlow<Long?> = _latestResponseTimeMs.asStateFlow()

    private val recentLatencyValues = mutableListOf<Long>()
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
                        resetResponseTimeTracking()
                        restartSocketAndJobs()
                    } else if (_connectionStatus.value == ConnectionStatus.NO_CONFIG || _connectionStatus.value == ConnectionStatus.CONNECTION_LOST) {
                        Log.i(TAG, "Valid network config present, attempting to connect/reconnect. Status -> CONNECTING")
                        _connectionStatus.value = ConnectionStatus.CONNECTING
                        resetResponseTimeTracking()
                    }
                } else {
                    Log.w(TAG, "Invalid or missing network config. Setting status to NO_CONFIG.")
                    _connectionStatus.value = ConnectionStatus.NO_CONFIG
                    resetResponseTimeTracking()
                    stopSocketAndJobs()
                }
            }
        }
    }

    @Synchronized
    private fun updateAverageResponseTime(newLatency: Long) {
        if (newLatency < 0) {
            Log.w(TAG, "Attempted to add negative latency ($newLatency ms) to average. Ignoring.")
            return
        }
        recentLatencyValues.add(newLatency)
        while (recentLatencyValues.size > RESPONSE_TIME_WINDOW_SIZE) {
            recentLatencyValues.removeAt(0)
        }
        if (recentLatencyValues.isNotEmpty()) {
            val average = recentLatencyValues.average().roundToLong()
            _latestResponseTimeMs.value = average
            Log.v(TAG,"Updated average latency: $average ms (Window: $recentLatencyValues)")
        } else {
            _latestResponseTimeMs.value = null
        }
    }

    @Synchronized
    private fun resetResponseTimeTracking() {
        if (recentLatencyValues.isNotEmpty() || _latestResponseTimeMs.value != null) {
            recentLatencyValues.clear()
            _latestResponseTimeMs.value = null
            Log.d(TAG, "Response time tracking reset.")
        }
    }

    private fun restartSocketAndJobs() {
        Log.d(TAG, "Restarting socket and all jobs...")
        stopSocketAndJobs()
        val config = currentNetworkConfig
        if (config?.ip == null || config.port == null) {
            Log.w(TAG, "Cannot restart socket and jobs, network config is invalid.")
            _connectionStatus.value = ConnectionStatus.NO_CONFIG
            _latestResponseTimeMs.value = null
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
            resetResponseTimeTracking()
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
        resetResponseTimeTracking()
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
        val clientReceiveTime = System.currentTimeMillis()
        Log.d(TAG, "Processing packet: ID=${packet.packetId}, Type=${packet.type}")
        when (packet.type) {
            UdpPacketType.HEALTH_CHECK_PONG -> {
                val pingSendTime = pendingPings.remove(packet.packetId)
                if (pingSendTime != null) {
                    val rtt = clientReceiveTime - pingSendTime
                    val oneWayLatency = (rtt.toDouble() / 2.0).roundToLong()
                    if (oneWayLatency >= 0) {
                        updateAverageResponseTime(oneWayLatency)
                        Log.i(TAG, "HEALTH_CHECK_PONG received for ID: ${packet.packetId}. Latency: $oneWayLatency ms (PONG_ts: ${packet.timestamp}, PING_ts: $pingSendTime)")
                    } else {
                        Log.w(TAG, "Calculated negative latency for PONG ID ${packet.packetId} ($oneWayLatency ms). PONG_ts: ${packet.timestamp}, PING_ts: $pingSendTime. Not updating response time.")
                    }
                    _lastSuccessfulHealthCheckTime.value = clientReceiveTime
                    _consecutiveFailedHealthChecks.value = 0
                    _consecutiveSuccessfulHealthChecks.value = (_consecutiveSuccessfulHealthChecks.value + 1).coerceAtMost(MIN_SUCCESSFUL_HEALTH_CHECKS_FOR_CONNECTED + 1)
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
                val commandSendTime = pendingMacroAcks.remove(packet.packetId)
                if (commandSendTime != null) {
                    val rtt = clientReceiveTime - commandSendTime
                    val oneWayLatency = (rtt.toDouble() / 2.0).roundToLong()
                    if (oneWayLatency >= 0) {
                        updateAverageResponseTime(oneWayLatency)
                        Log.i(TAG, "MACRO_ACK received for ID: ${packet.packetId}. Latency (ACK): $oneWayLatency ms (ACK_ts: ${packet.timestamp}, CMD_ts: $commandSendTime)")
                    } else {
                        Log.w(TAG, "Calculated negative latency for ACK ID ${packet.packetId} ($oneWayLatency ms). ACK_ts: ${packet.timestamp}, CMD_ts: $commandSendTime. Not updating response time.")
                    }
                    _lastSuccessfulHealthCheckTime.value = System.currentTimeMillis()
                    _consecutiveFailedHealthChecks.value = 0
                    _consecutiveSuccessfulHealthChecks.value = (_consecutiveSuccessfulHealthChecks.value + 1).coerceAtMost(MIN_SUCCESSFUL_HEALTH_CHECKS_FOR_CONNECTED + 1)
                    if (pendingMacroAcks.isEmpty()) {
                        if (_connectionStatus.value == ConnectionStatus.SENDING_PENDING_ACK || _connectionStatus.value == ConnectionStatus.CONNECTING) {
                            Log.i(TAG, "All MACRO_ACKs received or single ACK restored connection. Status -> CONNECTED")
                            _connectionStatus.value = ConnectionStatus.CONNECTED
                        }
                    } else {
                        if (_connectionStatus.value == ConnectionStatus.CONNECTING) {
                            _connectionStatus.value = ConnectionStatus.SENDING_PENDING_ACK
                        }
                        Log.d(TAG, "${pendingMacroAcks.size} MACRO_ACKs still pending.")
                    }
                } else {
                    Log.w(TAG, "Received ACK for unknown or timed-out MACRO_COMMAND ID: ${packet.packetId}")
                }
            }
            // Client should not typically receive these from the server, but log if it does.
            UdpPacketType.HEALTH_CHECK_PING,
            UdpPacketType.MACRO_COMMAND,
            UdpPacketType.TRIGGER_IMPORT_BROWSER,
            UdpPacketType.CAPTURE_MOUSE_POSITION,
            UdpPacketType.AUTO_DRAG_LOOP_COMMAND -> {
                 Log.d(TAG, "Received unexpected packet type ${packet.type} from server (ID: ${packet.packetId}). Ignoring.")
            }
        }
    }

    private fun startHealthChecks() {
        if (healthCheckJob?.isActive == true) return
        Log.d(TAG, "Starting health check job with dynamic interval.")
        healthCheckJob = appScope.launch(Dispatchers.IO) {
            while (isActive) {
                val currentStatus = _connectionStatus.value
                val delayInterval = when (currentStatus) {
                    ConnectionStatus.CONNECTING,
                    ConnectionStatus.CONNECTION_LOST -> FAST_HEALTH_CHECK_INTERVAL_MS
                    else -> DEFAULT_HEALTH_CHECK_INTERVAL_MS
                }
                Log.v(TAG, "Health check delay interval: $delayInterval ms (Status: $currentStatus)")
                delay(delayInterval)
                val config = currentNetworkConfig
                if (config?.ip != null && config.port != null && udpSocket != null && !udpSocket!!.isClosed) {
                    sendHealthCheckPing()
                } else if (udpSocket == null || udpSocket!!.isClosed) {
                    Log.w(TAG, "Health check: UDP socket is null or closed. Attempting to restart connection components.")
                    if (config?.ip != null && config.port != null){
                        restartSocketAndJobs()
                    } else {
                         Log.w(TAG, "Health check: Cannot restart, config is invalid.")
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
        config.port ?: return
        try {
            val jsonData = json.encodeToString(pingPacket)
            val dataBytes = jsonData.toByteArray(Charsets.UTF_8)
            val datagramPacket = DatagramPacket(dataBytes, dataBytes.size, InetAddress.getByName(config.ip), config.port)
            socket.send(datagramPacket)
            pendingPings[packetId] = sendTime
            Log.i(TAG, "Sent HEALTH_CHECK_PING (ID: $packetId) to ${config.ip}:${config.port} at $sendTime")
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
            resetResponseTimeTracking()
            _consecutiveSuccessfulHealthChecks.value = 0
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
            resetResponseTimeTracking()
            _consecutiveFailedHealthChecks.value = MAX_FAILED_HEALTH_CHECKS
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
            payload = inputActionJson,
            timestamp = System.currentTimeMillis()
        )
        val packetId = commandPacket.packetId
        val sendTime = commandPacket.timestamp
        config.port ?: return
        appScope.launch(Dispatchers.IO) {
            try {
                val jsonData = json.encodeToString(commandPacket)
                val dataBytes = jsonData.toByteArray(Charsets.UTF_8)
                val datagramPacket = DatagramPacket(
                    dataBytes, dataBytes.size,
                    InetAddress.getByName(config.ip), config.port
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

    fun sendTriggerImportBrowser(url: String): Boolean {
        val config = currentNetworkConfig ?: run {
            Log.w(TAG, "Cannot send TRIGGER_IMPORT_BROWSER, network config missing.")
            _connectionStatus.value = ConnectionStatus.NO_CONFIG
            return false
        }
        val socket = udpSocket ?: run {
            Log.e(TAG, "Cannot send TRIGGER_IMPORT_BROWSER, UDP socket is null.")
            _connectionStatus.value = ConnectionStatus.CONNECTION_LOST
            if (config.ip != null && config.port != null) restartSocketAndJobs()
            return false
        }
        val payload = TriggerImportPayload(url = url)
        val payloadJson: String = try {
             json.encodeToString(payload)
        } catch (e: Exception) {
             Log.e(TAG, "Error serializing TriggerImportPayload", e)
             return false
        }
        val triggerPacket = UdpPacket(
            type = UdpPacketType.TRIGGER_IMPORT_BROWSER,
            payload = payloadJson,
            timestamp = System.currentTimeMillis()
        )
        val packetId = triggerPacket.packetId
        config.port ?: return false
        appScope.launch(Dispatchers.IO) {
            try {
                val jsonData = json.encodeToString(triggerPacket)
                val dataBytes = jsonData.toByteArray(Charsets.UTF_8)
                val datagramPacket = DatagramPacket(
                    dataBytes, dataBytes.size,
                    InetAddress.getByName(config.ip), config.port
                )
                socket.send(datagramPacket)
                Log.i(TAG, "Sent TRIGGER_IMPORT_BROWSER (ID: $packetId, URL: $url) to ${config.ip}:${config.port}")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending TRIGGER_IMPORT_BROWSER (ID: $packetId): ${e.message}", e)
            }
        }
        return true
    }

    /**
     * Sends a command to the server to capture the current mouse position.
     *
     * @param purpose A string indicating if this is for "SRC" (source) or "DES" (destination).
     * @return True if the command was successfully queued for sending, false otherwise.
     */
    fun sendCaptureMousePositionCommand(purpose: String): Boolean {
        val config = currentNetworkConfig ?: run {
            Log.w(TAG, "Cannot send CAPTURE_MOUSE_POSITION, network config missing.")
            _connectionStatus.value = ConnectionStatus.NO_CONFIG
            return false
        }
        val socket = udpSocket ?: run {
            Log.e(TAG, "Cannot send CAPTURE_MOUSE_POSITION, UDP socket is null.")
            _connectionStatus.value = ConnectionStatus.CONNECTION_LOST
            if (config.ip != null && config.port != null) restartSocketAndJobs()
            return false
        }
         if (_connectionStatus.value == ConnectionStatus.NO_CONFIG || _connectionStatus.value == ConnectionStatus.CONNECTION_LOST) {
            Log.w(TAG, "Cannot send CAPTURE_MOUSE_POSITION, connection status is ${_connectionStatus.value}")
            return false
        }

        val payload = CaptureMousePayload(purpose = purpose)
        val payloadJson: String = try {
            json.encodeToString(payload)
        } catch (e: Exception) {
            Log.e(TAG, "Error serializing CaptureMousePayload for purpose '$purpose'", e)
            return false
        }

        val capturePacket = UdpPacket(
            type = UdpPacketType.CAPTURE_MOUSE_POSITION,
            payload = payloadJson,
            timestamp = System.currentTimeMillis()
        )
        val packetId = capturePacket.packetId
        config.port ?: return false // Should be caught by earlier check

        appScope.launch(Dispatchers.IO) {
            try {
                val jsonData = json.encodeToString(capturePacket)
                val dataBytes = jsonData.toByteArray(Charsets.UTF_8)
                val datagramPacket = DatagramPacket(
                    dataBytes, dataBytes.size,
                    InetAddress.getByName(config.ip), config.port
                )
                socket.send(datagramPacket)
                // No ACK expected for this type, so not adding to pendingMacroAcks
                Log.i(TAG, "Sent CAPTURE_MOUSE_POSITION (ID: $packetId, Purpose: $purpose) to ${config.ip}:${config.port}")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending CAPTURE_MOUSE_POSITION (ID: $packetId, Purpose: $purpose): ${e.message}", e)
                // Optionally handle send failure
            }
        }
        return true
    }

    /**
     * Sends a command to the server to start or stop the auto drag-and-drop loop.
     *
     * @param action A string indicating the action, "START" or "STOP".
     * @return True if the command was successfully queued for sending, false otherwise.
     */
    fun sendAutoDragLoopCommand(action: String): Boolean {
        val config = currentNetworkConfig ?: run {
            Log.w(TAG, "Cannot send AUTO_DRAG_LOOP_COMMAND, network config missing.")
            _connectionStatus.value = ConnectionStatus.NO_CONFIG
            return false
        }
        val socket = udpSocket ?: run {
            Log.e(TAG, "Cannot send AUTO_DRAG_LOOP_COMMAND, UDP socket is null.")
            _connectionStatus.value = ConnectionStatus.CONNECTION_LOST
            if (config.ip != null && config.port != null) restartSocketAndJobs()
            return false
        }
        if (_connectionStatus.value == ConnectionStatus.NO_CONFIG || _connectionStatus.value == ConnectionStatus.CONNECTION_LOST) {
            Log.w(TAG, "Cannot send AUTO_DRAG_LOOP_COMMAND, connection status is ${_connectionStatus.value}")
            return false
        }

        val payload = AutoDragLoopPayload(action = action)
        val payloadJson: String = try {
            json.encodeToString(payload)
        } catch (e: Exception) {
            Log.e(TAG, "Error serializing AutoDragLoopPayload for action '$action'", e)
            return false
        }

        val loopCommandPacket = UdpPacket(
            type = UdpPacketType.AUTO_DRAG_LOOP_COMMAND,
            payload = payloadJson,
            timestamp = System.currentTimeMillis()
        )
        val packetId = loopCommandPacket.packetId
        config.port ?: return false

        appScope.launch(Dispatchers.IO) {
            try {
                val jsonData = json.encodeToString(loopCommandPacket)
                val dataBytes = jsonData.toByteArray(Charsets.UTF_8)
                val datagramPacket = DatagramPacket(
                    dataBytes, dataBytes.size,
                    InetAddress.getByName(config.ip), config.port
                )
                socket.send(datagramPacket)
                // No ACK expected for this type
                Log.i(TAG, "Sent AUTO_DRAG_LOOP_COMMAND (ID: $packetId, Action: $action) to ${config.ip}:${config.port}")
            } catch (e: Exception) {
                Log.e(TAG, "Error sending AUTO_DRAG_LOOP_COMMAND (ID: $packetId, Action: $action): ${e.message}", e)
            }
        }
        return true
    }


    fun getCurrentConnectionStatus(): ConnectionStatus = _connectionStatus.value

    override fun toString(): String {
        return "ConnectionManager(status=${_connectionStatus.value}, config=$currentNetworkConfig, pP=${pendingPings.size}, pA=${pendingMacroAcks.size}, RTT/2=${_latestResponseTimeMs.value}ms)"
    }

    fun onCleared() {
        Log.d(TAG, "ConnectionManager cleared. Stopping socket and jobs.")
        stopSocketAndJobs()
    }
}
