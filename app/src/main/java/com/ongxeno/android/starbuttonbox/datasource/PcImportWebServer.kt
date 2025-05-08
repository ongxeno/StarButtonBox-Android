package com.ongxeno.android.starbuttonbox.datasource // Or your preferred network package

import android.content.Context
// Removed WifiManager import as the deprecated method is removed
import android.util.Log
import com.ongxeno.android.starbuttonbox.di.ApplicationScope // Hilt scope qualifier
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.* // For ContentType, HttpStatusCode
import io.ktor.server.application.* // For call object, install, Application
import io.ktor.server.engine.* // For ApplicationEngine
import io.ktor.server.netty.* // For Netty engine
// Removed Ktor logging imports
import io.ktor.server.request.* // For receiveText, path()
import io.ktor.server.response.* // For respondText
import io.ktor.server.routing.* // For routing { get, post }
import kotlinx.coroutines.* // For CoroutineScope, Job, Dispatchers, withContext, cancelAndJoin, delay
import java.io.IOException // For exception handling
import java.net.Inet4Address // For IP address checking
import java.net.NetworkInterface // For IP address checking
import java.util.Collections // For IP address checking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages a temporary Ktor HTTP server on the Android device
 * to facilitate importing layout files from a PC browser.
 * Injected as a Singleton by Hilt.
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
    private var serverJob: Job? = null // Job for the server coroutine
    private var serverEngine: ApplicationEngine? = null // Holds the running server instance
    private var serverPort: Int? = null // Holds the dynamically assigned port

    /**
     * Starts the embedded Ktor Netty server on an available port (port 0).
     * Configures routing for serving the HTML upload page and handling the POST request.
     * Needs INTERNET and ACCESS_WIFI_STATE permissions.
     *
     * @param onJsonReceived Callback function invoked when JSON content is successfully received via POST /upload.
     * @return The URL (http://<device_ip>:<port>) the server is listening on, or null if failed to start.
     */
    suspend fun startServer(onJsonReceived: (String) -> Unit): String? {
        // Prevent starting if already running
        if (serverEngine?.application?.isActive == true || serverJob?.isActive == true) {
            Log.w(TAG, "Server already running or starting.")
            val ip = getWifiIpAddress()
            return if (ip != null && serverPort != null) "http://$ip:$serverPort" else {
                Log.w(TAG, "Server was running but couldn't get current IP/Port for URL.")
                null
            }
        }

        stopServerInternal() // Stop any potentially lingering server

        val ipAddress = getWifiIpAddress()
        if (ipAddress == null) {
            Log.e(TAG, "Failed to get device Wi-Fi IP address. Cannot start server.")
            return null
        }

        var startedEngine: ApplicationEngine? = null
        var assignedPort: Int? = null
        var startError: Exception? = null

        serverJob = appScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Attempting to start Ktor server on port 0...")
                startedEngine = embeddedServer(Netty, port = 0) {
                    // CallLogging plugin removed
                    configureRouting(onJsonReceived) // Configure routes
                }.start(wait = false)

                assignedPort = startedEngine?.environment?.connectors?.firstOrNull()?.port
                serverEngine = startedEngine

                if (assignedPort != null) {
                    Log.i(TAG, "Ktor server started successfully on http://$ipAddress:$assignedPort")
                } else {
                    throw IOException("Failed to determine server port after starting.")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Ktor server", e)
                startError = e
                startedEngine?.stop(100, 500)
                serverEngine = null
                serverPort = null
            }
        }

        delay(500) // Wait briefly

        if (startError != null || serverEngine == null || assignedPort == null) {
            Log.e(TAG, "Server startup failed or port not assigned.", startError)
            serverJob?.cancelAndJoin()
            serverJob = null
            return null
        }

        serverPort = assignedPort
        return "http://$ipAddress:$serverPort"
    }

    /**
     * Configures the Ktor application routing.
     *
     * @param onJsonReceived The callback to invoke when JSON is received.
     */
    private fun Application.configureRouting(onJsonReceived: (String) -> Unit) {
        routing {
            get("/") {
                try {
                    val htmlContent = createImportHtml()
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
                    val jsonContent = call.receiveText()
                    if (jsonContent.isBlank()) {
                        Log.w(TAG, "POST /upload - Received empty content.")
                        call.respondText("Error: Received empty content.", ContentType.Text.Plain, HttpStatusCode.BadRequest)
                        return@post
                    }
                    Log.i(TAG, "POST /upload - Received JSON content (Size: ${jsonContent.length} bytes)")

                    withContext(Dispatchers.Main) {
                        onJsonReceived(jsonContent)
                    }
                    call.respondText("Import process initiated on device.", ContentType.Text.Plain, HttpStatusCode.OK)
                    Log.d(TAG, "POST /upload - Responded OK to browser.")

                } catch (e: Exception) {
                    Log.e(TAG, "Error handling POST /upload", e)
                    call.respondText("Error processing upload: ${e.message}", ContentType.Text.Plain, HttpStatusCode.InternalServerError)
                }
            }
        }
    }

    /**
     * Stops the Ktor server gracefully if it's running.
     */
    suspend fun stopServer() {
        stopServerInternal()
    }

    /** Internal stop function */
    private suspend fun stopServerInternal() {
        if (serverEngine != null || serverJob?.isActive == true) {
            Log.i(TAG, "Stopping Ktor server...")
            try {
                serverEngine?.stop(1000, 2000)
                serverJob?.cancelAndJoin()
                Log.i(TAG, "Ktor server stopped.")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping Ktor server", e)
            } finally {
                serverEngine = null
                serverJob = null
                serverPort = null
            }
        } else {
            Log.d(TAG, "stopServerInternal called, but server was not running.")
        }
    }

    /**
     * Reads the HTML content for the file upload page from the assets folder.
     *
     * @return A String containing the HTML document, or a basic error page if reading fails.
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
     * Requires INTERNET permission. ACCESS_WIFI_STATE is no longer strictly needed by this method.
     *
     * @return The IPv4 address string or null if not found or error occurs.
     */
    private fun getWifiIpAddress(): String? {
        Log.d(TAG, "Attempting to get device IP address...")
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                // Filter out loopback, virtual, and down interfaces
                if (intf.isLoopback || !intf.isUp || intf.isVirtual) continue

                // Prioritize interfaces likely to be Wi-Fi
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
            // If no Wi-Fi hinted interface gave an IP, check all other up/non-loopback/non-virtual interfaces
            Log.w(TAG, "No typical Wi-Fi interface provided a usable IP, checking all others...")
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp || intf.isVirtual) continue
                // Skip if already checked (Wi-Fi hint)
                if (intf.displayName.contains("wlan", ignoreCase = true) || intf.displayName.contains("wifi", ignoreCase = true)) continue

                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress
                        if (ip != null && ip != "0.0.0.0") {
                            Log.w(TAG, "Using IP from other interface (${intf.displayName}): $ip")
                            return ip // Return first non-loopback IPv4 found
                        }
                    }
                }
            }

        } catch (ex: Exception) {
            Log.e(TAG, "Exception getting IP via NetworkInterface: ${ex.message}", ex)
        }

        // Deprecated WifiManager fallback removed

        Log.e(TAG, "Failed to find a suitable non-loopback IPv4 address.")
        return null // Return null if no IP found
    }
}
