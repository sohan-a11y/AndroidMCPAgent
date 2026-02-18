package com.android.ai.mcp.execution

import com.android.ai.mcp.domain.ActionPlan
import com.android.ai.mcp.domain.AppSettings
import com.android.ai.mcp.domain.ValidationResult
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class ActionValidator {

    companion object {
        const val ACTION_OPEN_APP = "open_app"
        const val ACTION_CLICK_BY_TEXT = "click_by_text"
        const val ACTION_INPUT_TEXT = "input_text"
        const val ACTION_SCROLL = "scroll"
        const val ACTION_BACK = "back"
        const val ACTION_HOME = "home"
        const val ACTION_GET_SCREEN_TEXT = "get_screen_text"
        const val ACTION_FILL_SAVED_PASSWORD = "fill_saved_password"

        val ALLOWED_ACTIONS = setOf(
            ACTION_OPEN_APP,
            ACTION_CLICK_BY_TEXT,
            ACTION_INPUT_TEXT,
            ACTION_SCROLL,
            ACTION_BACK,
            ACTION_HOME,
            ACTION_GET_SCREEN_TEXT,
            ACTION_FILL_SAVED_PASSWORD
        )

        private val ALLOWED_SCROLL_DIRECTIONS = setOf("up", "down", "left", "right")
    }

    fun validate(plan: ActionPlan, maxSteps: Int): ValidationResult {
        val errors = mutableListOf<String>()
        val safeMaxSteps = AppSettings.sanitizeMaxPlanSteps(maxSteps)

        if (plan.steps.isEmpty()) {
            errors.add("Plan has no steps")
        }

        if (plan.steps.size > safeMaxSteps) {
            errors.add("Plan has ${plan.steps.size} steps but max allowed is $safeMaxSteps")
        }

        plan.steps.forEachIndexed { index, step ->
            if (step.action !in ALLOWED_ACTIONS) {
                errors.add("Step ${index + 1}: unsupported action '${step.action}'")
                return@forEachIndexed
            }

            when (step.action) {
                ACTION_OPEN_APP -> {
                    val packageName = step.params.requireString("package_name")
                    val appName = step.params.requireString("app_name")
                    if (packageName == null && appName == null) {
                        errors.add("Step ${index + 1}: open_app requires package_name or app_name")
                    }
                }

                ACTION_CLICK_BY_TEXT -> {
                    if (step.params.requireString("text") == null) {
                        errors.add("Step ${index + 1}: click_by_text requires text")
                    }
                }

                ACTION_INPUT_TEXT -> {
                    if (step.params.requireString("text") == null) {
                        errors.add("Step ${index + 1}: input_text requires text")
                    }
                }

                ACTION_SCROLL -> {
                    val direction = step.params.optionalString("direction")?.lowercase()
                    if (direction != null && direction !in ALLOWED_SCROLL_DIRECTIONS) {
                        errors.add(
                            "Step ${index + 1}: scroll direction must be one of $ALLOWED_SCROLL_DIRECTIONS"
                        )
                    }
                }

                ACTION_BACK,
                ACTION_HOME,
                ACTION_GET_SCREEN_TEXT -> {
                    // No required params.
                }

                ACTION_FILL_SAVED_PASSWORD -> {
                    val fieldHint = step.params.optionalString("field_hint")
                    val accountHint = step.params.optionalString("account_hint")
                    if (fieldHint != null && fieldHint.length > 120) {
                        errors.add("Step ${index + 1}: field_hint is too long")
                    }
                    if (accountHint != null && accountHint.length > 120) {
                        errors.add("Step ${index + 1}: account_hint is too long")
                    }
                }
            }
        }

        return ValidationResult(errors)
    }

    private fun Map<String, JsonElement>.requireString(key: String): String? {
        return optionalString(key)?.takeIf { it.isNotEmpty() }
    }

    private fun Map<String, JsonElement>.optionalString(key: String): String? {
        val primitive = this[key] as? JsonPrimitive ?: return null
        return primitive.contentOrNull?.trim()
    }
}
