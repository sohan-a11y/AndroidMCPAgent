package com.android.mcp.agent.logging

import android.util.Log
import com.android.mcp.agent.protocol.MCPCommand
import com.android.mcp.agent.protocol.MCPResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicLong

/**
 * Append-only audit log for all MCP commands.
 *
 * Every command that passes through the [CommandRouter] is logged here,
 * regardless of success or failure. Logs are kept in memory for MVP.
 *
 * Future: persist to Room database, export as JSON/CSV.
 */
class AuditLogger {

    companion object {
        private const val TAG = "AuditLogger"
        private const val MAX_ENTRIES = 1000
    }

    private val json = Json { prettyPrint = false }
    private val counter = AtomicLong(0)

    private val entries = mutableListOf<AuditEntry>()
    private val _entriesFlow = MutableStateFlow<List<AuditEntry>>(emptyList())
    val entriesFlow: StateFlow<List<AuditEntry>> = _entriesFlow.asStateFlow()

    /**
     * Log a command execution.
     */
    fun log(
        command: MCPCommand,
        clientId: String,
        response: MCPResponse,
        durationMs: Long
    ) {
        val entry = AuditEntry(
            id = counter.incrementAndGet(),
            timestamp = System.currentTimeMillis(),
            clientId = clientId,
            action = command.action,
            params = try {
                json.encodeToString(command.params)
            } catch (_: Exception) {
                "{}"
            },
            status = response.status,
            errorCode = response.error?.code,
            durationMs = durationMs
        )

        synchronized(entries) {
            entries.add(entry)
            // Trim old entries
            if (entries.size > MAX_ENTRIES) {
                entries.removeAt(0)
            }
            _entriesFlow.value = entries.toList()
        }

        Log.d(TAG, "[${entry.timestampFormatted}] ${entry.action} → ${entry.status} (${entry.durationMs}ms)")
    }

    /**
     * Get all log entries.
     */
    fun getEntries(): List<AuditEntry> {
        return synchronized(entries) { entries.toList() }
    }

    /**
     * Get recent entries (most recent first).
     */
    fun getRecentEntries(limit: Int = 50): List<AuditEntry> {
        return synchronized(entries) {
            entries.takeLast(limit).reversed()
        }
    }

    /**
     * Clear all log entries.
     */
    fun clear() {
        synchronized(entries) {
            entries.clear()
            _entriesFlow.value = emptyList()
        }
        Log.i(TAG, "Audit log cleared")
    }

    /**
     * Get total number of entries.
     */
    fun count(): Int = synchronized(entries) { entries.size }
}
