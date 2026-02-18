package com.android.ai.mcp.ai

import com.android.ai.mcp.domain.ActionPlan
import com.android.ai.mcp.domain.AiProvider

class AiPlanner(
    private val openRouterClient: OpenRouterClient,
    private val nvidiaClient: NvidiaClient,
    private val promptBuilder: PromptBuilder,
    private val actionPlanParser: ActionPlanParser
) {

    data class PlanningResult(
        val actionPlan: ActionPlan,
        val normalizedPlanJson: String,
        val extractedPlanJson: String,
        val rawModelOutput: String,
        val rawProviderResponse: String
    )

    suspend fun generatePlan(
        provider: AiProvider,
        apiKey: String,
        command: String,
        screenContext: String,
        maxSteps: Int
    ): PlanningResult {
        val systemPrompt = promptBuilder.buildSystemPrompt(maxSteps)
        val userPrompt = promptBuilder.buildUserPrompt(command, screenContext)

        val providerResponse = when (provider) {
            AiProvider.OPENROUTER -> {
                openRouterClient.generatePlan(apiKey, systemPrompt, userPrompt)
            }

            AiProvider.NVIDIA -> {
                nvidiaClient.generatePlan(apiKey, systemPrompt, userPrompt)
            }
        }

        val parsed = actionPlanParser.parse(providerResponse.modelContent)
        return PlanningResult(
            actionPlan = parsed.actionPlan,
            normalizedPlanJson = parsed.normalizedJson,
            extractedPlanJson = parsed.extractedJson,
            rawModelOutput = providerResponse.modelContent,
            rawProviderResponse = providerResponse.rawResponse
        )
    }
}
