package com.android.ai.mcp.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

abstract class BaseChatClient(
    private val httpClient: OkHttpClient,
    private val json: Json
) {

    protected abstract val endpointUrl: String
    protected abstract val modelId: String

    protected open fun extraHeaders(): Map<String, String> = emptyMap()

    suspend fun generatePlan(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String
    ): PlanGenerationResponse {
        return withContext(Dispatchers.IO) {
            val payload = ChatCompletionRequest(
                model = modelId,
                messages = listOf(
                    ChatMessage(role = "system", content = systemPrompt),
                    ChatMessage(role = "user", content = userPrompt)
                )
            )

            val request = Request.Builder()
                .url(endpointUrl)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .apply {
                    extraHeaders().forEach { (key, value) -> header(key, value) }
                }
                .post(
                    json.encodeToString(payload)
                        .toRequestBody("application/json".toMediaType())
                )
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException(
                        "Provider request failed (${response.code}): $body"
                    )
                }

                val modelContent = extractModelContent(body)
                    ?: throw IllegalStateException("Provider response did not include model content")

                PlanGenerationResponse(
                    modelContent = modelContent,
                    rawResponse = body
                )
            }
        }
    }

    private fun extractModelContent(rawBody: String): String? {
        val root = json.parseToJsonElement(rawBody) as? JsonObject ?: return null
        val choices = root["choices"] as? JsonArray ?: return null
        val firstChoice = choices.firstOrNull() as? JsonObject ?: return null
        val message = firstChoice["message"] as? JsonObject ?: return null
        val content = message["content"] ?: return null
        return content.toNormalizedText()
    }

    private fun JsonElement.toNormalizedText(): String? {
        return when (this) {
            is JsonPrimitive -> contentOrNull
            is JsonArray -> {
                buildString {
                    for (part in this@toNormalizedText) {
                        when (part) {
                            is JsonPrimitive -> {
                                part.contentOrNull?.let { append(it) }
                            }

                            is JsonObject -> {
                                val text = (part["text"] as? JsonPrimitive)?.contentOrNull
                                if (text != null) append(text)
                            }

                            else -> Unit
                        }
                    }
                }.ifBlank { null }
            }

            else -> null
        }
    }
}
