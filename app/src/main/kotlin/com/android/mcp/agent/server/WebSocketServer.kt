package com.android.mcp.agent.server

import android.util.Log
import com.android.mcp.agent.commands.CommandRouter
import com.android.mcp.agent.protocol.MCPCommand
import com.android.mcp.agent.protocol.MCPError
import com.android.mcp.agent.protocol.MCPResponse
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import io.ktor.server.plugins.origin
import io.ktor.server.response.respondText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration

/**
 * Ktor-based WebSocket server for MCP protocol communication.
 *
 * Runs on the device, accepts connections from AI clients.
 * All communication uses JSON over WebSocket.
 *
 * Connection flow:
 * 1. Client connects to ws://device-ip:port/mcp
 * 2. Client sends "pair" command with pairing code
 * 3. Server returns auth token
 * 4. Client sends commands with auth token
 */
class WebSocketServer(
    private val authManager: AuthManager,
    private val sessionManager: SessionManager,
    private val commandRouter: CommandRouter,
    private val port: Int = 8765
) {
    companion object {
        private const val TAG = "WebSocketServer"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private var server: ApplicationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    /**
     * Start the WebSocket server.
     */
    fun start() {
        if (_isRunning.value) {
            Log.w(TAG, "Server already running")
            return
        }

        server = embeddedServer(CIO, port = port) {
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(30)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                webSocket("/mcp") {
                    handleMCPSession(this)
                }

                // Health check endpoint (non-WS)
                get("/health") {
                    call.respondText("""{"status":"ok","version":"0.1.0"}""")
                }
            }
        }

        scope.launch {
            try {
                server?.start(wait = false)
                _isRunning.value = true
                Log.i(TAG, "WebSocket server started on port $port")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start server", e)
                _isRunning.value = false
            }
        }
    }

    /**
     * Stop the WebSocket server.
     */
    fun stop() {
        scope.launch {
            try {
                server?.stop(1000, 2000)
                server = null
                _isRunning.value = false
                sessionManager.disconnectAll()
                Log.i(TAG, "WebSocket server stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping server", e)
            }
        }
    }

    /**
     * Handle an individual WebSocket session.
     */
    private suspend fun handleMCPSession(session: WebSocketServerSession) {
        var clientId: String? = null

        try {
            for (frame in session.incoming) {
                if (frame !is Frame.Text) continue

                val rawText = frame.readText()
                Log.d(TAG, "Received: $rawText")

                val command = try {
                    json.decodeFromString<MCPCommand>(rawText)
                } catch (e: Exception) {
                    val errorResponse = MCPResponse.error(
                        "unknown",
                        MCPError.INVALID_COMMAND,
                        "Failed to parse command JSON: ${e.message}"
                    )
                    session.send(Frame.Text(json.encodeToString(errorResponse)))
                    continue
                }

                // Handle pairing
                if (command.action == MCPCommand.ACTION_PAIR) {
                    val pairingCode = command.params["code"]
                        ?.let { json.decodeFromString<String>(it.toString()) }
                        ?.replace("\"", "")
                    val requestedClientId = command.params["client_id"]
                        ?.let { it.toString().replace("\"", "") }
                        ?: "client-${System.currentTimeMillis()}"

                    if (pairingCode == null) {
                        session.send(Frame.Text(json.encodeToString(
                            MCPResponse.error(command.id, MCPError.MISSING_PARAMS, "Missing 'code' param")
                        )))
                        continue
                    }

                    val token = authManager.attemptPairing(pairingCode, requestedClientId)
                    if (token != null) {
                        clientId = requestedClientId
                        sessionManager.onClientConnected(
                            clientId,
                            session.call.request.origin.remoteHost
                        )
                        session.send(Frame.Text(json.encodeToString(
                            MCPResponse.success(command.id, mapOf(
                                "auth_token" to kotlinx.serialization.json.JsonPrimitive(token),
                                "expires_in_ms" to kotlinx.serialization.json.JsonPrimitive(3_600_000L),
                                "supported_actions" to kotlinx.serialization.json.JsonArray(
                                    commandRouter.supportedActions().map {
                                        kotlinx.serialization.json.JsonPrimitive(it)
                                    }
                                )
                            ))
                        )))
                    } else {
                        session.send(Frame.Text(json.encodeToString(
                            MCPResponse.error(command.id, MCPError.PAIRING_FAILED)
                        )))
                    }
                    continue
                }

                // Validate auth for all other commands
                val token = command.auth_token
                val validatedClientId = if (token != null) {
                    authManager.validateToken(token)
                } else null

                if (validatedClientId == null) {
                    session.send(Frame.Text(json.encodeToString(
                        MCPResponse.error(command.id, MCPError.UNAUTHORIZED)
                    )))
                    continue
                }

                // Dispatch command
                val response = commandRouter.dispatch(command, validatedClientId)
                session.send(Frame.Text(json.encodeToString(response)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Session error", e)
        } finally {
            clientId?.let { sessionManager.onClientDisconnected(it) }
            Log.i(TAG, "Session ended for client: $clientId")
        }
    }

    /**
     * Get the server port.
     */
    fun getPort(): Int = port
}
