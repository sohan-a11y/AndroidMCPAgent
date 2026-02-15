package com.android.mcp.agent.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents an incoming command from an AI client.
 *
 * Example:
 * ```json
 * {
 *   "id": "cmd-001",
 *   "action": "click_by_text",
 *   "params": { "text": "Allow" },
 *   "auth_token": "abc123"
 * }
 * ```
 */
@Serializable
data class MCPCommand(
    val id: String,
    val action: String,
    val params: Map<String, JsonElement> = emptyMap(),
    val auth_token: String? = null
) {
    companion object {
        // Core action constants
        const val ACTION_OPEN_APP = "open_app"
        const val ACTION_CLICK = "click"
        const val ACTION_CLICK_BY_TEXT = "click_by_text"
        const val ACTION_CLICK_BY_ID = "click_by_id"
        const val ACTION_INPUT_TEXT = "input_text"
        const val ACTION_SCROLL = "scroll"
        const val ACTION_GET_SCREEN_TEXT = "get_screen_text"
        const val ACTION_SCREENSHOT = "screenshot"
        const val ACTION_GET_DEVICE_STATE = "get_device_state"
        const val ACTION_LIST_APPS = "list_apps"

        // System actions
        const val ACTION_PING = "ping"
        const val ACTION_PAIR = "pair"

        // Global navigation
        const val ACTION_BACK = "back"
        const val ACTION_HOME = "home"
        const val ACTION_RECENTS = "recents"
    }
}
