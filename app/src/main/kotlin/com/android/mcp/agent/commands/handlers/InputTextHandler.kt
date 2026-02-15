package com.android.mcp.agent.commands.handlers

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.android.mcp.agent.accessibility.MCPAccessibilityService
import com.android.mcp.agent.accessibility.NodeExtensions.findByText
import com.android.mcp.agent.accessibility.NodeExtensions.findByViewId
import com.android.mcp.agent.accessibility.NodeExtensions.findInputFields
import com.android.mcp.agent.commands.CommandHandler
import com.android.mcp.agent.commands.CommandResult
import com.android.mcp.agent.permissions.PermissionManager
import com.android.mcp.agent.protocol.MCPError
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Inputs text into a focused or specified text field.
 *
 * Params:
 * - "text" (required): Text to input
 * - "field_hint" (optional): Hint text to find the right field
 * - "field_id" (optional): View ID of the target field
 * - "index" (optional): Index of input field to use (default: 0)
 * - "append" (optional): If true, append to existing text (default: false)
 */
class InputTextHandler : CommandHandler {

    override val action = "input_text"
    override val requiredPermission = PermissionManager.PERM_TEXT_INPUT

    override suspend fun execute(params: Map<String, JsonElement>): CommandResult {
        val service = MCPAccessibilityService.instance
            ?: return CommandResult.Error(MCPError.ACCESSIBILITY_NOT_ENABLED)

        val rootNode = service.rootInActiveWindow
            ?: return CommandResult.Error(
                MCPError.EXECUTION_FAILED,
                "Cannot access current window"
            )

        val text = params["text"]?.jsonPrimitive?.content
            ?: return CommandResult.Error(
                MCPError.MISSING_PARAMS,
                "Required param 'text' is missing"
            )

        val fieldHint = params["field_hint"]?.jsonPrimitive?.content
        val fieldId = params["field_id"]?.jsonPrimitive?.content
        val index = params["index"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        val append = params["append"]?.jsonPrimitive?.content?.toBoolean() ?: false

        // Find target input field
        val targetNode: AccessibilityNodeInfo? = when {
            fieldId != null -> rootNode.findByViewId(fieldId).firstOrNull()
            fieldHint != null -> rootNode.findByText(fieldHint).firstOrNull()
            else -> {
                val inputs = rootNode.findInputFields()
                inputs.getOrNull(index)
            }
        }

        if (targetNode == null) {
            return CommandResult.Error(
                MCPError.ELEMENT_NOT_FOUND,
                "No input field found"
            )
        }

        // Focus the field
        targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)

        // Set text
        val existingText = if (append) targetNode.text?.toString() ?: "" else ""
        val newText = existingText + text

        val arguments = Bundle().apply {
            putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                newText
            )
        }
        val result = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)

        return if (result) {
            CommandResult.Success(mapOf(
                "input_set" to JsonPrimitive(true),
                "text_length" to JsonPrimitive(newText.length)
            ))
        } else {
            CommandResult.Error(
                MCPError.EXECUTION_FAILED,
                "Failed to set text on the input field"
            )
        }
    }
}
