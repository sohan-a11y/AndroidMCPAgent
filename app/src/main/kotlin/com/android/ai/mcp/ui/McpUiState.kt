package com.android.ai.mcp.ui

import com.android.ai.mcp.ai.ModelCatalogRepository
import com.android.ai.mcp.domain.ActionPlan
import com.android.ai.mcp.domain.AppSettings
import com.android.ai.mcp.domain.CommandSource
import com.android.ai.mcp.domain.ExecutionState
import com.android.ai.mcp.storage.automation.CredentialEntryEntity
import com.android.ai.mcp.storage.automation.TaskTemplateEntity
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
    val pendingCommandSource: CommandSource = CommandSource.MANUAL,
    val pendingTemplateId: Long? = null,
    val pendingMaxPlanSteps: Int? = null,
    val pendingResumeStepIndex: Int? = null,
    val validationErrors: List<String> = emptyList(),
    val executionState: ExecutionState = ExecutionState.IDLE,
    val executionCurrentStep: Int = 0,
    val executionTotalSteps: Int = 0,
    val executionMessage: String = "",
    val openRouterFreeModels: List<ModelCatalogRepository.CatalogModel> = emptyList(),
    val isRefreshingOpenRouterModels: Boolean = false,
    val modelValidationMessage: String? = null,
    val templates: List<TaskTemplateEntity> = emptyList(),
    val newTemplateName: String = "",
    val credentials: List<CredentialEntryEntity> = emptyList(),
    val credentialAppPackageInput: String = "",
    val credentialFieldHintInput: String = "",
    val credentialAccountLabelInput: String = "",
    val credentialUsernameInput: String = "",
    val credentialPasswordInput: String = "",
    val isVaultUnlocked: Boolean = false,
    val isMicrophonePermissionGranted: Boolean = false,
    val pendingCredentialFillPrompt: CredentialFillPrompt? = null,
    val logs: List<CommandRunEntity> = emptyList(),
    val errorMessage: String? = null
)
