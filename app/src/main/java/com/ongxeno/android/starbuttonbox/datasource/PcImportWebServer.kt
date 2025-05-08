package com.ongxeno.android.starbuttonbox.datasource // Or your preferred network package

import android.content.Context
import android.util.Log
import com.ongxeno.android.starbuttonbox.di.ApplicationScope // Hilt scope qualifier
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.* // For ContentType, HttpStatusCode
import io.ktor.server.application.* // For call object, install, Application
// --- Updated Ktor 3 Imports ---
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.EmbeddedServer // Import EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty // Import Netty engine factory
import io.ktor.server.netty.NettyApplicationEngine // Import specific engine type
// --- End Updated Ktor 3 Imports ---
import io.ktor.server.request.* // For receiveText
import io.ktor.server.response.* // For respondText
import io.ktor.server.routing.* // For routing { get, post }
import kotlinx.coroutines.* // For CoroutineScope, Job, Dispatchers, withContext, cancelAndJoin, delay, CompletableDeferred, withTimeoutOrNull
import java.io.IOException // For exception handling
import java.net.Inet4Address // For IP address checking
import java.net.NetworkInterface // For IP address checking
import java.net.BindException // To catch port in use errors
import java.util.Collections // For IP address checking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a temporary Ktor HTTP server on the Android device
 * to facilitate importing layout files from a PC browser.
 * Using Ktor 3.x APIs.
 *
 * @property context Application context.
 * @property appScope Application-level CoroutineScope for launching server tasks.
 */
