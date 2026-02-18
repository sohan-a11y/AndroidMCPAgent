package com.android.ai.mcp.domain

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ActionPlan(
    val steps: List<PlanStep>
)

@Serializable
data class PlanStep(
    val action: String,
    val params: Map<String, JsonElement> = emptyMap()
)
