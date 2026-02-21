package com.android.ai.mcp.execution

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.android.ai.mcp.domain.PlanStep
import com.android.ai.mcp.storage.automation.CredentialVaultRepository
import com.android.ai.mcp.system.MCPAccessibilityService
import com.android.ai.mcp.system.ScreenContextReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive

class UiActionPerformer(
    private val context: Context,
    private val screenContextReader: ScreenContextReader,
    private val credentialVaultRepository: CredentialVaultRepository
) {

    suspend fun execute(step: PlanStep): ActionExecutionResult {
        return withContext(Dispatchers.Default) {
            when (step.action) {
                ActionValidator.ACTION_OPEN_APP -> executeOpenApp(step)
                ActionValidator.ACTION_CLICK_BY_TEXT -> executeClickByText(step)
                ActionValidator.ACTION_INPUT_TEXT -> executeInputText(step)
                ActionValidator.ACTION_SCROLL -> executeScroll(step)
                ActionValidator.ACTION_BACK -> executeBack()
                ActionValidator.ACTION_HOME -> executeHome()
                ActionValidator.ACTION_GET_SCREEN_TEXT -> executeGetScreenText()
                ActionValidator.ACTION_FILL_SAVED_PASSWORD -> executeFillSavedPassword(step)
                ActionValidator.ACTION_WAIT_FOR_TEXT -> executeWaitForText(step)
                ActionValidator.ACTION_LONG_PRESS -> executeLongPress(step)
                else -> ActionExecutionResult(success = false, errorMessage = "Unsupported action: ${step.action}")
            }
        }
    }

    private fun executeOpenApp(step: PlanStep): ActionExecutionResult {
        val requestedPackageName = step.params.stringParam("package_name")
        val requestedAppName = step.params.stringParam("app_name")
        val packageName = resolveLaunchablePackageName(
            requestedPackageName = requestedPackageName,
            requestedAppName = requestedAppName
        ) ?: return ActionExecutionResult(
            success = false,
            errorMessage = buildOpenAppErrorMessage(
                requestedPackageName = requestedPackageName,
                requestedAppName = requestedAppName
            )
        )

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return ActionExecutionResult(
                success = false,
                errorMessage = "No launch intent found for package: $packageName"
            )

        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)

        val payload = buildJsonObject {
            put("launched", JsonPrimitive(true))
            put("package_name", JsonPrimitive(packageName))
        }

        return ActionExecutionResult(success = true, resultJson = payload.toString())
    }

    private suspend fun executeFillSavedPassword(step: PlanStep): ActionExecutionResult {
        if (!credentialVaultRepository.isSessionUnlocked()) {
            return ActionExecutionResult(
                success = false,
                errorMessage = "Credential vault is locked. Unlock vault before execution."
            )
        }

        val rootNode = rootNodeOrNull()
            ?: return ActionExecutionResult(
                success = false,
                errorMessage = "Accessibility service or active window unavailable"
            )

        try {
            val appPackage = rootNode.packageName?.toString()
                ?: return ActionExecutionResult(
                    success = false,
                    errorMessage = "Could not detect active package for credential lookup"
                )

            val fieldHint = step.params.stringParam("field_hint")
            val accountHint = step.params.stringParam("account_hint")
            val credential = credentialVaultRepository.resolveCredential(
                appPackage = appPackage,
                fieldHint = fieldHint,
                accountHint = accountHint
            ) ?: return ActionExecutionResult(
                success = false,
                errorMessage = "No saved credential matched package '$appPackage'"
            )

            val targetNode = if (!fieldHint.isNullOrBlank()) {
                val candidates = mutableListOf<AccessibilityNodeInfo>()
                findByText(rootNode, fieldHint.lowercase(), candidates)
                candidates.firstOrNull { it.isEditable || it.className?.toString() == "android.widget.EditText" }
            } else {
                collectEditableNodes(rootNode).firstOrNull()
            } ?: return ActionExecutionResult(
                success = false,
                errorMessage = "No editable field found for saved credential input"
            )

            targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)

            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    credential.password
                )
            }
            val setTextResult = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            if (!setTextResult) {
                return ActionExecutionResult(
                    success = false,
                    errorMessage = "Failed to fill saved password"
                )
            }

            val payload = buildJsonObject {
                put("credential_filled", JsonPrimitive(true))
                put("account_label", JsonPrimitive(credential.accountLabel))
                put("package_name", JsonPrimitive(credential.appPackage))
            }

            return ActionExecutionResult(success = true, resultJson = payload.toString())
        } finally {
            rootNode.recycle()
        }
    }

    private fun executeClickByText(step: PlanStep): ActionExecutionResult {
        val text = step.params.stringParam("text")
            ?: return ActionExecutionResult(
                success = false,
                errorMessage = "click_by_text missing text"
            )

        val rootNode = rootNodeOrNull()
            ?: return ActionExecutionResult(
                success = false,
                errorMessage = "Accessibility service or active window unavailable"
            )

        try {
            val matchingNodes = mutableListOf<AccessibilityNodeInfo>()
            findByText(rootNode, text.lowercase(), matchingNodes)

            if (matchingNodes.isEmpty()) {
                return ActionExecutionResult(
                    success = false,
                    errorMessage = "No element matched text '$text'"
                )
            }

            val targetNode = matchingNodes.first()
            var clickableNode: AccessibilityNodeInfo? = targetNode
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }

            val clicked = (clickableNode ?: targetNode).performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (!clicked) {
                return ActionExecutionResult(
                    success = false,
                    errorMessage = "Click action returned false"
                )
            }

            val payload = buildJsonObject {
                put("clicked", JsonPrimitive(true))
                put("matched_text", JsonPrimitive(text))
                put("matches_found", JsonPrimitive(matchingNodes.size))
            }

            return ActionExecutionResult(success = true, resultJson = payload.toString())
        } finally {
            rootNode.recycle()
        }
    }

    private fun executeInputText(step: PlanStep): ActionExecutionResult {
        val text = step.params.stringParam("text")
            ?: return ActionExecutionResult(
                success = false,
                errorMessage = "input_text missing text"
            )

        val rootNode = rootNodeOrNull()
            ?: return ActionExecutionResult(
                success = false,
                errorMessage = "Accessibility service or active window unavailable"
            )

        try {
            val fieldHint = step.params.stringParam("field_hint")
            val targetNode = if (fieldHint != null) {
                val candidates = mutableListOf<AccessibilityNodeInfo>()
                findByText(rootNode, fieldHint.lowercase(), candidates)
                candidates.firstOrNull { it.isEditable || it.className?.toString() == "android.widget.EditText" }
            } else {
                collectEditableNodes(rootNode).firstOrNull()
            }

            if (targetNode == null) {
                return ActionExecutionResult(
                    success = false,
                    errorMessage = "No editable field found"
                )
            }

            targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)

            val arguments = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }

            val setTextResult = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            if (!setTextResult) {
                return ActionExecutionResult(
                    success = false,
                    errorMessage = "Failed to set text"
                )
            }

            val payload = buildJsonObject {
                put("input_set", JsonPrimitive(true))
                put("text_length", JsonPrimitive(text.length))
            }

            return ActionExecutionResult(success = true, resultJson = payload.toString())
        } finally {
            rootNode.recycle()
        }
    }

    private fun executeScroll(step: PlanStep): ActionExecutionResult {
        val direction = step.params.stringParam("direction")?.lowercase() ?: "down"
        val rootNode = rootNodeOrNull()
            ?: return ActionExecutionResult(
                success = false,
                errorMessage = "Accessibility service or active window unavailable"
            )

        try {
            val scrollableNode = findScrollable(rootNode)
                ?: return ActionExecutionResult(
                    success = false,
                    errorMessage = "No scrollable node found"
                )

            val action = when (direction) {
                "down", "right" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                "up", "left" -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
                else -> {
                    return ActionExecutionResult(
                        success = false,
                        errorMessage = "Invalid scroll direction: $direction"
                    )
                }
            }

            val scrolled = scrollableNode.performAction(action)
            if (!scrolled) {
                return ActionExecutionResult(
                    success = false,
                    errorMessage = "Scroll action returned false"
                )
            }

            val payload = buildJsonObject {
                put("scrolled", JsonPrimitive(true))
                put("direction", JsonPrimitive(direction))
            }

            return ActionExecutionResult(success = true, resultJson = payload.toString())
        } finally {
            rootNode.recycle()
        }
    }

    private fun executeBack(): ActionExecutionResult {
        val service = MCPAccessibilityService.instance
            ?: return ActionExecutionResult(success = false, errorMessage = "Accessibility service not enabled")

        val result = service.pressBack()
        return ActionExecutionResult(
            success = result,
            resultJson = if (result) "{\"back\":true}" else null,
            errorMessage = if (result) null else "Back action failed"
        )
    }

    private fun executeHome(): ActionExecutionResult {
        val service = MCPAccessibilityService.instance
            ?: return ActionExecutionResult(success = false, errorMessage = "Accessibility service not enabled")

        val result = service.pressHome()
        return ActionExecutionResult(
            success = result,
            resultJson = if (result) "{\"home\":true}" else null,
            errorMessage = if (result) null else "Home action failed"
        )
    }

    private fun executeGetScreenText(): ActionExecutionResult {
        val texts = screenContextReader.readVisibleText()
        val payload = buildJsonObject {
            put("text_count", JsonPrimitive(texts.size))
            put("texts", buildJsonArray {
                texts.forEach { add(JsonPrimitive(it)) }
            })
        }
        return ActionExecutionResult(success = true, resultJson = payload.toString())
    }

    private suspend fun executeWaitForText(step: PlanStep): ActionExecutionResult {
        val text = step.params.stringParam("text")
            ?: return ActionExecutionResult(
                success = false,
                errorMessage = "wait_for_text missing text"
            )

        val timeoutMs = step.params.stringParam("timeout_ms")?.toLongOrNull() ?: 10_000L
        val pollIntervalMs = 500L
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val visibleTexts = screenContextReader.readVisibleText()
            val lowerQuery = text.lowercase()
            if (visibleTexts.any { it.lowercase().contains(lowerQuery) }) {
                val payload = buildJsonObject {
                    put("text_found", JsonPrimitive(true))
                    put("matched_text", JsonPrimitive(text))
                    put("waited_ms", JsonPrimitive(System.currentTimeMillis() - startTime))
                }
                return ActionExecutionResult(success = true, resultJson = payload.toString())
            }
            delay(pollIntervalMs)
        }

        return ActionExecutionResult(
            success = false,
            errorMessage = "Text '$text' not found within ${timeoutMs}ms"
        )
    }

    private fun executeLongPress(step: PlanStep): ActionExecutionResult {
        val text = step.params.stringParam("text")
            ?: return ActionExecutionResult(
                success = false,
                errorMessage = "long_press missing text"
            )

        val rootNode = rootNodeOrNull()
            ?: return ActionExecutionResult(
                success = false,
                errorMessage = "Accessibility service or active window unavailable"
            )

        try {
            val matchingNodes = mutableListOf<AccessibilityNodeInfo>()
            findByText(rootNode, text.lowercase(), matchingNodes)

            if (matchingNodes.isEmpty()) {
                return ActionExecutionResult(
                    success = false,
                    errorMessage = "No element matched text '$text'"
                )
            }

            val targetNode = matchingNodes.first()
            var longClickableNode: AccessibilityNodeInfo? = targetNode
            while (longClickableNode != null && !longClickableNode.isLongClickable) {
                longClickableNode = longClickableNode.parent
            }

            val pressed = (longClickableNode ?: targetNode)
                .performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)

            if (!pressed) {
                return ActionExecutionResult(
                    success = false,
                    errorMessage = "Long press action returned false"
                )
            }

            val payload = buildJsonObject {
                put("long_pressed", JsonPrimitive(true))
                put("matched_text", JsonPrimitive(text))
            }

            return ActionExecutionResult(success = true, resultJson = payload.toString())
        } finally {
            rootNode.recycle()
        }
    }

    private fun rootNodeOrNull(): AccessibilityNodeInfo? {
        val service = MCPAccessibilityService.instance ?: return null
        return service.rootInActiveWindow
    }

    private fun findByText(
        node: AccessibilityNodeInfo,
        query: String,
        output: MutableList<AccessibilityNodeInfo>
    ) {
        val nodeText = node.text?.toString()?.lowercase().orEmpty()
        val nodeDescription = node.contentDescription?.toString()?.lowercase().orEmpty()

        if (nodeText.contains(query) || nodeDescription.contains(query)) {
            output.add(node)
        }

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            findByText(child, query, output)
        }
    }

    private fun collectEditableNodes(node: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val output = mutableListOf<AccessibilityNodeInfo>()
        collectEditableNodesRecursive(node, output)
        return output
    }

    private fun collectEditableNodesRecursive(
        node: AccessibilityNodeInfo,
        output: MutableList<AccessibilityNodeInfo>
    ) {
        if (node.isEditable || node.className?.toString() == "android.widget.EditText") {
            output.add(node)
        }

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            collectEditableNodesRecursive(child, output)
        }
    }

    private fun findScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) {
            return node
        }

        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            val childMatch = findScrollable(child)
            if (childMatch != null) {
                return childMatch
            }
        }

        return null
    }

    private fun Map<String, JsonElement>.stringParam(key: String): String? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        return primitive.jsonPrimitive.content.trim().takeIf { it.isNotEmpty() }
    }

    private fun resolveLaunchablePackageName(
        requestedPackageName: String?,
        requestedAppName: String?
    ): String? {
        val packageManager = context.packageManager
        val requestedPackage = requestedPackageName?.trim()?.takeIf { it.isNotEmpty() }
        if (requestedPackage != null && packageManager.getLaunchIntentForPackage(requestedPackage) != null) {
            return requestedPackage
        }

        val apps = readLaunchableApps()
        if (apps.isEmpty()) return null

        val queries = linkedSetOf<String>()
        requestedPackage?.let { queries.add(it.lowercase()) }
        requestedAppName?.trim()?.takeIf { it.isNotEmpty() }?.let {
            queries.add(it.lowercase())
        }
        inferKeywordFromPackageName(requestedPackage)?.let { queries.add(it) }
        if (queries.isEmpty()) return null

        var bestMatch: LaunchableAppInfo? = null
        var bestScore = Int.MIN_VALUE
        for (app in apps) {
            val score = queries.maxOf { query -> scoreAppMatch(query, app) }
            if (score > bestScore) {
                bestScore = score
                bestMatch = app
            }
        }

        return if (bestScore >= 40) bestMatch?.packageName else null
    }

    private fun readLaunchableApps(): List<LaunchableAppInfo> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = packageManager.queryIntentActivities(launcherIntent, 0)
        return resolved
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName?.trim().orEmpty()
                if (packageName.isEmpty()) return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager)?.toString()?.trim().orEmpty()
                LaunchableAppInfo(
                    normalizedLabel = label.ifEmpty { packageName }.lowercase(),
                    packageName = packageName,
                    normalizedPackageName = packageName.lowercase()
                )
            }
            .distinctBy { it.packageName }
    }

    private fun scoreAppMatch(query: String, app: LaunchableAppInfo): Int {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isEmpty()) return Int.MIN_VALUE

        if (app.normalizedPackageName == normalizedQuery) return 120
        if (app.normalizedLabel == normalizedQuery) return 110

        val substringScore = when {
            app.normalizedPackageName.startsWith(normalizedQuery) -> 95
            app.normalizedLabel.startsWith(normalizedQuery) -> 85
            app.normalizedPackageName.contains(normalizedQuery) -> 75
            app.normalizedLabel.contains(normalizedQuery) -> 70
            else -> 0
        }

        val tokens = normalizedQuery.split(Regex("[^a-z0-9]+")).filter { it.length >= 2 }
        var tokenScore = 0
        if (tokens.isNotEmpty()) {
            val tokenHits = tokens.count { token ->
                app.normalizedPackageName.contains(token) || app.normalizedLabel.contains(token)
            }
            tokenScore = tokenHits * 12
            if (tokenHits == tokens.size) tokenScore += 20
        }

        return maxOf(substringScore, tokenScore)
    }

    private fun inferKeywordFromPackageName(packageName: String?): String? {
        val normalized = packageName?.trim()?.lowercase().orEmpty()
        if (normalized.isEmpty()) return null

        val parts = normalized.split(".").filter { it.isNotBlank() }
        return parts.lastOrNull { part ->
            part.length >= 4 && part !in setOf("com", "android", "app")
        }
    }

    private fun buildOpenAppErrorMessage(
        requestedPackageName: String?,
        requestedAppName: String?
    ): String {
        if (requestedPackageName.isNullOrBlank() && requestedAppName.isNullOrBlank()) {
            return "open_app requires package_name or app_name"
        }
        return buildString {
            append("Could not resolve a launchable app")
            requestedPackageName?.takeIf { it.isNotBlank() }?.let { append(" for package '$it'") }
            requestedAppName?.takeIf { it.isNotBlank() }?.let {
                if (requestedPackageName.isNullOrBlank()) {
                    append(" for app '$it'")
                } else {
                    append(" or app '$it'")
                }
            }
        }
    }

    private data class LaunchableAppInfo(
        val normalizedLabel: String,
        val packageName: String,
        val normalizedPackageName: String
    )
}
