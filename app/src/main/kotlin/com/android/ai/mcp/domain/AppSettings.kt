package com.android.ai.mcp.domain

data class AppSettings(
    val selectedProvider: AiProvider = AiProvider.OPENROUTER,
    val maxPlanSteps: Int = DEFAULT_MAX_PLAN_STEPS,
    val stepDelayMs: Int = DEFAULT_STEP_DELAY_MS
) {
    companion object {
        const val MIN_MAX_PLAN_STEPS = 1
        const val MAX_MAX_PLAN_STEPS = 50
        const val DEFAULT_MAX_PLAN_STEPS = 20

        const val MIN_STEP_DELAY_MS = 100
        const val MAX_STEP_DELAY_MS = 5000
        const val DEFAULT_STEP_DELAY_MS = 700

        fun sanitizeMaxPlanSteps(value: Int): Int {
            return value.coerceIn(MIN_MAX_PLAN_STEPS, MAX_MAX_PLAN_STEPS)
        }

        fun sanitizeStepDelayMs(value: Int): Int {
            return value.coerceIn(MIN_STEP_DELAY_MS, MAX_STEP_DELAY_MS)
        }
    }
}
