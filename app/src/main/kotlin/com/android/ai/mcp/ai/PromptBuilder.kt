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
              9) wait_for_text with required parameter text and optional timeout_ms (default 10000). Use after open_app or navigation to wait for a screen to load before interacting.
              10) long_press with required parameter text. Performs a long press on the element matching the text.
            - Use params object for every step.
            - Do not use any action outside the allowlist.
            - When opening apps, prefer package_name values from the launchable_apps list in screen context.
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

    fun buildRetryUserPrompt(
        command: String,
        screenContext: String,
        previousPlanJson: String,
        failedStepIndex: Int,
        failedStepError: String
    ): String {
        return """
            User command:
            $command

            Previous plan failed at step ${failedStepIndex + 1} with error: $failedStepError
            Previous plan:
            $previousPlanJson

            Current screen context (after failure):
            $screenContext

            Generate a corrected plan that avoids the previous failure. You may skip already-completed steps if the screen context shows they succeeded.
        """.trimIndent()
    }
}
