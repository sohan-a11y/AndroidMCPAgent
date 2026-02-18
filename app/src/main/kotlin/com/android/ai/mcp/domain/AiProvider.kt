package com.android.ai.mcp.domain

enum class AiProvider(val value: String) {
    OPENROUTER("openrouter"),
    NVIDIA("nvidia");

    companion object {
        fun fromValue(value: String?): AiProvider {
            return entries.firstOrNull { it.value == value } ?: OPENROUTER
        }
    }
}
