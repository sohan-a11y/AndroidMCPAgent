package com.android.mcp.agent.commands.handlers

import android.view.accessibility.AccessibilityNodeInfo
import com.android.mcp.agent.accessibility.MCPAccessibilityService
import com.android.mcp.agent.accessibility.NodeExtensions.findByText
import com.android.mcp.agent.accessibility.NodeExtensions.findByViewId
import com.android.mcp.agent.commands.CommandHandler
import com.android.mcp.agent.commands.CommandResult
import com.android.mcp.agent.permissions.PermissionManager
import com.android.mcp.agent.protocol.MCPError
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Clicks a UI element found by text content or view ID.
 *
 * Params:
 * - "text" (optional): Text content to search for
 * - "view_id" (optional): View resource ID (e.g., "com.app:id/button")
 * - "index" (optional): If multiple matches, which one to click (default: 0)
 *
 * At least one of "text" or "view_id" must be provided.
 */
class ClickHandler : CommandHandler {

    override val action = "click"
    override val requiredPermission = PermissionManager.PERM_CLICKS

    override suspend fun execute(params: Map<String, JsonElement>): CommandResult {
        val service = MCPAccessibilityService.instance
            ?: return CommandResult.Error(MCPError.ACCESSIBILITY_NOT_ENABLED)

        val rootNode = service.rootInActiveWindow
            ?: return CommandResult.Error(
                MCPError.EXECUTION_FAILED,
                "Cannot access current window"
            )

        val text = params["text"]?.jsonPrimitive?.content
        val viewId = params["view_id"]?.jsonPrimitive?.content
        val index = params["index"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0

        if (text == null && viewId == null) {
            return CommandResult.Error(
                MCPError.MISSING_PARAMS,
                "At least one of 'text' or 'view_id' must be provided"
            )
        }

        val matchingNodes = mutableListOf<AccessibilityNodeInfo>()

        if (text != null) {
            matchingNodes.addAll(rootNode.findByText(text))
        }
        if (viewId != null && matchingNodes.isEmpty()) {
            matchingNodes.addAll(rootNode.findByViewId(viewId))
        }

        if (matchingNodes.isEmpty()) {
            return CommandResult.Error(
                MCPError.ELEMENT_NOT_FOUND,
                "No element found matching text='$text' viewId='$viewId'"
            )
        }

        if (index >= matchingNodes.size) {
            return CommandResult.Error(
                MCPError.INVALID_PARAMS,
                "Index $index out of range. Found ${matchingNodes.size} matching elements."
            )
        }

        val targetNode = matchingNodes[index]

        // Walk up tree to find a clickable ancestor if needed
        var clickableNode: AccessibilityNodeInfo? = targetNode
        while (clickableNode != null && !clickableNode.isClickable) {
            clickableNode = clickableNode.parent
        }

        val nodeToClick = clickableNode ?: targetNode
        val clicked = nodeToClick.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        return if (clicked) {
            CommandResult.Success(mapOf(
                "clicked" to JsonPrimitive(true),
                "element_text" to JsonPrimitive(targetNode.text?.toString() ?: ""),
                "matches_found" to JsonPrimitive(matchingNodes.size)
            ))
        } else {
            CommandResult.Error(
                MCPError.EXECUTION_FAILED,
                "Click action returned false for element"
            )
        }
    }
}
