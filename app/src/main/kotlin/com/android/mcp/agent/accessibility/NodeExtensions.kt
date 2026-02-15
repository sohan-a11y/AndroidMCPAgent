package com.android.mcp.agent.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.serialization.json.*

/**
 * Extension functions for AccessibilityNodeInfo traversal and querying.
 *
 * These are the core utilities that command handlers use to interact with the UI tree.
 */
object NodeExtensions {

    /**
     * Find all nodes whose text contains the given string (case-insensitive).
     */
    fun AccessibilityNodeInfo.findByText(query: String): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        findByTextRecursive(this, query.lowercase(), results)
        return results
    }

    private fun findByTextRecursive(
        node: AccessibilityNodeInfo,
        query: String,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        val nodeText = node.text?.toString()?.lowercase() ?: ""
        val nodeDesc = node.contentDescription?.toString()?.lowercase() ?: ""

        if (nodeText.contains(query) || nodeDesc.contains(query)) {
            results.add(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findByTextRecursive(child, query, results)
        }
    }

    /**
     * Find all nodes with the given view resource ID.
     * ID format: "com.package:id/view_name"
     */
    fun AccessibilityNodeInfo.findByViewId(viewId: String): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        findByViewIdRecursive(this, viewId, results)
        return results
    }

    private fun findByViewIdRecursive(
        node: AccessibilityNodeInfo,
        viewId: String,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.viewIdResourceName == viewId) {
            results.add(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findByViewIdRecursive(child, viewId, results)
        }
    }

    /**
     * Find all editable/input fields on screen.
     */
    fun AccessibilityNodeInfo.findInputFields(): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        findInputFieldsRecursive(this, results)
        return results
    }

    private fun findInputFieldsRecursive(
        node: AccessibilityNodeInfo,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.isEditable || node.className?.toString() == "android.widget.EditText") {
            results.add(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findInputFieldsRecursive(child, results)
        }
    }

    /**
     * Find the first scrollable container on screen.
     */
    fun AccessibilityNodeInfo.findScrollable(): AccessibilityNodeInfo? {
        if (this.isScrollable) return this

        for (i in 0 until this.childCount) {
            val child = this.getChild(i) ?: continue
            val found = child.findScrollable()
            if (found != null) return found
        }

        return null
    }

    /**
     * Get a structured JSON representation of the full UI tree.
     * Useful for AI agents to understand screen layout.
     */
    fun AccessibilityNodeInfo.getFullTree(maxDepth: Int = 20): JsonElement {
        return buildTreeNode(this, 0, maxDepth)
    }

    private fun buildTreeNode(
        node: AccessibilityNodeInfo,
        depth: Int,
        maxDepth: Int
    ): JsonElement {
        if (depth > maxDepth) return JsonPrimitive("...truncated")

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val props = mutableMapOf<String, JsonElement>()

        // Basic info
        node.className?.toString()?.let { props["class"] = JsonPrimitive(it) }
        node.text?.toString()?.let { props["text"] = JsonPrimitive(it) }
        node.contentDescription?.toString()?.let { props["description"] = JsonPrimitive(it) }
        node.viewIdResourceName?.let { props["id"] = JsonPrimitive(it) }

        // State
        if (node.isClickable) props["clickable"] = JsonPrimitive(true)
        if (node.isScrollable) props["scrollable"] = JsonPrimitive(true)
        if (node.isEditable) props["editable"] = JsonPrimitive(true)
        if (node.isChecked) props["checked"] = JsonPrimitive(true)
        if (node.isSelected) props["selected"] = JsonPrimitive(true)
        if (node.isFocused) props["focused"] = JsonPrimitive(true)

        // Bounds
        props["bounds"] = JsonObject(mapOf(
            "left" to JsonPrimitive(bounds.left),
            "top" to JsonPrimitive(bounds.top),
            "right" to JsonPrimitive(bounds.right),
            "bottom" to JsonPrimitive(bounds.bottom)
        ))

        // Children
        if (node.childCount > 0) {
            val children = mutableListOf<JsonElement>()
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                children.add(buildTreeNode(child, depth + 1, maxDepth))
            }
            if (children.isNotEmpty()) {
                props["children"] = JsonArray(children)
            }
        }

        return JsonObject(props)
    }
}
