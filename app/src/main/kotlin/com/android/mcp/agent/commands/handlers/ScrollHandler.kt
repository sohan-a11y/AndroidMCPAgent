package com.android.mcp.agent.commands.handlers

import android.view.accessibility.AccessibilityNodeInfo
import com.android.mcp.agent.accessibility.MCPAccessibilityService
import com.android.mcp.agent.accessibility.NodeExtensions.findScrollable
import com.android.mcp.agent.commands.CommandHandler
import com.android.mcp.agent.commands.CommandResult
import com.android.mcp.agent.permissions.PermissionManager
import com.android.mcp.agent.protocol.MCPError
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Scrolls the screen in a specified direction.
 *
 * Params:
 * - "direction" (optional): "up", "down", "left", "right" (default: "down")
 * - "view_id" (optional): Specific scrollable view to scroll
 */
class ScrollHandler : CommandHandler {

    override val action = "scroll"
    override val requiredPermission = PermissionManager.PERM_CLICKS

    override suspend fun execute(params: Map<String, JsonElement>): CommandResult {
        val service = MCPAccessibilityService.instance
            ?: return CommandResult.Error(MCPError.ACCESSIBILITY_NOT_ENABLED)

        val rootNode = service.rootInActiveWindow
            ?: return CommandResult.Error(
                MCPError.EXECUTION_FAILED,
                "Cannot access current window"
            )

        val direction = params["direction"]?.jsonPrimitive?.content ?: "down"

        // Find scrollable node
        val scrollableNode = rootNode.findScrollable()
            ?: return CommandResult.Error(
                MCPError.ELEMENT_NOT_FOUND,
                "No scrollable element found on screen"
            )

        val scrollAction = when (direction.lowercase()) {
            "down" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            "up" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            "right" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            "left" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            else -> return CommandResult.Error(
                MCPError.INVALID_PARAMS,
                "Invalid direction '$direction'. Use: up, down, left, right"
            )
        }

        val scrolled = scrollableNode.performAction(scrollAction)

        return if (scrolled) {
            CommandResult.Success(mapOf(
                "scrolled" to JsonPrimitive(true),
                "direction" to JsonPrimitive(direction)
            ))
        } else {
            CommandResult.Error(
                MCPError.EXECUTION_FAILED,
                "Scroll action failed — may be at the end of content"
            )
        }
    }
}
