package com.android.mcp.agent.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Core AccessibilityService that provides UI interaction capabilities.
 *
 * This service is the "hands" of the MCP agent. It can:
 * - Read the UI tree (all visible elements)
 * - Perform clicks, scrolls, and text input
 * - Execute global actions (back, home, recents)
 *
 * Uses a singleton pattern so command handlers can access it.
 * The service lifecycle is managed by Android — enabled/disabled in Settings.
 */
class MCPAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "MCPAccessibility"

        /**
         * Singleton reference to the running service instance.
         * Null when the service is not active.
         */
        @Volatile
        var instance: MCPAccessibilityService? = null
            private set

        /**
         * Check if the accessibility service is currently running.
         */
        val isRunning: Boolean
            get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        // Configure the service
        serviceInfo = serviceInfo.apply {
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = flags or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
            notificationTimeout = 100
        }

        Log.i(TAG, "Accessibility service connected and configured")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to react to events for MVP.
        // Future: could track window changes, notifications, etc.
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        instance = null
        Log.i(TAG, "Accessibility service destroyed")
        super.onDestroy()
    }

    // ── Global Actions ──────────────────────────────────────────────

    /**
     * Press the Back button.
     */
    fun pressBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Press the Home button.
     */
    fun pressHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Open Recent Apps.
     */
    fun openRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    /**
     * Open the notification shade.
     */
    fun openNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    /**
     * Open Quick Settings.
     */
    fun openQuickSettings(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }
}
