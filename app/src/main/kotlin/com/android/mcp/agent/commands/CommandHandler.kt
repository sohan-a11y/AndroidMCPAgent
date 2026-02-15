package com.android.mcp.agent.commands

import kotlinx.serialization.json.JsonElement

/**
 * Interface for all MCP command handlers.
 *
 * Each handler implements a single action type (e.g., click, scroll, open_app).
 * Handlers are registered with the [CommandRouter] and dispatched based on action name.
 */
interface CommandHandler {
    /** The action name this handler responds to (e.g., "click_by_text"). */
    val action: String

    /** The permission category required (used for permission checking). */
    val requiredPermission: String

    /**
     * Execute the command with the given parameters.
     *
     * @param params The command parameters as a JSON element map.
     * @return A [CommandResult] indicating success or failure.
     */
    suspend fun execute(params: Map<String, JsonElement>): CommandResult
}
