package com.android.ai.mcp.domain

data class AppSettings(
    val selectedProvider: AiProvider = AiProvider.OPENROUTER,
    val maxPlanSteps: Int = DEFAULT_MAX_PLAN_STEPS,
    val stepDelayMs: Int = DEFAULT_STEP_DELAY_MS,
    val openRouterModelId: String = DEFAULT_OPENROUTER_MODEL_ID,
    val nvidiaModelId: String = DEFAULT_NVIDIA_MODEL_ID,
    val wakeWord: String = DEFAULT_WAKE_WORD,
    val wakeEnabled: Boolean = DEFAULT_WAKE_ENABLED,
    val wakeScope: WakeScope = WakeScope.ALWAYS_ON_FOREGROUND,
    val vaultSessionTimeoutMinutes: Int = DEFAULT_VAULT_SESSION_TIMEOUT_MINUTES
) {
    companion object {
        const val MIN_MAX_PLAN_STEPS = 1
        const val MAX_MAX_PLAN_STEPS = 50
        const val DEFAULT_MAX_PLAN_STEPS = 20

        const val MIN_STEP_DELAY_MS = 100
        const val MAX_STEP_DELAY_MS = 5000
        const val DEFAULT_STEP_DELAY_MS = 700

        const val DEFAULT_OPENROUTER_MODEL_ID = "moonshotai/kimi-k2.5:free"
        const val DEFAULT_NVIDIA_MODEL_ID = "moonshotai/kimi-k2.5"

        const val DEFAULT_WAKE_WORD = "AI"
        const val DEFAULT_WAKE_ENABLED = false

        const val MIN_VAULT_SESSION_TIMEOUT_MINUTES = 1
        const val MAX_VAULT_SESSION_TIMEOUT_MINUTES = 30
        const val DEFAULT_VAULT_SESSION_TIMEOUT_MINUTES = 5

        fun sanitizeMaxPlanSteps(value: Int): Int {
            return value.coerceIn(MIN_MAX_PLAN_STEPS, MAX_MAX_PLAN_STEPS)
        }

        fun sanitizeStepDelayMs(value: Int): Int {
            return value.coerceIn(MIN_STEP_DELAY_MS, MAX_STEP_DELAY_MS)
        }

        fun sanitizeModelId(value: String?, fallback: String): String {
            val trimmed = value?.trim().orEmpty()
            return if (trimmed.isEmpty()) fallback else trimmed
        }

        fun sanitizeWakeWord(value: String?): String {
            val trimmed = value?.trim().orEmpty()
            return if (trimmed.isEmpty()) DEFAULT_WAKE_WORD else trimmed
        }

        fun sanitizeVaultSessionTimeoutMinutes(value: Int): Int {
            return value.coerceIn(
                MIN_VAULT_SESSION_TIMEOUT_MINUTES,
                MAX_VAULT_SESSION_TIMEOUT_MINUTES
            )
        }
    }
}
