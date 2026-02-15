package com.android.mcp.agent.permissions

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences-backed storage for MCP permission toggles.
 *
 * Persists user's permission choices across app restarts.
 */
class PermissionStore(context: Context) {

    companion object {
        private const val PREFS_NAME = "mcp_permissions"
        private const val KEY_PREFIX = "perm_"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Check if a permission is enabled.
     * Falls back to the default value if not previously set.
     */
    fun isPermissionEnabled(permission: String): Boolean {
        val defaultValue = PermissionManager.ALL_PERMISSIONS[permission] ?: false
        return prefs.getBoolean("$KEY_PREFIX$permission", defaultValue)
    }

    /**
     * Set a permission enabled/disabled.
     */
    fun setPermissionEnabled(permission: String, enabled: Boolean) {
        prefs.edit().putBoolean("$KEY_PREFIX$permission", enabled).apply()
    }

    /**
     * Reset all permissions to defaults.
     */
    fun resetToDefaults() {
        val editor = prefs.edit()
        PermissionManager.ALL_PERMISSIONS.forEach { (key, default) ->
            editor.putBoolean("$KEY_PREFIX$key", default)
        }
        editor.apply()
    }
}
