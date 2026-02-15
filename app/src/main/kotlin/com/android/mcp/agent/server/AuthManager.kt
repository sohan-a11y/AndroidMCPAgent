package com.android.mcp.agent.server

import android.util.Log
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages authentication for AI client connections.
 *
 * Flow:
 * 1. Server generates a 6-digit pairing code (displayed on device)
 * 2. Client sends "pair" command with the code
 * 3. If correct, server issues an auth token (time-limited)
 * 4. Client includes token in subsequent requests
 *
 * Security:
 * - Pairing codes expire after 5 minutes
 * - Auth tokens expire after configurable duration (default 1 hour)
 * - Only one active session per device (MVP)
 * - All tokens stored in memory only (not persisted)
 */
class AuthManager(
    private val tokenExpiryMs: Long = 3_600_000L // 1 hour default
) {
    companion object {
        private const val TAG = "AuthManager"
        private const val PAIRING_CODE_LENGTH = 6
        private const val PAIRING_CODE_EXPIRY_MS = 300_000L // 5 minutes
    }

    private val random = SecureRandom()
    private val activeTokens = ConcurrentHashMap<String, TokenInfo>()

    @Volatile
    private var currentPairingCode: String? = null
    private var pairingCodeCreatedAt: Long = 0L

    data class TokenInfo(
        val token: String,
        val clientId: String,
        val createdAt: Long,
        val expiresAt: Long
    ) {
        val isExpired: Boolean
            get() = System.currentTimeMillis() > expiresAt
    }

    /**
     * Generate a new 6-digit pairing code.
     * Invalidates any previous code.
     */
    fun generatePairingCode(): String {
        val code = (0 until PAIRING_CODE_LENGTH)
            .map { random.nextInt(10) }
            .joinToString("")
        currentPairingCode = code
        pairingCodeCreatedAt = System.currentTimeMillis()
        Log.i(TAG, "New pairing code generated")
        return code
    }

    /**
     * Attempt to pair with a code. Returns auth token if successful.
     */
    fun attemptPairing(code: String, clientId: String): String? {
        val expectedCode = currentPairingCode ?: return null

        // Check expiry
        if (System.currentTimeMillis() - pairingCodeCreatedAt > PAIRING_CODE_EXPIRY_MS) {
            currentPairingCode = null
            Log.w(TAG, "Pairing code expired")
            return null
        }

        // Check code
        if (code != expectedCode) {
            Log.w(TAG, "Invalid pairing code attempt from $clientId")
            return null
        }

        // Invalidate pairing code (one-time use)
        currentPairingCode = null

        // Revoke any existing tokens (one session at a time for MVP)
        activeTokens.clear()

        // Generate token
        val token = generateToken()
        val now = System.currentTimeMillis()
        activeTokens[token] = TokenInfo(
            token = token,
            clientId = clientId,
            createdAt = now,
            expiresAt = now + tokenExpiryMs
        )

        Log.i(TAG, "Pairing successful for client: $clientId")
        return token
    }

    /**
     * Validate an auth token.
     * @return The client ID if valid, null otherwise.
     */
    fun validateToken(token: String): String? {
        val info = activeTokens[token] ?: return null

        if (info.isExpired) {
            activeTokens.remove(token)
            Log.i(TAG, "Token expired for client: ${info.clientId}")
            return null
        }

        return info.clientId
    }

    /**
     * Revoke all active tokens (disconnect all clients).
     */
    fun revokeAll() {
        activeTokens.clear()
        currentPairingCode = null
        Log.i(TAG, "All tokens revoked")
    }

    /**
     * Get the current pairing code (for display on device).
     * Returns null if no active code or code has expired.
     */
    fun getCurrentPairingCode(): String? {
        val code = currentPairingCode ?: return null
        if (System.currentTimeMillis() - pairingCodeCreatedAt > PAIRING_CODE_EXPIRY_MS) {
            currentPairingCode = null
            return null
        }
        return code
    }

    /**
     * Check if there's an active authenticated session.
     */
    fun hasActiveSession(): Boolean {
        // Clean expired tokens
        activeTokens.entries.removeAll { it.value.isExpired }
        return activeTokens.isNotEmpty()
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
