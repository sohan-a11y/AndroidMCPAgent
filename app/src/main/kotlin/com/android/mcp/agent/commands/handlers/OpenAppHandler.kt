package com.android.mcp.agent.commands.handlers

import android.content.Context
import android.content.Intent
import com.android.mcp.agent.commands.CommandHandler
import com.android.mcp.agent.commands.CommandResult
import com.android.mcp.agent.permissions.PermissionManager
import com.android.mcp.agent.protocol.MCPError
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Launches an app by package name.
 *
 * Params:
 * - "package_name" (required): The package name (e.g., "com.whatsapp")
 *
 * Result:
 * - "launched": true
 * - "package_name": the launched package
 */
class OpenAppHandler(
    private val context: Context
) : CommandHandler {

    override val action = "open_app"
    override val requiredPermission = PermissionManager.PERM_APP_LAUNCH

    override suspend fun execute(params: Map<String, JsonElement>): CommandResult {
        val packageName = params["package_name"]?.jsonPrimitive?.content
            ?: return CommandResult.Error(
                MCPError.MISSING_PARAMS,
                "Required param 'package_name' is missing"
            )

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return CommandResult.Error(
                MCPError.APP_NOT_FOUND,
                "No launch intent found for package: $packageName"
            )

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)

        return CommandResult.Success(mapOf(
            "launched" to JsonPrimitive(true),
            "package_name" to JsonPrimitive(packageName)
        ))
    }
}
