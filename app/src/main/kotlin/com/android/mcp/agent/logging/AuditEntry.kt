package com.android.mcp.agent.logging

import kotlinx.serialization.Serializable

/**
 * A single audit log entry representing one MCP command execution.
 */
@Serializable
data class AuditEntry(
    val id: Long,
    val timestamp: Long,
    val clientId: String,
    val action: String,
    val params: String,         // JSON string of params
    val status: String,         // "success" or "error"
    val errorCode: String? = null,
    val durationMs: Long
) {
    val timestampFormatted: String
        get() {
            val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }

    val isSuccess: Boolean
        get() = status == "success"
}
