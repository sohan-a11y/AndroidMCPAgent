package com.android.mcp.agent.commands.handlers

import com.android.mcp.agent.commands.CommandHandler
import com.android.mcp.agent.commands.CommandResult
import com.android.mcp.agent.permissions.PermissionManager
import com.android.mcp.agent.protocol.MCPError
import kotlinx.serialization.json.JsonElement

/**
 * Takes a screenshot of the current screen.
 *
 * NOTE: This is a Phase 2 feature. MediaProjection requires a user-granted
 * screen capture permission via an Activity result callback. For MVP, this
 * handler returns a "not implemented" response.
 *
 * Phase 2 implementation will:
 * - Request MediaProjection permission
 * - Capture screen to bitmap
 * - Encode as base64 PNG
 * - Return in response
 */
class ScreenshotHandler : CommandHandler {

    override val action = "screenshot"
    override val requiredPermission = PermissionManager.PERM_SCREENSHOTS

    override suspend fun execute(params: Map<String, JsonElement>): CommandResult {
        // Phase 2: MediaProjection implementation
        return CommandResult.Error(
            MCPError.NOT_IMPLEMENTED,
            "Screenshot capture requires MediaProjection (Phase 2). " +
            "Use 'get_screen_text' for text-based screen reading."
        )
    }
}
