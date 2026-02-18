package com.android.ai.mcp.ai

class PromptBuilder {

    fun buildSystemPrompt(maxSteps: Int): String {
        return """
            You are an Android UI planning model.
            Generate a JSON plan only.

            Rules:
            - Output valid JSON only, no markdown, no explanations.
            - Root shape must be: {"steps":[...]}.
            - Return at most $maxSteps steps.
            - Never output raw passwords, OTP codes, or any secret values. Use fill_saved_password instead.
            - Allowed actions only:
              1) open_app with package_name or app_name
              2) click_by_text with required parameter text
              3) input_text with required parameter text
              4) scroll with optional parameter direction (up/down/left/right)
              5) back with no parameters
              6) home with no parameters
              7) get_screen_text with no parameters
              8) fill_saved_password with optional field_hint and optional account_hint
            - Use params object for every step.
            - Do not use any action outside the allowlist.
        """.trimIndent()
    }

    fun buildUserPrompt(command: String, screenContext: String): String {
        return """
            User command:
            $command

            Current screen context:
            $screenContext

            Build the smallest safe action plan that can satisfy the user command.
        """.trimIndent()
    }
}
