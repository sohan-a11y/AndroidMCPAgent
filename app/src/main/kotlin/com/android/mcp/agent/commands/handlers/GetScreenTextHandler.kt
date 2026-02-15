package com.android.mcp.agent.commands.handlers

import android.view.accessibility.AccessibilityNodeInfo
import com.android.mcp.agent.accessibility.MCPAccessibilityService
import com.android.mcp.agent.accessibility.NodeExtensions.getFullTree
import com.android.mcp.agent.commands.CommandHandler
import com.android.mcp.agent.commands.CommandResult
import com.android.mcp.agent.permissions.PermissionManager
import com.android.mcp.agent.protocol.MCPError
import kotlinx.serialization.json.*

/**
 * Reads all visible text from the current screen.
 *
 * Params:
 * - "structured" (optional): If true, returns structured UI tree (default: false)
 * - "max_depth" (optional): Max depth for tree traversal (default: 20)
 *
 * Result:
 * - "texts": Array of visible text strings
 * - "tree": (if structured=true) Nested UI tree with text, class, bounds
 * - "package_name": Current foreground app package
 */
class GetScreenTextHandler : CommandHandler {

    override val action = "get_screen_text"
    override val requiredPermission = PermissionManager.PERM_READ_SCREEN

    override suspend fun execute(params: Map<String, JsonElement>): CommandResult {
        val service = MCPAccessibilityService.instance
            ?: return CommandResult.Error(MCPError.ACCESSIBILITY_NOT_ENABLED)

        val rootNode = service.rootInActiveWindow
            ?: return CommandResult.Error(
                MCPError.EXECUTION_FAILED,
                "Cannot access current window"
            )

        val structured = params["structured"]?.jsonPrimitive?.content?.toBoolean() ?: false
        val maxDepth = params["max_depth"]?.jsonPrimitive?.content?.toIntOrNull() ?: 20

        val packageName = rootNode.packageName?.toString() ?: "unknown"

        return if (structured) {
            val tree = rootNode.getFullTree(maxDepth)
            CommandResult.Success(mapOf(
                "tree" to tree,
                "package_name" to JsonPrimitive(packageName)
            ))
        } else {
            val texts = mutableListOf<String>()
            collectTexts(rootNode, texts)
            CommandResult.Success(mapOf(
                "texts" to JsonArray(texts.map { JsonPrimitive(it) }),
                "text_count" to JsonPrimitive(texts.size),
                "package_name" to JsonPrimitive(packageName)
            ))
        }
    }

    private fun collectTexts(node: AccessibilityNodeInfo, texts: MutableList<String>) {
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it) }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectTexts(child, texts)
        }
    }
}
