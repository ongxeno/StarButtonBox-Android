package com.ongxeno.android.starbuttonbox.datasource

import android.content.Context
import android.util.Log
import com.ongxeno.android.starbuttonbox.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import java.io.IOException
import java.net.BindException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLocalWebServer @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope
) {
    private val TAG = "AppLocalWebServer"
    private var serverJob: Job? = null
    private var serverEngine: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private var serverPort: Int? = null

    private val PREFERRED_PORT = 58008

    enum class ServerMode {
        LAYOUT_IMPORT,
        SERVER_SETUP_ASSISTANCE
    }

    suspend fun startServer(
        mode: ServerMode,
        onJsonReceived: ((String) -> Unit)? = null
    ): String? {
        if (serverJob?.isActive == true && serverEngine != null) {
            val currentIp = getWifiIpAddress()
            val currentPort = serverPort ?: PREFERRED_PORT
            Log.w(TAG, "Server already running. Current URL: http://$currentIp:$currentPort")
            return if (currentIp != null) "http://$currentIp:$currentPort" else null
        }

        val ipAddress = getWifiIpAddress()
        if (ipAddress == null) {
            Log.e(TAG, "Failed to get device Wi-Fi IP address. Cannot start server.")
            return null
        }

        val startDeferred = CompletableDeferred<EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>?>()

        serverJob = appScope.launch(Dispatchers.IO) {
            var localEngine: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
            try {
                Log.d(TAG, "Attempting to start Ktor server on port $PREFERRED_PORT for mode: $mode")
                localEngine = embeddedServer(
                    Netty,
                    port = PREFERRED_PORT,
                    host = "0.0.0.0",
                    module = { configureKtorApplication(mode, onJsonReceived) }
                ).start(wait = false)

                serverEngine = localEngine
                serverPort = PREFERRED_PORT
                Log.i(TAG, "Ktor server initiated for $mode on http://$ipAddress:$PREFERRED_PORT")
                startDeferred.complete(localEngine)
            } catch (e: BindException) {
                Log.e(TAG, "Port $PREFERRED_PORT likely in use.", e)
                serverEngine = null; serverPort = null; startDeferred.complete(null)
            } catch (e: Exception) {
                Log.e(TAG, "General failure to start Ktor server for $mode", e)
                localEngine?.stop()
                serverEngine = null; serverPort = null; startDeferred.complete(null)
            }
        }

        val startedEngineResult = try {
            withTimeoutOrNull(7000L) { startDeferred.await() }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Ktor server startup timed out.")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Exception waiting for Ktor server start deferred.", e)
            null
        }

        return if (startedEngineResult != null) {
            Log.i(TAG, "Server startup successful for $mode on port $PREFERRED_PORT.")
            "http://$ipAddress:$PREFERRED_PORT"
        } else {
            Log.e(TAG, "Ktor server startup failed or timed out on port $PREFERRED_PORT.")
            stopServerInternal()
            null
        }
    }

    private fun Application.configureKtorApplication(
        mode: ServerMode,
        onJsonReceived: ((String) -> Unit)?
    ) {
        routing {
            when (mode) {
                ServerMode.LAYOUT_IMPORT -> configureLayoutImportRoutes(onJsonReceived)
                ServerMode.SERVER_SETUP_ASSISTANCE -> configureServerSetupRoutes()
            }
        }
    }

    private fun Routing.configureLayoutImportRoutes(onJsonReceived: ((String) -> Unit)?) {
        get("/") { serveHtmlAsset("web_pages/import_page.html", call) }
        get("/import-layout") { serveHtmlAsset("web_pages/import_page.html", call) }
        post("/upload") {
            try {
                val jsonContent = call.receiveText()
                if (jsonContent.isBlank()) {
                    call.respondText("Error: Received empty content.", ContentType.Text.Plain, HttpStatusCode.BadRequest)
                    return@post
                }
                call.respondText("Import process initiated on device.", ContentType.Text.Plain, HttpStatusCode.OK)
                withContext(Dispatchers.Main) { onJsonReceived?.invoke(jsonContent) }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling POST /upload for layout import", e)
                call.respondText("Error processing upload: ${e.message}", ContentType.Text.Plain, HttpStatusCode.InternalServerError)
            }
        }
    }

    private fun Routing.configureServerSetupRoutes() {
        get("/setup-server") {
            serveHtmlAsset("web_pages/pc_server_setup.html", call)
        }
        // MODIFIED: Serve the installer EXE
        get("/download/StarButtonBoxServer_Installer_v1.0.exe") {
            serveStaticAsset("server_files/StarButtonBoxServer_Installer_v1.0.exe", ContentType.Application.OctetStream, call)
        }
        get("/") { // Fallback for root in setup mode
            serveHtmlAsset("web_pages/pc_server_setup.html", call)
        }
    }

    private suspend fun serveHtmlAsset(assetPath: String, call: ApplicationCall) {
        Log.d(TAG, "Serving HTML asset: $assetPath")
        try {
            context.assets.open(assetPath).bufferedReader().use { reader ->
                val content = reader.readText()
                call.respondText(content, ContentType.Text.Html, HttpStatusCode.OK)
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error serving HTML asset '$assetPath'", e)
            call.respondText("Error: File not found ($assetPath).", ContentType.Text.Html, HttpStatusCode.NotFound)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error serving HTML asset '$assetPath'", e)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    private suspend fun serveStaticAsset(assetPath: String, contentType: ContentType, call: ApplicationCall) {
        Log.d(TAG, "Serving static asset: $assetPath with contentType: $contentType")
        try {
            context.assets.open(assetPath).use { inputStream ->
                call.respondOutputStream(contentType, HttpStatusCode.OK) {
                    inputStream.copyTo(this)
                }
                Log.d(TAG,"Successfully served static asset: $assetPath")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error serving static asset '$assetPath'. File not found or IO error.", e)
            call.respondText("Error: File not found ($assetPath).", contentType, HttpStatusCode.NotFound)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error serving static asset '$assetPath'", e)
            call.respond(HttpStatusCode.InternalServerError, "An unexpected error occurred.")
        }
    }

    suspend fun stopServer() {
        stopServerInternal()
    }

    private suspend fun stopServerInternal() {
        coroutineScope {
            val currentEngine = serverEngine
            val currentJob = serverJob
            if (currentEngine != null || currentJob?.isActive == true) {
                Log.i(TAG, "Stopping Ktor server...")
                try {
                    currentEngine?.stop(1000L, 2000L)
                    currentJob?.cancelAndJoin()
                    Log.i(TAG, "Ktor server stopped successfully.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during Ktor server stop", e)
                } finally {
                    serverEngine = null
                    serverJob = null
                    serverPort = null
                }
            } else {
                Log.d(TAG, "stopServerInternal: Server was not running or already stopped.")
            }
        }
    }

    private fun getWifiIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces.sortedByDescending { it.name.contains("wlan", true) || it.name.contains("wifi", true) }) {
                if (intf.isLoopback || !intf.isUp || intf.isVirtual) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val ip = addr.hostAddress
                        if (ip != null && ip != "0.0.0.0") {
                            Log.i(TAG, "IP found (Interface: ${intf.displayName}): $ip")
                            return ip
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Exception getting IP: ${ex.message}", ex)
        }
        Log.e(TAG, "Failed to find a suitable non-loopback IPv4 address.")
        return null
    }

    fun onCleared() {
        Log.d(TAG, "AppLocalWebServer onCleared called. Stopping server if running.")
        appScope.launch {
            stopServerInternal()
        }
    }
}
