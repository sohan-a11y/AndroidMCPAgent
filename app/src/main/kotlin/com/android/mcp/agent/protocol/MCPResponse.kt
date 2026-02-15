package com.android.mcp.agent.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Represents an outgoing response to the AI client.
 *
 * Example success:
 * ```json
 * {
 *   "id": "cmd-001",
 *   "status": "success",
 *   "result": { "clicked": true, "confidence": 0.92 }
 * }
 * ```
 *
 * Example error:
 * ```json
 * {
 *   "id": "cmd-001",
 *   "status": "error",
 *   "error": { "code": "PERMISSION_DENIED", "message": "UI clicks are disabled" }
 * }
 * ```
 */
@Serializable
data class MCPResponse(
    val id: String,
    val status: String,
    val result: JsonElement? = null,
    val error: MCPErrorPayload? = null
) {
    companion object {
        const val STATUS_SUCCESS = "success"
        const val STATUS_ERROR = "error"

        fun success(id: String, result: JsonElement? = null): MCPResponse {
            return MCPResponse(
                id = id,
                status = STATUS_SUCCESS,
                result = result
            )
        }

        fun success(id: String, data: Map<String, JsonElement>): MCPResponse {
            return MCPResponse(
                id = id,
                status = STATUS_SUCCESS,
                result = JsonObject(data)
            )
        }

        fun error(id: String, code: MCPError, message: String? = null): MCPResponse {
            return MCPResponse(
                id = id,
                status = STATUS_ERROR,
                error = MCPErrorPayload(
                    code = code.name,
                    message = message ?: code.defaultMessage
                )
            )
        }
    }
}

@Serializable
data class MCPErrorPayload(
    val code: String,
    val message: String
)
