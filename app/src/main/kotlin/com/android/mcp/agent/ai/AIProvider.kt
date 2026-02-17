package com.android.mcp.agent.ai

/**
 * Sealed class representing supported AI providers for embedded AI integration.
 *
 * Each provider includes:
 * - Display name for UI presentation
 * - Base URL for chat completions endpoint
 * - Default model name (can be overridden by user)
 * - OpenAI compatibility flag (all supported providers are OpenAI-compatible)
 */
sealed class AIProvider(
    val displayName: String,
    val baseUrl: String,
    val defaultModel: String,
    val isOpenAICompatible: Boolean = true
) {
    /**
     * OpenRouter - Multi-model AI gateway
     * Supports many models including Google Gemini
     */
    data object OpenRouter : AIProvider(
        displayName = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1/chat/completions",
        defaultModel = "google/gemini-2.0-flash-001"
    )

    /**
     * NVIDIA NIM - NVIDIA's inference microservices
     * Provides access to optimized NVIDIA models
     */
    data object NVIDIA : AIProvider(
        displayName = "NVIDIA NIM",
        baseUrl = "https://integrate.api.nvidia.com/v1/chat/completions",
        defaultModel = "nvidia/llama-3.1-nemotron-ultra-253b-v1"
    )

    /**
     * Kimi K2.5 - Moonshot AI
     * Chinese AI provider with strong multilingual support
     */
    data object Kimi : AIProvider(
        displayName = "Kimi K2.5 (Moonshot)",
        baseUrl = "https://api.moonshot.cn/v1/chat/completions",
        defaultModel = "moonshot-v1-32k"
    )

    companion object {
        /**
         * Returns all available providers as a list
         */
        fun all(): List<AIProvider> = listOf(OpenRouter, NVIDIA, Kimi)

        /**
         * Returns provider by display name, or null if not found
         */
        fun fromDisplayName(name: String): AIProvider? {
            return all().find { it.displayName == name }
        }
    }
}
