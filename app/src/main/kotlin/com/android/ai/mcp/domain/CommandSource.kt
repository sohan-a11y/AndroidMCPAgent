package com.android.ai.mcp.domain

enum class CommandSource(val value: String) {
    MANUAL("manual"),
    VOICE("voice"),
    TEMPLATE("template");

    companion object {
        fun fromValue(value: String?): CommandSource {
            return entries.firstOrNull { it.value == value } ?: MANUAL
        }
    }
}
