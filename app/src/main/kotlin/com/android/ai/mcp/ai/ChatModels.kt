package com.android.ai.mcp.ai

import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.1
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)
