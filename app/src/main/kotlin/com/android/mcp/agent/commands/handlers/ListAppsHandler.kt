package com.android.mcp.agent.commands.handlers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.android.mcp.agent.commands.CommandHandler
import com.android.mcp.agent.commands.CommandResult
import com.android.mcp.agent.permissions.PermissionManager
import kotlinx.serialization.json.*

/**
 * Lists installed apps on the device.
 *
 * Params:
 * - "launchable_only" (optional): If true, only show apps with a launcher intent (default: true)
 * - "query" (optional): Filter apps by name (case-insensitive substring match)
 *
 * Result:
 * - "apps": Array of { "name", "package_name" }
 * - "count": Number of apps returned
 */
class ListAppsHandler(
    private val context: Context
) : CommandHandler {

    override val action = "list_apps"
    override val requiredPermission = PermissionManager.PERM_DEVICE_INFO

    override suspend fun execute(params: Map<String, JsonElement>): CommandResult {
        val launchableOnly = params["launchable_only"]?.jsonPrimitive?.content?.toBoolean() ?: true
        val query = params["query"]?.jsonPrimitive?.content?.lowercase()

        val pm = context.packageManager

        val apps = if (launchableOnly) {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
                .map { resolveInfo ->
                    val appName = resolveInfo.loadLabel(pm).toString()
                    val packageName = resolveInfo.activityInfo.packageName
                    appName to packageName
                }
        } else {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .map { appInfo ->
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    val packageName = appInfo.packageName
                    appName to packageName
                }
        }

        val filteredApps = if (query != null) {
            apps.filter { (name, pkg) ->
                name.lowercase().contains(query) || pkg.lowercase().contains(query)
            }
        } else {
            apps
        }

        val sortedApps = filteredApps
            .distinctBy { it.second }
            .sortedBy { it.first.lowercase() }

        val appsJson = JsonArray(sortedApps.map { (name, pkg) ->
            JsonObject(mapOf(
                "name" to JsonPrimitive(name),
                "package_name" to JsonPrimitive(pkg)
            ))
        })

        return CommandResult.Success(mapOf(
            "apps" to appsJson,
            "count" to JsonPrimitive(sortedApps.size)
        ))
    }
}
