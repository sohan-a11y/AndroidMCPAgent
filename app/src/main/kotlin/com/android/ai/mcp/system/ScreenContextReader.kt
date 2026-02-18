package com.android.ai.mcp.system

import android.view.accessibility.AccessibilityNodeInfo

class ScreenContextReader {

    fun currentPackageName(): String? {
        val service = MCPAccessibilityService.instance ?: return null
        val rootNode = service.rootInActiveWindow ?: return null
        return rootNode.packageName?.toString()
    }

    fun buildContextForPlanner(maxChars: Int = 4000): String {
        val service = MCPAccessibilityService.instance
            ?: return "Accessibility service not enabled"

        val rootNode = service.rootInActiveWindow
            ?: return "No active window available"

        val packageName = rootNode.packageName?.toString() ?: "unknown"
        val texts = readVisibleText(maxItems = 200)

        val context = buildString {
            appendLine("package_name: $packageName")
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
}
