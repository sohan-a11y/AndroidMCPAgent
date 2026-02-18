package com.android.ai.mcp.ui

data class CredentialFillPrompt(
    val appPackage: String,
    val fieldHint: String?,
    val accountHint: String?,
    val stepNumber: Int,
    val totalSteps: Int
)
