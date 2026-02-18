package com.android.ai.mcp.execution

data class ActionExecutionResult(
    val success: Boolean,
    val resultJson: String? = null,
    val errorMessage: String? = null
)
