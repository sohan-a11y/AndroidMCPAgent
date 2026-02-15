package com.android.mcp.agent.commands

import com.android.mcp.agent.protocol.MCPError
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Sealed class representing the result of a command execution.
 */
sealed class CommandResult {

    /**
     * Command executed successfully.
     * @param data Key-value result data to return to the client.
     */
    data class Success(
        val data: Map<String, JsonElement> = emptyMap()
    ) : CommandResult() {

        companion object {
            /** Simple success with a boolean flag. */
            fun ok(key: String = "success", value: Boolean = true): Success {
                return Success(mapOf(key to JsonPrimitive(value)))
            }

            /** Success with a string result. */
            fun withMessage(message: String): Success {
                return Success(mapOf("message" to JsonPrimitive(message)))
            }

            /** Success with arbitrary data. */
            fun withData(vararg pairs: Pair<String, JsonElement>): Success {
                return Success(pairs.toMap())
            }
        }
    }

    /**
     * Command failed.
     * @param error The error code.
     * @param message Optional detailed message.
     */
    data class Error(
        val error: MCPError,
        val message: String? = null
    ) : CommandResult()
}