@Singleton
class PcImportWebServer @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope // Use the qualified scope
) {
    private val TAG = "PcImportWebServer"
    private var serverJob: Job? = null
    // --- Updated Type Declaration for Ktor 3 Netty Engine ---
    private var serverEngine: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    // --- End Updated Type Declaration ---
    private var serverPort: Int? = null // Store the actual assigned port

    // --- Define a fixed port to try ---
    private val FIXED_PORT = 58008 // Example high port number

    /**
     * Starts the embedded Ktor Netty server on a FIXED port using Ktor 3.x APIs.
     *
     * @param onJsonReceived Callback function invoked when JSON content is successfully received via POST /upload.
     * @return The URL (http://<device_ip>:<port>) the server is listening on, or null if failed to start.
     */
    suspend fun startServer(onJsonReceived: (String) -> Unit): String? {
        // Check using serverJob and serverEngine null status
        if (serverJob?.isActive == true && serverEngine != null) {
            Log.w(TAG, "Server already running or starting (Job active).")
            val ip = getWifiIpAddress()
            return if (ip != null) "http://$ip:$FIXED_PORT" else {
                Log.w(TAG, "Server was running but couldn't get current IP for URL.")
                null
            }
        }

        val ipAddress = getWifiIpAddress()
        if (ipAddress == null) {
            Log.e(TAG, "Failed to get device Wi-Fi IP address. Cannot start server.")
            return null
        }

        // Deferred to signal completion or failure of startup attempt
        val startDeferred = CompletableDeferred<EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>?>() // Deferred holds the specific engine type

        serverJob = appScope.launch(Dispatchers.IO) {
            var localStartedEngine: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null // Use specific type
            try {
                Log.d(TAG, "Attempting to start Ktor 3 server on fixed port $FIXED_PORT...")
                localStartedEngine = embeddedServer( // embeddedServer returns the specific type
                    Netty,
                    port = FIXED_PORT,
                    host = "0.0.0.0",
                    module = { configureRouting(onJsonReceived) }
                ).start(wait = false)

                serverEngine = localStartedEngine // Assign specific type to specific type variable
                serverPort = FIXED_PORT

                Log.i(TAG, "Ktor 3 server startup sequence initiated on http://$ipAddress:$FIXED_PORT")
                startDeferred.complete(localStartedEngine) // Signal success

            } catch (e: BindException) {
                Log.e(TAG, "Failed to start Ktor server on port $FIXED_PORT - Port likely already in use.", e)
                serverEngine = null
                serverPort = null
                startDeferred.complete(null) // Signal failure
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Ktor server on port $FIXED_PORT", e)
                localStartedEngine?.stop()
                serverEngine = null
                serverPort = null
                startDeferred.complete(null) // Signal failure
            }
        }

        // Wait for the startup attempt to complete (or fail)
        val startedEngineResult = try {
            withTimeoutOrNull(5000L) {
                startDeferred.await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception waiting for start deferred", e)
            null
        }

        // Check outcome
        return if (startedEngineResult != null) {
            Log.i(TAG, "Server startup successful on fixed port $FIXED_PORT.")
            "http://$ipAddress:$FIXED_PORT"
        } else {
            Log.e(TAG, "Server startup failed or timed out on fixed port $FIXED_PORT.")
            stopServerInternal() // Ensure server is stopped
            null
        }
    }

    /**
     * Configures the Ktor application routing using Ktor 3.x DSL.
     */
    private fun Application.configureRouting(onJsonReceived: (String) -> Unit) {
        routing {
            get("/") {
                try {
                    val htmlContent = createImportHtml() // Read from assets
                    if (htmlContent.contains("Error loading page")) {
                        call.respondText(htmlContent, ContentType.Text.Html, HttpStatusCode.InternalServerError)
                    } else {
                        call.respondText(htmlContent, ContentType.Text.Html)
                        Log.d(TAG, "GET / - Served import page.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error serving GET /", e)
                    call.respondText("Error loading page: ${e.message}", ContentType.Text.Html, HttpStatusCode.InternalServerError)
                }
            }
            post("/upload") {
                try {
                    val jsonContent = call.receiveText() // Ktor 3 receive text
                    if (jsonContent.isBlank()) {
                        Log.w(TAG, "POST /upload - Received empty content.")
                        call.respondText("Error: Received empty content.", ContentType.Text.Plain, HttpStatusCode.BadRequest)
                        return@post
                    }
                    Log.i(TAG, "POST /upload - Received JSON content (Size: ${jsonContent.length} bytes)")

                    call.respondText("Import process initiated on device.", ContentType.Text.Plain, HttpStatusCode.OK)
                    withContext(Dispatchers.Main) { // Assuming callback might touch UI state
                        onJsonReceived(jsonContent)
                    }
                    Log.d(TAG, "POST /upload - Responded OK to browser.")

                } catch (e: Exception) {
                    Log.e(TAG, "Error handling POST /upload", e)
                    call.respondText("Error processing upload: ${e.message}", ContentType.Text.Plain, HttpStatusCode.InternalServerError)
                }
            }
        }
    }

    /**
     * Stops the Ktor server gracefully using Ktor 3 stop method.
     */
    suspend fun stopServer() {
        stopServerInternal()
    }

    /** Internal stop function */
    private suspend fun stopServerInternal() {
        coroutineScope {
            val currentEngine = serverEngine
            val currentJob = serverJob
            // Check if engine exists OR job is active
            if (currentEngine != null || currentJob?.isActive == true) {
                Log.i(TAG, "Stopping Ktor 3 server...")
                try {
                    currentEngine?.stop(300, 5000) // Call stop on the specific type
                    currentJob?.cancelAndJoin()
                    Log.i(TAG, "Ktor 3 server stopped.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping Ktor 3 server", e)
                } finally {
                    serverEngine = null
                    serverJob = null
                    serverPort = null
                }
            } else {
                Log.d(TAG, "stopServerInternal called, but server was not running.")
            }
        }
    }

    /**
     * Reads the HTML content for the file upload page from the assets folder.
     */
    private fun createImportHtml(): String {
        Log.d(TAG, "Reading import_page.html from assets...")
        return try {
            context.assets.open("import_page.html").bufferedReader().use {
                it.readText()
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading import_page.html from assets", e)
            """
            <!DOCTYPE html><html><head><title>Error</title></head>
            <body><h1>Error loading import page</h1><p>Could not read 'import_page.html' from application assets: ${e.message}</p></body></html>
            """.trimIndent()
        }
    }

    /**
     * Attempts to get the device's local IPv4 address, prioritizing Wi-Fi interfaces.
     */
    private fun getWifiIpAddress(): String? {
        Log.d(TAG, "Attempting to get device IP address...")
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp || intf.isVirtual) continue
                val isWifi = intf.displayName.contains("wlan", ignoreCase = true) ||
                        intf.displayName.contains("wifi", ignoreCase = true)
                if (isWifi) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val ip = addr.hostAddress
                            if (ip != null && ip != "0.0.0.0") {
                                Log.i(TAG, "IP found via NetworkInterface (Wi-Fi Hint: ${intf.displayName}): $ip")
                                return ip
                            }
                        }
                    }
                }
            }
            Log.w(TAG, "No typical Wi-Fi interface provided a usable IP, checking all others...")
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp || intf.isVirtual) continue
                if (intf.displayName.contains("wlan", ignoreCase = true) || intf.displayName.contains("wifi", ignoreCase = true)) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress
                        if (ip != null && ip != "0.0.0.0") {
                            Log.w(TAG, "Using IP from other interface (${intf.displayName}): $ip")
                            return ip
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Exception getting IP via NetworkInterface: ${ex.message}", ex)
        }
        Log.e(TAG, "Failed to find a suitable non-loopback IPv4 address.")
        return null
    }
}
