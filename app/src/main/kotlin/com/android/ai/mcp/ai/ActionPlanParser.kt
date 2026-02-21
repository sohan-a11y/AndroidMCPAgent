package com.android.ai.mcp.ai

import com.android.ai.mcp.domain.ActionPlan
import com.android.ai.mcp.domain.PlanStep
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class ActionPlanParser(
    private val json: Json
) {

    data class ParsedActionPlan(
        val actionPlan: ActionPlan,
        val normalizedJson: String,
        val extractedJson: String
    )

    fun parse(rawModelText: String): ParsedActionPlan {
        val extractedJson = extractJsonPayload(rawModelText)
        val root = json.parseToJsonElement(extractedJson) as? JsonObject
            ?: throw IllegalStateException("Model output is not a JSON object")

        val steps = (root["steps"] as? JsonArray)
            ?.mapIndexed { index, element ->
                val stepObject = element as? JsonObject
                    ?: throw IllegalStateException("Step $index is not a JSON object")

                val action = stepObject["action"]?.jsonPrimitive?.content
                    ?: throw IllegalStateException("Step $index is missing 'action'")

                val params = when (val paramsElement = stepObject["params"]) {
                    is JsonObject -> paramsElement.toMap()
                    else -> stepObject
                        .filterKeys { it != "action" && it != "params" }
                        .mapValues { (_, value) -> value as JsonElement }
                }

                PlanStep(action = action, params = params)
            }
            ?: throw IllegalStateException("Model output is missing steps array")

        val actionPlan = ActionPlan(steps = steps)
        return ParsedActionPlan(
            actionPlan = actionPlan,
            normalizedJson = json.encodeToString(actionPlan),
            extractedJson = extractedJson
        )
    }

    private fun extractJsonPayload(rawModelText: String): String {
        var text = rawModelText.trim()

        if (text.startsWith("```")) {
            text = text
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        }

        val firstBrace = text.indexOf('{')
        val lastBrace = text.lastIndexOf('}')
        if (firstBrace < 0 || lastBrace <= firstBrace) {
            throw IllegalStateException("Could not find JSON object in model output")
        }

        return text.substring(firstBrace, lastBrace + 1)
    }
}
