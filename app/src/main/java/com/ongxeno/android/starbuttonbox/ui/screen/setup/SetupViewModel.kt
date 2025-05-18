package com.ongxeno.android.starbuttonbox.ui.screen.setup // Create a new package for setup screens

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import com.ongxeno.android.starbuttonbox.datasource.AppLocalWebServer // Assuming PcImportWebServer is refactored to this
import com.ongxeno.android.starbuttonbox.datasource.SettingDatasource
import com.ongxeno.android.starbuttonbox.di.ApplicationScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.InetAddress
import javax.inject.Inject

// Defines the different steps in the setup process
enum class SetupStep {
    SERVER_QUERY,        // Ask if server is already installed
    SERVER_INSTRUCTIONS, // Show instructions to install server on PC
    NETWORK_CONFIG,      // Configure network connection to the PC server
    SETUP_COMPLETE       // Intermediate state to signal completion before navigation
}

// Defines the discovery state for the network configuration step
sealed class SetupDiscoveryState {
    data object Idle : SetupDiscoveryState()
    data object Searching : SetupDiscoveryState()
    data class Discovered(val servers: List<NetworkConfig>) : SetupDiscoveryState()
    data class Error(val message: String) : SetupDiscoveryState()
}


@HiltViewModel
class SetupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope,
    private val settingDatasource: SettingDatasource,
    private val appLocalWebServer: AppLocalWebServer // Renamed from PcImportWebServer
) : ViewModel() {

    private val _tag = "SetupViewModel"
    private val NSD_SERVICE_TYPE = "_starbuttonbox._udp." // Same as in SettingViewModel

    private val _currentStep = MutableStateFlow(SetupStep.SERVER_QUERY)
    val currentStep: StateFlow<SetupStep> = _currentStep.asStateFlow()

    private val _ktorServerUrl = MutableStateFlow<String?>(null)
    val ktorServerUrl: StateFlow<String?> = _ktorServerUrl.asStateFlow()

    private val _isKtorServerRunning = MutableStateFlow(false)
    val isKtorServerRunning: StateFlow<Boolean> = _isKtorServerRunning.asStateFlow()

    // --- NSD States for Network Config Step ---
    private val _discoveryState = MutableStateFlow<SetupDiscoveryState>(SetupDiscoveryState.Idle)
    val discoveryState: StateFlow<SetupDiscoveryState> = _discoveryState.asStateFlow()

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isNsdDiscoveryActive = false
    private val discoveredServicesMap = mutableMapOf<String, NsdServiceInfo>() // Using map for better management
    private val resolvedServersList = MutableStateFlow<List<NetworkConfig>>(emptyList())

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null
    private var nsdResolveJobs = mutableMapOf<String, Job>()


    init {
        Log.d(_tag, "ViewModel initialized. Current step: ${_currentStep.value}")
        // Observe resolved servers to update discovery state
        viewModelScope.launch {
            resolvedServersList.collect { servers ->
                if (isNsdDiscoveryActive) {
                    _discoveryState.value = SetupDiscoveryState.Discovered(servers)
                } else if (_discoveryState.value !is SetupDiscoveryState.Error) {
                    _discoveryState.value = if (servers.isNotEmpty()) SetupDiscoveryState.Discovered(servers) else SetupDiscoveryState.Idle
                }
            }
        }
    }

    fun answerServerQuery(isServerAlreadySetup: Boolean) {
        Log.d(_tag, "answerServerQuery: isServerAlreadySetup = $isServerAlreadySetup")
        if (isServerAlreadySetup) {
            _currentStep.value = SetupStep.NETWORK_CONFIG
            // Start discovery when moving to network config step
            startNsdDiscovery()
        } else {
            _currentStep.value = SetupStep.SERVER_INSTRUCTIONS
            startKtorServerForSetup()
        }
    }

    private fun startKtorServerForSetup() {
        if (_isKtorServerRunning.value) {
            Log.d(_tag, "Ktor server for setup already running at ${_ktorServerUrl.value}")
            return
        }
        viewModelScope.launch {
            _isKtorServerRunning.value = true
            _ktorServerUrl.value = "Starting server..." // Initial status
            // Assuming AppLocalWebServer.startServer now takes a mode
            val url = appLocalWebServer.startServer(AppLocalWebServer.ServerMode.SERVER_SETUP_ASSISTANCE) { /* No JSON expected for this mode */ }
            if (url != null) {
                _ktorServerUrl.value = url
                Log.i(_tag, "Ktor server for PC setup started at: $url")
            } else {
                _ktorServerUrl.value = "Error: Could not start local server."
                _isKtorServerRunning.value = false // Reflect failure
                Log.e(_tag, "Failed to start Ktor server for PC setup.")
            }
        }
    }

    fun proceedToNetworkConfig() {
        Log.d(_tag, "Proceeding to Network Configuration step.")
        viewModelScope.launch {
            if (_isKtorServerRunning.value) {
                appLocalWebServer.stopServer()
                _isKtorServerRunning.value = false
                _ktorServerUrl.value = null
                Log.i(_tag, "Ktor server stopped.")
            }
        }
        _currentStep.value = SetupStep.NETWORK_CONFIG
        // Start discovery when moving to network config step
        startNsdDiscovery()
    }

    fun saveNetworkConfigurationAndFinish(ip: String, port: Int) {
        viewModelScope.launch {
            Log.i(_tag, "Saving network configuration: IP=$ip, Port=$port")
            settingDatasource.saveSettings(ip, port)
            settingDatasource.setFirstLaunchCompleted() // Mark first launch as done
            Log.i(_tag, "First launch marked as complete.")
            _currentStep.value = SetupStep.SETUP_COMPLETE // Signal completion
        }
    }

    // --- NSD Discovery Logic (adapted from SettingViewModel) ---

    fun startNsdDiscovery() {
        viewModelScope.launch {
            if (isNsdDiscoveryActive) {
                Log.d(_tag, "NSD Discovery already active. Restarting for refresh.")
                stopNsdDiscoveryInternal()
                delay(250)
            }
            discoveredServicesMap.clear()
            resolvedServersList.value = emptyList()
            _discoveryState.value = SetupDiscoveryState.Searching
            Log.i(_tag, "Starting NSD service discovery for type: $NSD_SERVICE_TYPE")

            acquireMulticastLock()
            initializeDiscoveryListenerInternal()

            try {
                if (discoveryListener == null) throw IllegalStateException("Discovery listener not initialized.")
                nsdManager.discoverServices(NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
                isNsdDiscoveryActive = true
            } catch (e: Exception) {
                Log.e(_tag, "Error starting NSD discovery", e)
                _discoveryState.value = SetupDiscoveryState.Error("Failed to start discovery: ${e.message}")
                isNsdDiscoveryActive = false
                releaseMulticastLock()
                discoveryListener = null
            }
        }
    }

    private fun stopNsdDiscoveryInternal() {
        val listener = discoveryListener
        if (listener != null) {
            try {
                nsdManager.stopServiceDiscovery(listener)
                Log.d(_tag, "nsdManager.stopServiceDiscovery called.")
            } catch (e: Exception) {
                Log.w(_tag, "Exception stopping NSD discovery (internal): ${e.message}")
            } finally {
                if (discoveryListener == listener) discoveryListener = null
            }
        }
        nsdResolveJobs.values.forEach { it.cancel() } // Cancel any ongoing resolve jobs
        nsdResolveJobs.clear()
    }

    fun stopNsdDiscovery() {
        if (!isNsdDiscoveryActive && discoveryListener == null) return
        Log.i(_tag, "Stopping NSD service discovery.")
        isNsdDiscoveryActive = false
        stopNsdDiscoveryInternal()
        releaseMulticastLock()
        // Keep resolvedServersList as is, update _discoveryState based on it
        if (_discoveryState.value !is SetupDiscoveryState.Error) {
            _discoveryState.value = if (resolvedServersList.value.isNotEmpty()) SetupDiscoveryState.Discovered(resolvedServersList.value) else SetupDiscoveryState.Idle
        }
    }

    private fun acquireMulticastLock() {
        synchronized(this) {
            if (multicastLock == null) {
                multicastLock = wifiManager.createMulticastLock("starbuttonbox_setup_mdns_lock").apply {
                    setReferenceCounted(true)
                }
            }
            if (multicastLock?.isHeld == false) {
                try {
                    multicastLock?.acquire()
                    Log.i(_tag, "MulticastLock acquired for setup.")
                } catch (e: Exception) {
                    Log.e(_tag, "Error acquiring MulticastLock for setup", e)
                    _discoveryState.value = SetupDiscoveryState.Error("Network lock error.")
                }
            }
        }
    }

    private fun releaseMulticastLock() {
        synchronized(this) {
            if (multicastLock?.isHeld == true) {
                try {
                    multicastLock?.release()
                    Log.i(_tag, "MulticastLock released for setup.")
                } catch (e: Exception) {
                    Log.e(_tag, "Error releasing MulticastLock for setup", e)
                }
            }
        }
    }

    private fun initializeDiscoveryListenerInternal() {
        if (discoveryListener != null) return
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(_tag, "NSD Discovery Started: $regType")
                if (isNsdDiscoveryActive && _discoveryState.value !is SetupDiscoveryState.Discovered) {
                    _discoveryState.value = SetupDiscoveryState.Searching
                }
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                val foundType = service.serviceType?.removeSuffix(".")
                val expectedType = NSD_SERVICE_TYPE.removeSuffix(".")
                if (foundType != expectedType) {
                    Log.d(_tag, "Ignoring service: Type mismatch. Found='$foundType', Expected='$expectedType'")
                    return
                }
                Log.i(_tag, "NSD Service Found: ${service.serviceName}")
                synchronized(discoveredServicesMap) {
                    if (!discoveredServicesMap.containsKey(service.serviceName)) {
                        discoveredServicesMap[service.serviceName] = service
                        resolveDiscoveredService(service)
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.w(_tag, "NSD Service Lost: ${service.serviceName}")
                val serviceKey = service.serviceName
                synchronized(discoveredServicesMap) {
                    discoveredServicesMap.remove(serviceKey)
                    nsdResolveJobs[serviceKey]?.cancel() // Cancel ongoing resolve if any
                    nsdResolveJobs.remove(serviceKey)
                }
                val ipToRemove = service.host?.hostAddress
                resolvedServersList.update { current ->
                    if (ipToRemove != null) current.filterNot { it.ip == ipToRemove && it.port == service.port }
                    else current
                }
            }
            override fun onDiscoveryStopped(serviceType: String) { Log.i(_tag, "NSD Discovery Stopped: $serviceType"); isNsdDiscoveryActive = false }
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(_tag, "NSD Start Discovery Failed: $errorCode"); _discoveryState.value = SetupDiscoveryState.Error("Discovery start failed (Code: $errorCode)"); isNsdDiscoveryActive = false; releaseMulticastLock(); discoveryListener = null
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(_tag, "NSD Stop Discovery Failed: $errorCode"); releaseMulticastLock(); discoveryListener = null // Don't necessarily set error state here
            }
        }
    }

    private fun resolveDiscoveredService(serviceInfo: NsdServiceInfo) {
        val serviceKey = serviceInfo.serviceName
        if (nsdResolveJobs[serviceKey]?.isActive == true) {
            Log.d(_tag, "Resolve job already active for $serviceKey")
            return
        }
        nsdResolveJobs[serviceKey] = viewModelScope.launch(Dispatchers.IO) { // Ensure IO dispatcher for network
            Log.d(_tag, "Attempting to resolve service: $serviceKey")
            try {
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(sInfo: NsdServiceInfo, errCode: Int) {
                        Log.e(_tag, "NSD Resolve Failed for ${sInfo.serviceName}: $errCode")
                        synchronized(discoveredServicesMap) { discoveredServicesMap.remove(sInfo.serviceName) }
                        nsdResolveJobs.remove(sInfo.serviceName)
                    }

                    override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                        Log.i(_tag, "NSD Service Resolved: ${resolvedInfo.serviceName}")
                        val host: InetAddress? = resolvedInfo.host
                        val port: Int = resolvedInfo.port
                        val ipAddress: String? = host?.hostAddress

                        if (ipAddress != null && ipAddress != "0.0.0.0" && !ipAddress.startsWith("127.") && port > 0) {
                            val newServer = NetworkConfig(ip = ipAddress, port = port)
                            resolvedServersList.update { current ->
                                if (current.none { it.ip == newServer.ip && it.port == newServer.port }) (current + newServer).distinct()
                                else current
                            }
                        }
                        synchronized(discoveredServicesMap) { discoveredServicesMap.remove(resolvedInfo.serviceName) }
                        nsdResolveJobs.remove(resolvedInfo.serviceName)
                    }
                })
            } catch (e: Exception) {
                Log.e(_tag, "Exception calling nsdManager.resolveService for $serviceKey", e)
                synchronized(discoveredServicesMap) { discoveredServicesMap.remove(serviceKey) }
                nsdResolveJobs.remove(serviceKey)
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        Log.d(_tag, "SetupViewModel cleared.")
        viewModelScope.launch { // Ensure server stop is also on a coroutine
            appLocalWebServer.stopServer()
        }
        stopNsdDiscovery() // This already handles releasing multicast lock
    }
}
