package com.android.ai.mcp.ui

import com.android.ai.mcp.domain.ActionPlan
import com.android.ai.mcp.domain.AppSettings
import com.android.ai.mcp.domain.ExecutionState
import com.android.ai.mcp.storage.logs.CommandRunEntity

data class McpUiState(
    val settings: AppSettings = AppSettings(),
    val hasOpenRouterKey: Boolean = false,
    val hasNvidiaKey: Boolean = false,
    val openRouterKeyInput: String = "",
    val nvidiaKeyInput: String = "",
    val commandText: String = "",
    val screenContextPreview: String = "",
    val isAccessibilityEnabled: Boolean = false,
    val isPlanning: Boolean = false,
    val pendingPlan: ActionPlan? = null,
    val pendingPlanJson: String = "",
    val pendingRunId: Long? = null,
    val validationErrors: List<String> = emptyList(),
    val executionState: ExecutionState = ExecutionState.IDLE,
    val executionCurrentStep: Int = 0,
    val executionTotalSteps: Int = 0,
    val executionMessage: String = "",
    val logs: List<CommandRunEntity> = emptyList(),
    val errorMessage: String? = null
)
