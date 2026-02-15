package com.android.mcp.agent.commands

import android.util.Log
import com.android.mcp.agent.logging.AuditLogger
import com.android.mcp.agent.permissions.PermissionManager
import com.android.mcp.agent.protocol.MCPCommand
import com.android.mcp.agent.protocol.MCPError
import com.android.mcp.agent.protocol.MCPResponse
import kotlinx.serialization.json.JsonObject

/**
 * Routes incoming MCP commands to the appropriate handler.
 *
 * Responsibilities:
 * - Validate the action exists
 * - Check user permissions
 * - Dispatch to the correct handler
 * - Convert results to MCP responses
 * - Log all actions via [AuditLogger]
 */
class CommandRouter(
    private val permissionManager: PermissionManager,
    private val auditLogger: AuditLogger
) {
    companion object {
        private const val TAG = "CommandRouter"
    }

    private val handlers = mutableMapOf<String, CommandHandler>()

    /**
     * Register a command handler.
     */
    fun register(handler: CommandHandler) {
        handlers[handler.action] = handler
        Log.d(TAG, "Registered handler: ${handler.action}")
    }

    /**
     * Register multiple handlers at once.
     */
    fun registerAll(vararg handlerList: CommandHandler) {
        handlerList.forEach { register(it) }
    }

    /**
     * Get list of all supported actions.
     */
    fun supportedActions(): List<String> = handlers.keys.toList()

    /**
     * Route and execute a command.
     *
     * Flow: validate → check permissions → execute → log → respond
     */
    suspend fun dispatch(command: MCPCommand, clientId: String): MCPResponse {
        val startTime = System.currentTimeMillis()

        // Handle built-in commands
        if (command.action == MCPCommand.ACTION_PING) {
            return MCPResponse.success(command.id, mapOf(
                "pong" to kotlinx.serialization.json.JsonPrimitive(true),
                "timestamp" to kotlinx.serialization.json.JsonPrimitive(System.currentTimeMillis())
            ))
        }

        // Find handler
        val handler = handlers[command.action]
        if (handler == null) {
            val response = MCPResponse.error(
                command.id,
                MCPError.INVALID_COMMAND,
                "Unknown action: ${command.action}. Supported: ${supportedActions()}"
            )
            auditLogger.log(command, clientId, response, System.currentTimeMillis() - startTime)
            return response
        }

        // Check permissions
        if (!permissionManager.isAllowed(handler.requiredPermission)) {
            val response = MCPResponse.error(
                command.id,
                MCPError.PERMISSION_DENIED,
                "Action '${command.action}' requires permission '${handler.requiredPermission}' which is disabled"
            )
            auditLogger.log(command, clientId, response, System.currentTimeMillis() - startTime)
            return response
        }

        // Execute
        return try {
            val result = handler.execute(command.params)
            val response = when (result) {
                is CommandResult.Success -> MCPResponse.success(
                    command.id,
                    JsonObject(result.data)
                )
                is CommandResult.Error -> MCPResponse.error(
                    command.id,
                    result.error,
                    result.message
                )
            }
            auditLogger.log(command, clientId, response, System.currentTimeMillis() - startTime)
            response
        } catch (e: Exception) {
            Log.e(TAG, "Handler execution failed for ${command.action}", e)
            val response = MCPResponse.error(
                command.id,
                MCPError.EXECUTION_FAILED,
                "Exception: ${e.message}"
            )
            auditLogger.log(command, clientId, response, System.currentTimeMillis() - startTime)
            response
        }
    }
}
