package com.android.mcp.agent.permissions

/**
 * Manages per-action permission checks.
 *
 * Each command handler declares which permission it requires.
 * The user toggles these on/off from the Dashboard UI.
 * Permissions are checked by [CommandRouter] before dispatching.
 */
class PermissionManager(
    private val store: PermissionStore
) {
    companion object {
        // Permission categories
        const val PERM_CLICKS = "clicks"
        const val PERM_TEXT_INPUT = "text_input"
        const val PERM_SCREENSHOTS = "screenshots"
        const val PERM_READ_SCREEN = "read_screen"
        const val PERM_APP_LAUNCH = "app_launch"
        const val PERM_DEVICE_INFO = "device_info"

        /** All permission keys with default state (true = allowed by default). */
        val ALL_PERMISSIONS = mapOf(
            PERM_CLICKS to true,
            PERM_TEXT_INPUT to true,
            PERM_SCREENSHOTS to false,   // Off by default — requires Phase 2
            PERM_READ_SCREEN to true,
            PERM_APP_LAUNCH to true,
            PERM_DEVICE_INFO to true
        )

        /** Human-readable labels for each permission. */
        val PERMISSION_LABELS = mapOf(
            PERM_CLICKS to "Allow UI Clicks",
            PERM_TEXT_INPUT to "Allow Text Input",
            PERM_SCREENSHOTS to "Allow Screenshots",
            PERM_READ_SCREEN to "Allow Screen Reading",
            PERM_APP_LAUNCH to "Allow App Launching",
            PERM_DEVICE_INFO to "Allow Device Info"
        )
    }

    /**
     * Check if an action permission is currently allowed.
     */
    fun isAllowed(permission: String): Boolean {
        return store.isPermissionEnabled(permission)
    }

    /**
     * Toggle a permission on/off.
     */
    fun setPermission(permission: String, enabled: Boolean) {
        store.setPermissionEnabled(permission, enabled)
    }

    /**
     * Get all permissions with their current state.
     */
    fun getAllPermissions(): Map<String, Boolean> {
        return ALL_PERMISSIONS.keys.associateWith { isAllowed(it) }
    }
}
