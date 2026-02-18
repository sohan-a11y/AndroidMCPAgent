package com.android.ai.mcp

import com.android.ai.mcp.ai.ActionPlanParser
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ActionPlanParserTest {

    private val parser = ActionPlanParser(
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    )

    @Test
    fun parsesFencedJsonResponse() {
        val raw = """
            ```json
            {
              "steps": [
                {"action": "open_app", "params": {"package_name": "com.example"}},
                {"action": "back", "params": {}}
              ]
            }
            ```
        """.trimIndent()

        val parsed = parser.parse(raw)
        assertEquals(2, parsed.actionPlan.steps.size)
        assertEquals("open_app", parsed.actionPlan.steps[0].action)
    }

    @Test
    fun parsesFlatStepSchemaWithoutParamsObject() {
        val raw = """
            {
              "steps": [
                {"action": "click_by_text", "text": "Allow"}
              ]
            }
        """.trimIndent()

        val parsed = parser.parse(raw)
        assertEquals(1, parsed.actionPlan.steps.size)
        assertEquals("click_by_text", parsed.actionPlan.steps[0].action)
        assertEquals("\"Allow\"", parsed.actionPlan.steps[0].params["text"].toString())
    }
}
