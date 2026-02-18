package com.android.ai.mcp

import com.android.ai.mcp.ai.PromptBuilder
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptBuilderTest {

    @Test
    fun includesDynamicMaxStepLimitInSystemPrompt() {
        val prompt = PromptBuilder().buildSystemPrompt(maxSteps = 8)
        assertTrue(prompt.contains("Return at most 8 steps"))
        assertTrue(prompt.contains("Never output raw passwords"))
        assertTrue(prompt.contains("fill_saved_password"))
    }
}
