package com.android.mcp.agent.server

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages active WebSocket sessions.
 *
 * MVP: Only one active session at a time.
 * Future: Multiple clients with different permission levels.
 */
class SessionManager {

    companion object {
        private const val TAG = "SessionManager"
    }

    data class Session(
        val clientId: String,
        val connectedAt: Long = System.currentTimeMillis(),
        val remoteAddress: String = "unknown"
    )

    private val _activeSession = MutableStateFlow<Session?>(null)
    val activeSession: StateFlow<Session?> = _activeSession.asStateFlow()

    private val _connectionCount = MutableStateFlow(0)
    val connectionCount: StateFlow<Int> = _connectionCount.asStateFlow()

    /**
     * Register a new authenticated session.
     * Disconnects any existing session (MVP: one at a time).
     */
    fun onClientConnected(clientId: String, remoteAddress: String = "unknown"): Session {
        val session = Session(
            clientId = clientId,
            remoteAddress = remoteAddress
        )
        _activeSession.value = session
        _connectionCount.value++
        Log.i(TAG, "Client connected: $clientId from $remoteAddress")
        return session
    }

    /**
     * Remove a client session.
     */
    fun onClientDisconnected(clientId: String) {
        if (_activeSession.value?.clientId == clientId) {
            _activeSession.value = null
            Log.i(TAG, "Client disconnected: $clientId")
        }
    }

    /**
     * Check if a specific client is currently connected.
     */
    fun isClientConnected(clientId: String): Boolean {
        return _activeSession.value?.clientId == clientId
    }

    /**
     * Disconnect all clients.
     */
    fun disconnectAll() {
        _activeSession.value = null
        Log.i(TAG, "All sessions cleared")
    }
}
