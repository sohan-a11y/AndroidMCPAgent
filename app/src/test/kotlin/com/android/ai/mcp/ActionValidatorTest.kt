package com.android.ai.mcp

import com.android.ai.mcp.domain.ActionPlan
import com.android.ai.mcp.domain.PlanStep
import com.android.ai.mcp.execution.ActionValidator
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionValidatorTest {

    private val validator = ActionValidator()

    @Test
    fun rejectsPlanExceedingConfiguredLimit() {
        val steps = (1..11).map {
            PlanStep(
                action = ActionValidator.ACTION_BACK,
                params = emptyMap()
            )
        }
        val plan = ActionPlan(steps)

        val result = validator.validate(plan, maxSteps = 10)
        assertFalse(result.isValid)
    }

    @Test
    fun acceptsPlanWithinConfiguredLimit() {
        val steps = (1..11).map {
            PlanStep(
                action = ActionValidator.ACTION_BACK,
                params = emptyMap()
            )
        }
        val plan = ActionPlan(steps)

        val result = validator.validate(plan, maxSteps = 50)
        assertTrue(result.isValid)
    }

    @Test
    fun validatesRequiredParams() {
        val plan = ActionPlan(
            steps = listOf(
                PlanStep(action = ActionValidator.ACTION_OPEN_APP, params = emptyMap()),
                PlanStep(action = ActionValidator.ACTION_CLICK_BY_TEXT, params = mapOf("text" to JsonPrimitive("Submit")))
            )
        )

        val result = validator.validate(plan, maxSteps = 20)
        assertFalse(result.isValid)
    }

    @Test
    fun openAppAcceptsAppNameWithoutPackageName() {
        val plan = ActionPlan(
            steps = listOf(
                PlanStep(
                    action = ActionValidator.ACTION_OPEN_APP,
                    params = mapOf("app_name" to JsonPrimitive("YouTube"))
                )
            )
        )

        val result = validator.validate(plan, maxSteps = 20)
        assertTrue(result.isValid)
    }

    @Test
    fun fillSavedPasswordIsAllowed() {
        val plan = ActionPlan(
            steps = listOf(
                PlanStep(
                    action = ActionValidator.ACTION_FILL_SAVED_PASSWORD,
                    params = mapOf("field_hint" to JsonPrimitive("Password"))
                )
            )
        )

        val result = validator.validate(plan, maxSteps = 20)
        assertTrue(result.isValid)
    }
}
