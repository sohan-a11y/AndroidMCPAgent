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
        val modelId: String,
        val normalizedPlanJson: String,
        val extractedPlanJson: String,
        val rawModelOutput: String,
        val rawProviderResponse: String
    )

    suspend fun generatePlan(
        provider: AiProvider,
        modelId: String,
        apiKey: String,
        command: String,
        screenContext: String,
        maxSteps: Int
    ): PlanningResult {
        val systemPrompt = promptBuilder.buildSystemPrompt(maxSteps)
        val userPrompt = promptBuilder.buildUserPrompt(command, screenContext)

        val providerResponse = when (provider) {
            AiProvider.OPENROUTER -> {
                openRouterClient.generatePlan(apiKey, modelId, systemPrompt, userPrompt)
            }

            AiProvider.NVIDIA -> {
                nvidiaClient.generatePlan(apiKey, modelId, systemPrompt, userPrompt)
            }
        }

        val parsed = actionPlanParser.parse(providerResponse.modelContent)
        return PlanningResult(
            actionPlan = parsed.actionPlan,
            modelId = modelId,
            normalizedPlanJson = parsed.normalizedJson,
            extractedPlanJson = parsed.extractedJson,
            rawModelOutput = providerResponse.modelContent,
            rawProviderResponse = providerResponse.rawResponse
        )
    }
}
