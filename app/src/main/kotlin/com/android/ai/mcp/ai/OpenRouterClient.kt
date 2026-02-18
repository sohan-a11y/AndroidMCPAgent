package com.android.ai.mcp.ai

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class OpenRouterClient(
    httpClient: OkHttpClient,
    json: Json
) : BaseChatClient(httpClient, json) {

    override val endpointUrl: String = "https://openrouter.ai/api/v1/chat/completions"

    override fun extraHeaders(): Map<String, String> {
        return mapOf(
            "HTTP-Referer" to "https://android-ai-mcp.local",
            "X-Title" to "Android AI MCP"
        )
    }
}
