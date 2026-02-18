package com.android.ai.mcp.domain

data class ValidationResult(
    val errors: List<String>
) {
    val isValid: Boolean
        get() = errors.isEmpty()
}
