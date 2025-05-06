package com.ongxeno.android.starbuttonbox.ui.screen.setting

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager // Import WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ongxeno.android.starbuttonbox.data.NetworkConfig
import com.ongxeno.android.starbuttonbox.datasource.SettingDatasource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay // Import delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.net.InetAddress
import javax.inject.Inject

// Discovery State remains the same
sealed class DiscoveryState {
    data object Idle : DiscoveryState()
    data object Searching : DiscoveryState()
    data class Discovered(val servers: List<NetworkConfig>) : DiscoveryState()
    data class Error(val message: String) : DiscoveryState()
}

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val settingDatasource: SettingDatasource,
    @ApplicationContext private val context: Context // Inject context
) : ViewModel() {

    private val _tag = "SettingViewModelNSD"
    // Match the service type found by the external discovery app
    // This is the type we EXPECT and primarily search for.
    private val NSD_SERVICE_TYPE = "_starbuttonbox._udp."

    // --- NsdManager and Listeners ---
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private var isDiscoveryActive = false
    private val discoveredServices = mutableMapOf<String, NsdServiceInfo>()
    private val resolvedServers = MutableStateFlow<List<NetworkConfig>>(emptyList())

    // --- Multicast Lock ---
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var multicastLock: WifiManager.MulticastLock? = null

    // --- State for Connection Config Dialog ---
    private val _showConnectionConfigDialog = MutableStateFlow(false)
    val showConnectionConfigDialogState: StateFlow<Boolean> = _showConnectionConfigDialog.asStateFlow()

    // --- Expose necessary settings state for the UI ---
    val networkConfigState: StateFlow<NetworkConfig?> = settingDatasource.networkConfigFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val keepScreenOnState: StateFlow<Boolean> = settingDatasource.keepScreenOnFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- State for Discovery ---
    private val _discoveryState = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    init {
        Log.d(_tag, "ViewModel initialized")
        // Combine resolved servers flow with discovery active status
        viewModelScope.launch {
            resolvedServers.collect { servers ->
                // Update DiscoveryState based on resolved servers and active status
                val currentState = _discoveryState.value
                if (isDiscoveryActive) {
                    // If actively searching or have found servers, show Discovered
                    // Use the latest list of servers
                    _discoveryState.value = DiscoveryState.Discovered(servers)
                } else if (currentState !is DiscoveryState.Error) {
                    // If discovery stopped and no error, reflect current servers or Idle
                    _discoveryState.value = if (servers.isNotEmpty()) DiscoveryState.Discovered(servers) else DiscoveryState.Idle
                }
                // Do not automatically overwrite an Error state here
            }
        }
    }

    // --- Event Handlers ---

    fun saveConnectionSettings(ip: String, port: Int) {
        viewModelScope.launch {
            settingDatasource.saveSettings(ip, port)
            _showConnectionConfigDialog.value = false
            stopDiscovery()
            Log.i(_tag, "Connection settings saved.")
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            settingDatasource.saveKeepScreenOn(enabled)
            Log.d(_tag, "Keep screen on set to: $enabled")
        }
    }

    fun showConnectionConfigDialog() {
        Log.d(_tag, "Showing connection config dialog.")
        _showConnectionConfigDialog.value = true
        startDiscovery()
    }

    fun hideConnectionConfigDialog(contextForToast: Context? = null) {
        viewModelScope.launch {
            val config = networkConfigState.value
            val isConfigValid = config?.ip != null && config.port != null
            val canDismiss = isConfigValid || !_showConnectionConfigDialog.value

            if (canDismiss) {
                Log.d(_tag, "Hiding connection config dialog.")
                _showConnectionConfigDialog.value = false
                stopDiscovery()
            } else {
                Log.w(_tag, "Attempted to hide connection config, but config is invalid.")
                contextForToast?.let {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(it, "Please save connection settings first", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // --- NSD Discovery Functions ---

    fun startDiscovery() {
        // Launch in viewModelScope to handle potential delays
        viewModelScope.launch {
            if (isDiscoveryActive) {
                Log.d(_tag, "Discovery is already active. Restarting for refresh...")
                stopNsdDiscoveryInternal() // Stop existing listener
                delay(250) // Wait briefly
                discoveredServices.clear()
                resolvedServers.value = emptyList()
                _discoveryState.value = DiscoveryState.Searching
                Log.d(_tag, "Restarting discovery after delay.")
            } else {
                Log.i(_tag, "Starting NSD service discovery for type: $NSD_SERVICE_TYPE")
                discoveredServices.clear()
                resolvedServers.value = emptyList()
                _discoveryState.value = DiscoveryState.Searching
            }

            acquireMulticastLock() // Acquire lock

            initializeDiscoveryListener() // Ensure listener is ready

            try {
                if (discoveryListener == null) {
                    throw IllegalStateException("Failed to initialize discovery listener.")
                }
                Log.d(_tag, "Calling nsdManager.discoverServices with type '$NSD_SERVICE_TYPE'")
                nsdManager.discoverServices(NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
                isDiscoveryActive = true
                Log.d(_tag, "nsdManager.discoverServices call initiated.")
            } catch (e: Exception) {
                Log.e(_tag, "Error starting NSD discovery", e)
                _discoveryState.value = DiscoveryState.Error("Failed to start discovery: ${e.message}")
                isDiscoveryActive = false
                releaseMulticastLock() // Release lock on failure
                discoveryListener = null // Clean up listener
            }
        }
    }

    // Internal function to stop NSD without affecting state immediately
    private fun stopNsdDiscoveryInternal() {
        val listenerToStop = discoveryListener
        if (listenerToStop != null) {
            try {
                Log.d(_tag, "Calling nsdManager.stopServiceDiscovery (internal)...")
                nsdManager.stopServiceDiscovery(listenerToStop)
            } catch (e: Exception) {
                Log.w(_tag, "Exception stopping NSD discovery (internal, may be benign): ${e.message}")
            } finally {
                // Nullify the reference only if it's the one we tried to stop
                if (discoveryListener == listenerToStop) {
                    discoveryListener = null
                    Log.d(_tag, "discoveryListener reference set to null.")
                }
            }
        } else {
            // Log.d(_tag, "stopNsdDiscoveryInternal called but listener was null.")
        }
    }

    // Public function to stop discovery process fully
    fun stopDiscovery() {
        if (!isDiscoveryActive && discoveryListener == null) {
            Log.d(_tag, "Discovery already fully stopped.")
            return
        }
        Log.i(_tag, "Stopping NSD service discovery.")
        isDiscoveryActive = false
        stopNsdDiscoveryInternal()
        releaseMulticastLock()
        discoveredServices.clear()

        if (_discoveryState.value !is DiscoveryState.Error) {
            _discoveryState.value = if (resolvedServers.value.isNotEmpty()) DiscoveryState.Discovered(resolvedServers.value) else DiscoveryState.Idle
        }
        Log.d(_tag, "Discovery stopped. Final state: ${_discoveryState.value}")
    }


    // --- Multicast Lock Management ---
    private fun acquireMulticastLock() {
        synchronized(this) {
            if (multicastLock == null) {
                multicastLock = wifiManager.createMulticastLock("starbuttonbox_mdns_lock").apply {
                    setReferenceCounted(true)
                }
            }
            if (multicastLock?.isHeld == false) {
                try {
                    multicastLock?.acquire()
                    Log.i(_tag, "MulticastLock acquired. isHeld=${multicastLock?.isHeld}")
                } catch (e: SecurityException) {
                    Log.e(_tag, "SecurityException acquiring MulticastLock. Check CHANGE_WIFI_MULTICAST_STATE permission.", e)
                    _discoveryState.value = DiscoveryState.Error("Permission missing for discovery.")
                } catch (e: Exception) {
                    Log.e(_tag, "Exception acquiring MulticastLock", e)
                    _discoveryState.value = DiscoveryState.Error("Failed to acquire network lock.")
                }
            } else {
                // Log.d(_tag, "MulticastLock already held or null.")
            }
        }
    }

    private fun releaseMulticastLock() {
        synchronized(this) {
            if (multicastLock?.isHeld == true) {
                try {
                    multicastLock?.release()
                    Log.i(_tag, "MulticastLock released. isHeld=${multicastLock?.isHeld}")
                    if (multicastLock?.isHeld == false) {
                        Log.d(_tag, "MulticastLock reference count reached zero.")
                    }
                } catch (e: Exception) {
                    Log.e(_tag, "Exception releasing MulticastLock", e)
                }
            } else {
                // Log.d(_tag, "MulticastLock not held or null, no need to release.")
            }
        }
    }


    // --- NSD Listener Initialization and Callbacks ---
    private fun initializeDiscoveryListener() {
        if (discoveryListener != null) {
            Log.w(_tag, "initializeDiscoveryListener called but listener already exists. Ignoring.")
            return
        }
        Log.d(_tag, "Initializing new NSD DiscoveryListener.")
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(_tag, "NSD Discovery Started Callback: $regType")
                if (isDiscoveryActive && _discoveryState.value !is DiscoveryState.Discovered) {
                    _discoveryState.value = DiscoveryState.Searching
                }
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                val foundType = service.serviceType
                Log.v(_tag, "onServiceFound: Name=${service.serviceName}, Raw Type='$foundType'")

                // *** IMPROVED CHECK: Compare types ignoring potential trailing dot differences ***
                val expectedTypeBase = NSD_SERVICE_TYPE.removeSuffix(".")
                val foundTypeBase = foundType?.removeSuffix(".")

                if (foundTypeBase != expectedTypeBase) {
                    Log.d(_tag, "Ignoring service: Type mismatch. Found Base='${foundTypeBase}', Expected Base='${expectedTypeBase}' (Full Expected: '$NSD_SERVICE_TYPE')")
                    return // Ignore if base types don't match
                }

                // Proceed if base types match
                Log.i(_tag, "NSD Service Found with matching type base: Name=${service.serviceName}")
                val serviceKey = service.serviceName
                synchronized(discoveredServices) {
                    if (!discoveredServices.containsKey(serviceKey)) {
                        Log.d(_tag, "Adding service '$serviceKey' to discovered map and initiating resolve.")
                        discoveredServices[serviceKey] = service
                        resolveService(service)
                    } else {
                        Log.d(_tag, "Service '$serviceKey' already found or resolving. Ignoring duplicate find.")
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                val foundType = service.serviceType
                Log.v(_tag, "onServiceLost: Name=${service.serviceName}, Raw Type='$foundType'")

                // *** IMPROVED CHECK: Compare types ignoring potential trailing dot differences ***
                val expectedTypeBase = NSD_SERVICE_TYPE.removeSuffix(".")
                val foundTypeBase = foundType?.removeSuffix(".")

                if (foundTypeBase != expectedTypeBase) {
                    Log.d(_tag, "Ignoring lost service: Type mismatch. Found Base='${foundTypeBase}', Expected Base='${expectedTypeBase}'")
                    return // Ignore if types don't match
                }

                Log.w(_tag, "NSD Service Lost with matching type base: ${service.serviceName}")
                val serviceKey = service.serviceName

                synchronized(discoveredServices) {
                    discoveredServices.remove(serviceKey)
                }

                val ipToRemove = service.host?.hostAddress
                resolvedServers.update { currentResolved ->
                    val initialSize = currentResolved.size
                    val updatedResolved = if (ipToRemove != null) {
                        currentResolved.filterNot { it.ip == ipToRemove && it.port == service.port }
                    } else {
                        currentResolved
                    }
                    if (initialSize != updatedResolved.size) {
                        Log.d(_tag, "Removed lost server '$serviceKey'. Updated resolved list size: ${updatedResolved.size}")
                    }
                    updatedResolved
                }
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.i(_tag, "NSD Discovery Stopped Callback: $serviceType")
                isDiscoveryActive = false
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(_tag, "NSD Start Discovery Failed Callback: Type=$serviceType, ErrorCode=$errorCode")
                _discoveryState.value = DiscoveryState.Error("Discovery start failed (Code: $errorCode)")
                isDiscoveryActive = false
                releaseMulticastLock()
                discoveryListener = null
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(_tag, "NSD Stop Discovery Failed Callback: Type=$serviceType, ErrorCode=$errorCode")
                isDiscoveryActive = false
                releaseMulticastLock()
                discoveryListener = null
            }
        }
    }

    // --- Resolve Service ---
    private fun resolveService(serviceInfo: NsdServiceInfo) {
        val serviceKey = serviceInfo.serviceName
        Log.d(_tag, "Attempting to resolve service: $serviceKey")
        try {
            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(failedServiceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(_tag, "NSD Resolve Failed for ${failedServiceInfo.serviceName}: ErrorCode=$errorCode")
                    synchronized(discoveredServices) {
                        discoveredServices.remove(failedServiceInfo.serviceName)
                    }
                }

                override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                    Log.i(_tag, "NSD Service Resolved: ${resolvedServiceInfo.serviceName}")
                    val host: InetAddress? = resolvedServiceInfo.host
                    val port: Int = resolvedServiceInfo.port
                    val ipAddress: String? = host?.hostAddress

                    if (ipAddress != null && ipAddress != "0.0.0.0" && !ipAddress.startsWith("127.") && !ipAddress.startsWith("::1") && port > 0) {
                        val newServer = NetworkConfig(ip = ipAddress, port = port)
                        Log.d(_tag, "Resolved to valid address: IP=$ipAddress, Port=$port")
                        resolvedServers.update { currentResolved ->
                            val exists = currentResolved.any { it.ip == newServer.ip && it.port == newServer.port }
                            if (!exists) {
                                (currentResolved + newServer).distinctBy { "${it.ip}:${it.port}" }
                            } else {
                                Log.d(_tag, "Resolved server $newServer already in list.")
                                currentResolved
                            }
                        }
                    } else {
                        Log.w(_tag, "Resolved service ${resolvedServiceInfo.serviceName} resulted in invalid host/IP ($ipAddress) or port ($port). Ignoring.")
                    }
                    synchronized(discoveredServices) {
                        discoveredServices.remove(resolvedServiceInfo.serviceName)
                    }
                }
            })
        } catch (e: IllegalArgumentException) {
            Log.e(_tag, "NSD Resolve Error (IllegalArgumentException) for ${serviceInfo.serviceName}. Resolve may already be pending.", e)
            synchronized(discoveredServices) { discoveredServices.remove(serviceInfo.serviceName) }
        } catch (e: Exception) {
            Log.e(_tag, "Exception calling nsdManager.resolveService for ${serviceInfo.serviceName}", e)
            synchronized(discoveredServices) { discoveredServices.remove(serviceInfo.serviceName) }
        }
    }


    override fun onCleared() {
        super.onCleared()
        stopDiscovery() // Ensure discovery stops and lock is released
        Log.d(_tag, "ViewModel cleared.")
    }
}
