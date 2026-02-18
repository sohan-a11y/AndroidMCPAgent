package com.android.ai.mcp.domain

enum class WakeScope(val value: String) {
    ALWAYS_ON_FOREGROUND("always_on_foreground"),
    APP_OPEN_ONLY("app_open_only"),
    MANUAL_START("manual_start");

    companion object {
        fun fromValue(value: String?): WakeScope {
            return entries.firstOrNull { it.value == value } ?: ALWAYS_ON_FOREGROUND
        }
    }
}
