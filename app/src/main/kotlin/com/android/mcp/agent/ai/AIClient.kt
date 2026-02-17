package com.android.mcp.agent.ai

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Unified AI client for making requests to various AI providers.
 *
 * Uses Ktor HTTP client to send OpenAI-compatible chat completion requests.
 * All supported providers (OpenRouter, NVIDIA NIM, Kimi) implement the same API format.
 *
 * Example usage:
 * ```kotlin
 * val client = AIClient(
 *     provider = AIProvider.OpenRouter,
 *     apiKey = "sk-or-v1-...",
 *     model = "google/gemini-2.0-flash-001"
 * )
 * val response = client.chatCompletion(
 *     messages = listOf(
 *         ChatMessage.system("You are a helpful assistant"),
 *         ChatMessage.user("What's the weather?")
 *     )
 * )
 * println(response.choices.first().message.content)
 * ```
 */
class AIClient(
    private val provider: AIProvider,
    private val apiKey: String,
    private val model: String? = null
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(this@AIClient.json)
        }
    }

    /**
     * Sends a chat completion request to the AI provider.
     *
     * @param messages List of chat messages in the conversation
     * @param temperature Controls randomness (0.0 to 2.0), default 0.7
     * @param maxTokens Maximum tokens to generate, default 1024
     * @return ChatCompletionResponse containing the AI's response
     * @throws Exception if the request fails
     */
    suspend fun chatCompletion(
        messages: List<ChatMessage>,
        temperature: Double = 0.7,
        maxTokens: Int = 1024
    ): ChatCompletionResponse {
        val requestBody = ChatCompletionRequest(
            model = model ?: provider.defaultModel,
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens
        )

        val response: HttpResponse = httpClient.post(provider.baseUrl) {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            
            // OpenRouter-specific headers
            if (provider is AIProvider.OpenRouter) {
                header("HTTP-Referer", "https://github.com/sohan-a11y/AndroidMCPAgent")
                header("X-Title", "Android MCP Agent")
            }

            setBody(requestBody)
        }

        if (!response.status.isSuccess()) {
            val errorBody = response.bodyAsText()
            throw Exception("AI request failed: ${response.status} - $errorBody")
        }

        val responseText = response.bodyAsText()
        return json.decodeFromString(responseText)
    }

    /**
     * Closes the HTTP client and releases resources.
     * Call this when done using the client.
     */
    fun close() {
        httpClient.close()
    }
}

/**
 * Represents a message in the chat conversation.
 */
@Serializable
data class ChatMessage(
    val role: String,
    val content: String
) {
    companion object {
        fun system(content: String) = ChatMessage("system", content)
        fun user(content: String) = ChatMessage("user", content)
        fun assistant(content: String) = ChatMessage("assistant", content)
    }
}

/**
 * Request body for chat completion API (OpenAI-compatible format).
 */
@Serializable
internal data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens")
    val maxTokens: Int = 1024
)

/**
 * Response from chat completion API.
 */
@Serializable
data class ChatCompletionResponse(
    val id: String,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage? = null
)

@Serializable
data class Choice(
    val index: Int,
    val message: ChatMessage,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)
