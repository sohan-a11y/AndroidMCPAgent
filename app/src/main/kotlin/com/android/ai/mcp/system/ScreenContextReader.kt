package com.android.ai.mcp.system

import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo

class ScreenContextReader(
    private val context: Context
) {

    fun currentPackageName(): String? {
        val service = MCPAccessibilityService.instance ?: return null
        val rootNode = service.rootInActiveWindow ?: return null
        return rootNode.packageName?.toString()
    }

    fun buildContextForPlanner(maxChars: Int = 20_000): String {
        val service = MCPAccessibilityService.instance
        val rootNode = service?.rootInActiveWindow
        val packageName = rootNode?.packageName?.toString() ?: "unknown"
        val texts = if (rootNode != null) readVisibleText(maxItems = 200) else emptyList()
        val launchableApps = readLaunchableApps(maxItems = 600)

        val context = buildString {
            appendLine("accessibility_active_window_available: ${rootNode != null}")
            appendLine("package_name: $packageName")
            appendLine("launchable_apps (label | package_name):")
            if (launchableApps.isEmpty()) {
                appendLine("- (none)")
            } else {
                launchableApps.forEach { app ->
                    appendLine("- ${app.label} | ${app.packageName}")
                }
            }
            appendLine("visible_text:")
            if (texts.isEmpty()) {
                appendLine("- (none)")
            } else {
                texts.forEach { appendLine("- $it") }
            }
        }

        return context.take(maxChars)
    }

    fun readVisibleText(maxItems: Int = 300): List<String> {
        val service = MCPAccessibilityService.instance ?: return emptyList()
        val rootNode = service.rootInActiveWindow ?: return emptyList()

        val output = LinkedHashSet<String>()
        collectText(rootNode, output, maxItems)
        return output.toList()
    }

    private fun readLaunchableApps(maxItems: Int): List<LaunchableApp> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = packageManager.queryIntentActivities(launcherIntent, 0)
        return resolved
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName?.trim().orEmpty()
                if (packageName.isEmpty()) return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                LaunchableApp(
                    label = if (label.isEmpty()) packageName else label,
                    packageName = packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy({ it.label.lowercase() }, { it.packageName.lowercase() }))
            .take(maxItems)
    }

    private fun collectText(
        node: AccessibilityNodeInfo,
        output: LinkedHashSet<String>,
        maxItems: Int
    ) {
        if (output.size >= maxItems) return

        node.text?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { output.add(it) }
        node.contentDescription?.toString()?.trim()?.takeIf { it.isNotEmpty() }?.let { output.add(it) }

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            collectText(child, output, maxItems)
            if (output.size >= maxItems) return
        }
    }

    private data class LaunchableApp(
        val label: String,
        val packageName: String
    )
}
