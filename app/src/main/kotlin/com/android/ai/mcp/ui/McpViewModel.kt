package com.android.ai.mcp.ui

import android.Manifest
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.ai.mcp.AndroidAiMcpApplication
import com.android.ai.mcp.domain.AiProvider
import com.android.ai.mcp.domain.CommandSource
import com.android.ai.mcp.domain.ExecutionState
import com.android.ai.mcp.domain.WakeScope
import com.android.ai.mcp.execution.ActionExecutor
import com.android.ai.mcp.storage.automation.TaskTemplateEntity
import com.android.ai.mcp.system.ExecutionForegroundService
import com.android.ai.mcp.system.MCPAccessibilityService
import com.android.ai.mcp.system.PlanNotificationActionReceiver
import com.android.ai.mcp.system.PlanReadyNotifier
import com.android.ai.mcp.system.VoiceCommandService
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class McpViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AndroidAiMcpApplication

    private val _uiState = MutableStateFlow(McpUiState())
    val uiState: StateFlow<McpUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    private var executionJob: Job? = null
    private var credentialPromptContinuation: CancellableContinuation<Boolean>? = null

    @Volatile
    private var stopRequested = false
    private var pendingWakeEnableAfterPermission = false
    private var isHostForeground = false

    private val notificationActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                PlanNotificationActionReceiver.ACTION_CONFIRM_PLAN -> {
                    if (_uiState.value.executionState == ExecutionState.READY_FOR_CONFIRMATION) {
                        confirmAndExecutePlan()
                        PlanReadyNotifier.dismiss(getApplication())
                    }
                }
                PlanNotificationActionReceiver.ACTION_CANCEL_PLAN -> {
                    if (_uiState.value.executionState == ExecutionState.READY_FOR_CONFIRMATION) {
                        cancelPreview()
                        PlanReadyNotifier.dismiss(getApplication())
                    }
                }
            }
        }
    }

    private val voiceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != VoiceCommandService.ACTION_VOICE_COMMAND) return
            if (isVoiceWorkflowBusy()) {
                _uiState.update {
                    it.copy(executionMessage = "Voice command ignored: agent is busy")
                }
                return
            }
            val command = intent.getStringExtra(VoiceCommandService.EXTRA_COMMAND_TEXT)?.trim().orEmpty()
            if (command.isNotEmpty()) {
                onCommandTextChanged(command)
                generatePlanFromCommand(
                    command = command,
                    commandSource = CommandSource.VOICE,
                    templateId = null
                )
            }
        }
    }

    init {
        registerVoiceReceiver()
        registerNotificationActionReceiver()
        observeSettings()
        observeLogs()
        observeTemplates()
        observeCredentials()
        refreshApiKeyState()
        refreshAccessibilityStatus()
        refreshOpenRouterFreeModels()
        syncVaultState()
    }

    override fun onCleared() {
        super.onCleared()
        val context = getApplication<Application>()
        try {
            context.unregisterReceiver(voiceReceiver)
        } catch (_: Exception) { }
        try {
            context.unregisterReceiver(notificationActionReceiver)
        } catch (_: Exception) { }
    }

    fun refreshAccessibilityStatus() {
        _uiState.update { state ->
            state.copy(isAccessibilityEnabled = MCPAccessibilityService.isRunning)
        }
    }

    fun onProviderSelected(provider: AiProvider) {
        viewModelScope.launch {
            app.settingsRepository.setSelectedProvider(provider)
        }
    }

    fun onOpenRouterKeyInputChanged(value: String) {
        _uiState.update { it.copy(openRouterKeyInput = value) }
    }

    fun onNvidiaKeyInputChanged(value: String) {
        _uiState.update { it.copy(nvidiaKeyInput = value) }
    }

    fun saveApiKeys() {
        val state = _uiState.value
        if (state.openRouterKeyInput.isNotBlank()) {
            app.secureStore.setApiKey(AiProvider.OPENROUTER, state.openRouterKeyInput)
        }
        if (state.nvidiaKeyInput.isNotBlank()) {
            app.secureStore.setApiKey(AiProvider.NVIDIA, state.nvidiaKeyInput)
        }

        _uiState.update {
            it.copy(
                openRouterKeyInput = "",
                nvidiaKeyInput = "",
                errorMessage = null
            )
        }
        refreshApiKeyState()
        refreshOpenRouterFreeModels()
    }

    fun onOpenRouterModelInputChanged(value: String) {
        viewModelScope.launch {
            app.settingsRepository.setOpenRouterModelId(value)
        }
    }

    fun onNvidiaModelInputChanged(value: String) {
        viewModelScope.launch {
            app.settingsRepository.setNvidiaModelId(value)
        }
    }

    fun onWakeWordChanged(value: String) {
        viewModelScope.launch {
            app.settingsRepository.setWakeWord(value)
        }
    }

    fun onWakeEnabledChanged(enabled: Boolean) {
        if (!enabled) {
            pendingWakeEnableAfterPermission = false
            viewModelScope.launch {
                app.settingsRepository.setWakeEnabled(false)
            }
            return
        }

        val context = getApplication<Application>()
        if (hasMicrophonePermission(context)) {
            pendingWakeEnableAfterPermission = false
            viewModelScope.launch {
                app.settingsRepository.setWakeEnabled(true)
            }
            return
        }

        pendingWakeEnableAfterPermission = true
        viewModelScope.launch {
            app.settingsRepository.setWakeEnabled(false)
            _events.emit(UiEvent.RequestMicrophonePermission)
        }
    }

    fun onWakeScopeChanged(scope: WakeScope) {
        viewModelScope.launch {
            app.settingsRepository.setWakeScope(scope)
        }
    }

    fun startVoiceListeningNow() {
        val context = getApplication<Application>()
        if (!hasMicrophonePermission(context)) {
            pendingWakeEnableAfterPermission = false
            viewModelScope.launch {
                _events.emit(UiEvent.RequestMicrophonePermission)
            }
            _uiState.update { it.copy(errorMessage = "Microphone permission is required for voice listening") }
            return
        }
        val settings = _uiState.value.settings
        VoiceCommandService.start(context, settings.wakeWord)
    }

    fun stopVoiceListeningNow() {
        VoiceCommandService.stop(getApplication())
    }

    fun onMicrophonePermissionResult(granted: Boolean) {
        _uiState.update { it.copy(isMicrophonePermissionGranted = granted) }
        if (granted) {
            if (pendingWakeEnableAfterPermission) {
                pendingWakeEnableAfterPermission = false
                viewModelScope.launch {
                    app.settingsRepository.setWakeEnabled(true)
                }
            } else {
                syncVoiceService(_uiState.value.settings)
            }
            return
        }

        pendingWakeEnableAfterPermission = false
        viewModelScope.launch {
            app.settingsRepository.setWakeEnabled(false)
        }
        _uiState.update { it.copy(errorMessage = "Microphone permission denied") }
        syncVoiceService(_uiState.value.settings)
    }

    fun onHostForegroundChanged(isForeground: Boolean) {
        isHostForeground = isForeground
        val micGranted = hasMicrophonePermission(getApplication())
        _uiState.update { it.copy(isMicrophonePermissionGranted = micGranted) }
        syncVoiceService(_uiState.value.settings)
    }

    fun onVaultSessionTimeoutChanged(value: String) {
        val parsed = value.toIntOrNull() ?: return
        viewModelScope.launch {
            app.settingsRepository.setVaultSessionTimeoutMinutes(parsed)
        }
    }

    fun refreshOpenRouterFreeModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingOpenRouterModels = true, modelValidationMessage = null) }
            try {
                val apiKey = app.secureStore.getApiKey(AiProvider.OPENROUTER)
                val models = app.modelCatalogRepository.refreshOpenRouterFreeModels(apiKey)
                _uiState.update {
                    it.copy(
                        openRouterFreeModels = models,
                        isRefreshingOpenRouterModels = false
                    )
                }
            } catch (e: Exception) {
                val cached = app.modelCatalogRepository.getCachedOpenRouterFreeModels()
                _uiState.update {
                    it.copy(
                        openRouterFreeModels = cached,
                        isRefreshingOpenRouterModels = false,
                        modelValidationMessage = "Model refresh failed: ${e.message ?: "unknown error"}"
                    )
                }
            }
        }
    }

    fun onCommandTextChanged(value: String) {
        _uiState.update { it.copy(commandText = value) }
    }

    fun incrementMaxSteps() {
        val next = uiState.value.settings.maxPlanSteps + 1
        viewModelScope.launch {
            app.settingsRepository.setMaxPlanSteps(next)
        }
    }

    fun decrementMaxSteps() {
        val next = uiState.value.settings.maxPlanSteps - 1
        viewModelScope.launch {
            app.settingsRepository.setMaxPlanSteps(next)
        }
    }

    fun onMaxStepsInputChanged(value: String) {
        val parsed = value.toIntOrNull() ?: return
        viewModelScope.launch {
            app.settingsRepository.setMaxPlanSteps(parsed)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun cancelPreview() {
        _uiState.update {
            it.copy(
                pendingPlan = null,
                pendingPlanJson = "",
                pendingRunId = null,
                pendingTemplateId = null,
                pendingMaxPlanSteps = null,
                pendingResumeStepIndex = null,
                validationErrors = emptyList(),
                executionState = ExecutionState.IDLE,
                executionMessage = "",
                pendingCredentialFillPrompt = null
            )
        }
    }

    fun requestStopExecution() {
        stopRequested = true
        _uiState.update {
            it.copy(executionMessage = "Stop requested...")
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            app.logsRepository.clearAll()
        }
    }

    fun selectRunForDetail(runId: Long) {
        _uiState.update { it.copy(selectedRunId = runId, selectedRunSteps = emptyList()) }
        viewModelScope.launch {
            app.logsRepository.observeStepsByRun(runId).collectLatest { steps ->
                _uiState.update { it.copy(selectedRunSteps = steps) }
            }
        }
    }

    fun clearRunDetail() {
        _uiState.update { it.copy(selectedRunId = null, selectedRunSteps = emptyList()) }
    }

    fun rerunFromLog(run: com.android.ai.mcp.storage.logs.CommandRunEntity) {
        onCommandTextChanged(run.commandText)
        generatePlanFromCommand(
            command = run.commandText,
            commandSource = CommandSource.MANUAL,
            templateId = null
        )
    }

    fun generatePlan() {
        val command = _uiState.value.commandText.trim()
        generatePlanFromCommand(
            command = command,
            commandSource = CommandSource.MANUAL,
            templateId = null
        )
    }

    fun replanAfterFailure() {
        val state = _uiState.value
        val command = state.lastFailedCommand?.trim()
        val planJson = state.lastFailedPlanJson
        val failedIndex = state.lastFailedStepIndex
        val failedError = state.lastFailedStepError

        if (command.isNullOrEmpty() || planJson.isNullOrEmpty() || failedIndex == null || failedError == null) {
            _uiState.update { it.copy(errorMessage = "No failure context available for re-planning") }
            return
        }

        _uiState.update {
            it.copy(
                lastFailedPlanJson = null,
                lastFailedStepIndex = null,
                lastFailedStepError = null,
                lastFailedCommand = null
            )
        }

        generatePlanFromCommand(
            command = command,
            commandSource = CommandSource.MANUAL,
            templateId = null,
            retryContext = com.android.ai.mcp.ai.AiPlanner.RetryContext(
                previousPlanJson = planJson,
                failedStepIndex = failedIndex,
                failedStepError = failedError
            )
        )
    }

    fun onTemplateNameChanged(value: String) {
        _uiState.update { it.copy(newTemplateName = value) }
    }

    fun saveCurrentCommandAsTemplate() {
        val state = _uiState.value
        val command = state.commandText.trim()
        val name = state.newTemplateName.trim()
        if (command.isEmpty() || name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Template name and command are required") }
            return
        }

        viewModelScope.launch {
            val provider = state.settings.selectedProvider
            val modelId = selectedModelId(provider, state)
            app.taskTemplateRepository.saveTemplate(
                name = name,
                commandText = command,
                provider = provider,
                modelId = modelId,
                maxPlanSteps = state.settings.maxPlanSteps,
                stepDelayMs = state.settings.stepDelayMs,
                appPackageHint = null
            )
            _uiState.update {
                it.copy(
                    newTemplateName = "",
                    executionMessage = "Template saved"
                )
            }
        }
    }

    fun runTemplate(template: TaskTemplateEntity) {
        viewModelScope.launch {
            val provider = AiProvider.fromValue(template.provider)
            app.settingsRepository.setSelectedProvider(provider)
            when (provider) {
                AiProvider.OPENROUTER -> app.settingsRepository.setOpenRouterModelId(template.modelId)
                AiProvider.NVIDIA -> app.settingsRepository.setNvidiaModelId(template.modelId)
            }
            app.settingsRepository.setMaxPlanSteps(template.maxPlanSteps)
            app.settingsRepository.setStepDelayMs(template.stepDelayMs)

            onCommandTextChanged(template.commandText)
            app.taskTemplateRepository.markTemplateUsed(template.id)

            generatePlanFromCommand(
                command = template.commandText,
                commandSource = CommandSource.TEMPLATE,
                templateId = template.id,
                providerOverride = provider,
                modelIdOverride = template.modelId,
                maxStepsOverride = template.maxPlanSteps
            )
        }
    }

    fun deleteTemplate(templateId: Long) {
        viewModelScope.launch {
            app.taskTemplateRepository.deleteTemplate(templateId)
        }
    }

    fun onCredentialAppPackageChanged(value: String) {
        _uiState.update { it.copy(credentialAppPackageInput = value) }
    }

    fun onCredentialFieldHintChanged(value: String) {
        _uiState.update { it.copy(credentialFieldHintInput = value) }
    }

    fun onCredentialAccountLabelChanged(value: String) {
        _uiState.update { it.copy(credentialAccountLabelInput = value) }
    }

    fun onCredentialUsernameChanged(value: String) {
        _uiState.update { it.copy(credentialUsernameInput = value) }
    }

    fun onCredentialPasswordChanged(value: String) {
        _uiState.update { it.copy(credentialPasswordInput = value) }
    }

    fun saveCredential() {
        val state = _uiState.value
        val appPackage = state.credentialAppPackageInput.trim()
        val accountLabel = state.credentialAccountLabelInput.trim()
        val password = state.credentialPasswordInput
        if (appPackage.isEmpty() || accountLabel.isEmpty() || password.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Credential requires app package, account label, and password") }
            return
        }

        viewModelScope.launch {
            app.credentialVaultRepository.upsertCredential(
                id = null,
                appPackage = appPackage,
                fieldHint = state.credentialFieldHintInput,
                accountLabel = accountLabel,
                username = state.credentialUsernameInput,
                passwordPlaintext = password
            )
            _uiState.update {
                it.copy(
                    credentialAppPackageInput = "",
                    credentialFieldHintInput = "",
                    credentialAccountLabelInput = "",
                    credentialUsernameInput = "",
                    credentialPasswordInput = "",
                    executionMessage = "Credential saved"
                )
            }
        }
    }

    fun deleteCredential(id: Long) {
        viewModelScope.launch {
            app.credentialVaultRepository.deleteCredential(id)
        }
    }

    fun requestVaultUnlock() {
        viewModelScope.launch {
            _events.emit(UiEvent.RequestBiometricUnlock)
        }
    }

    fun onVaultUnlockAuthenticated() {
        val timeout = _uiState.value.settings.vaultSessionTimeoutMinutes
        app.credentialVaultRepository.unlockSession(timeout)
        syncVaultState()
    }

    fun lockVault() {
        app.credentialVaultRepository.lockSession()
        syncVaultState()
    }

    fun approveCredentialFill() {
        val continuation = credentialPromptContinuation
        credentialPromptContinuation = null
        continuation?.resume(true)
        _uiState.update { it.copy(pendingCredentialFillPrompt = null) }
    }

    fun rejectCredentialFill() {
        val continuation = credentialPromptContinuation
        credentialPromptContinuation = null
        continuation?.resume(false)
        _uiState.update { it.copy(pendingCredentialFillPrompt = null) }
    }

    fun confirmAndExecutePlan() {
        val state = _uiState.value
        val plan = state.pendingPlan ?: return
        val runId = state.pendingRunId ?: return
        val startStepIndex = state.pendingResumeStepIndex ?: 0
        val allowManualHandoff = startStepIndex == 0

        val validation = app.actionValidator.validate(
            plan = plan,
            maxSteps = state.pendingMaxPlanSteps ?: state.settings.maxPlanSteps
        )
        if (!validation.isValid) {
            viewModelScope.launch {
                app.logsRepository.updateRunStatus(
                    runId = runId,
                    status = ActionExecutor.STATUS_VALIDATION_FAILED,
                    errorMessage = validation.errors.joinToString("; ")
                )
            }
            _uiState.update {
                it.copy(
                    executionState = ExecutionState.IDLE,
                    validationErrors = validation.errors,
                    errorMessage = "Plan no longer valid for current max step setting"
                )
            }
            return
        }

        executionJob?.cancel()
        stopRequested = false

        _uiState.update {
            it.copy(
                executionState = ExecutionState.RUNNING,
                executionCurrentStep = startStepIndex,
                executionTotalSteps = plan.steps.size,
                executionMessage = if (startStepIndex > 0) "Resuming execution..." else "Execution started"
            )
        }

        ExecutionForegroundService.start(getApplication())

        executionJob = viewModelScope.launch {
            try {
                val summary = app.actionExecutor.execute(
                    runId = runId,
                    actionPlan = plan,
                    stepDelayMs = state.settings.stepDelayMs,
                    stepTimeoutMs = state.settings.stepTimeoutMs,
                    startStepIndex = startStepIndex,
                    allowManualHandoff = allowManualHandoff,
                    shouldStop = { stopRequested },
                    onStepStarted = { currentStep, totalSteps, action ->
                        _uiState.update {
                            it.copy(
                                executionCurrentStep = currentStep,
                                executionTotalSteps = totalSteps,
                                executionMessage = "Executing $action ($currentStep/$totalSteps)"
                            )
                        }
                    },
                    onCredentialFillRequested = { step, currentStep, totalSteps ->
                        requestCredentialFillApproval(step, currentStep, totalSteps)
                    }
                )

                when (summary.state) {
                    ExecutionState.AWAITING_USER -> {
                        _uiState.update {
                            it.copy(
                                executionState = ExecutionState.AWAITING_USER,
                                executionMessage = "Manual unlock required. Unlock target app, then tap Resume.",
                                pendingResumeStepIndex = summary.failedStepIndex
                            )
                        }
                    }

                    ExecutionState.COMPLETED,
                    ExecutionState.STOPPED,
                    ExecutionState.FAILED -> {
                        val failureContext = if (summary.state == ExecutionState.FAILED) {
                            Triple(
                                state.pendingPlanJson,
                                summary.failedStepIndex,
                                summary.errorMessage
                            )
                        } else null

                        _uiState.update {
                            it.copy(
                                pendingPlan = null,
                                pendingPlanJson = "",
                                pendingRunId = null,
                                pendingTemplateId = null,
                                pendingMaxPlanSteps = null,
                                pendingResumeStepIndex = null,
                                pendingCredentialFillPrompt = null,
                                executionState = summary.state,
                                executionMessage = when (summary.state) {
                                    ExecutionState.COMPLETED -> "Execution completed"
                                    ExecutionState.STOPPED -> "Execution stopped"
                                    ExecutionState.FAILED -> summary.errorMessage ?: "Execution failed"
                                    else -> "Execution finished"
                                },
                                lastFailedPlanJson = failureContext?.first,
                                lastFailedStepIndex = failureContext?.second,
                                lastFailedStepError = failureContext?.third,
                                lastFailedCommand = if (failureContext != null) state.commandText else null
                            )
                        }
                    }

                    else -> Unit
                }
            } catch (e: Exception) {
                app.logsRepository.updateRunStatus(
                    runId = runId,
                    status = ActionExecutor.STATUS_FAILED,
                    errorMessage = e.message ?: "Execution error"
                )
                _uiState.update {
                    it.copy(
                        pendingPlan = null,
                        pendingPlanJson = "",
                        pendingRunId = null,
                        pendingTemplateId = null,
                        pendingMaxPlanSteps = null,
                        pendingResumeStepIndex = null,
                        pendingCredentialFillPrompt = null,
                        executionState = ExecutionState.FAILED,
                        executionMessage = "",
                        errorMessage = e.message ?: "Execution error"
                    )
                }
            } finally {
                ExecutionForegroundService.stop(getApplication())
            }
        }
    }

    private suspend fun requestCredentialFillApproval(
        step: com.android.ai.mcp.domain.PlanStep,
        currentStep: Int,
        totalSteps: Int
    ): Boolean {
        if (!app.credentialVaultRepository.isSessionUnlocked()) {
            _uiState.update {
                it.copy(errorMessage = "Vault is locked. Unlock vault before approving credential fill.")
            }
            return false
        }

        val appPackage = app.screenContextReader.currentPackageName() ?: "unknown"
        val fieldHint = step.params["field_hint"]?.toString()?.trim('"')
        val accountHint = step.params["account_hint"]?.toString()?.trim('"')

        return suspendCancellableCoroutine { continuation ->
            credentialPromptContinuation?.cancel()
            credentialPromptContinuation = continuation
            _uiState.update {
                it.copy(
                    pendingCredentialFillPrompt = CredentialFillPrompt(
                        appPackage = appPackage,
                        fieldHint = fieldHint,
                        accountHint = accountHint,
                        stepNumber = currentStep,
                        totalSteps = totalSteps
                    )
                )
            }
            continuation.invokeOnCancellation {
                if (credentialPromptContinuation === continuation) {
                    credentialPromptContinuation = null
                    _uiState.update { state -> state.copy(pendingCredentialFillPrompt = null) }
                }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            app.settingsRepository.getSettings().collectLatest { settings ->
                val micGranted = hasMicrophonePermission(getApplication())
                _uiState.update { state ->
                    state.copy(
                        settings = settings,
                        isMicrophonePermissionGranted = micGranted
                    )
                }
                if (!micGranted && settings.wakeEnabled) {
                    pendingWakeEnableAfterPermission = false
                    app.settingsRepository.setWakeEnabled(false)
                }
                syncVaultState()
                syncVoiceService(settings)
            }
        }
    }

    private fun observeLogs() {
        viewModelScope.launch {
            app.logsRepository.observeRuns().collectLatest { runs ->
                _uiState.update { it.copy(logs = runs) }
            }
        }
    }

    private fun observeTemplates() {
        viewModelScope.launch {
            app.taskTemplateRepository.observeTemplates().collectLatest { templates ->
                _uiState.update { it.copy(templates = templates) }
            }
        }
    }

    private fun observeCredentials() {
        viewModelScope.launch {
            app.credentialVaultRepository.observeCredentials().collectLatest { credentials ->
                _uiState.update { it.copy(credentials = credentials) }
            }
        }
    }

    private fun refreshApiKeyState() {
        _uiState.update {
            it.copy(
                hasOpenRouterKey = app.secureStore.hasApiKey(AiProvider.OPENROUTER),
                hasNvidiaKey = app.secureStore.hasApiKey(AiProvider.NVIDIA)
            )
        }
    }

    private fun syncVoiceService(settings: com.android.ai.mcp.domain.AppSettings) {
        val context = getApplication<Application>()
        if (!hasMicrophonePermission(context)) {
            VoiceCommandService.stop(context)
            return
        }

        when {
            !settings.wakeEnabled -> VoiceCommandService.stop(context)
            settings.wakeScope == WakeScope.MANUAL_START -> VoiceCommandService.stop(context)
            settings.wakeScope == WakeScope.APP_OPEN_ONLY && !isHostForeground -> VoiceCommandService.stop(context)
            else -> VoiceCommandService.start(context, settings.wakeWord)
        }
    }

    private fun syncVaultState() {
        _uiState.update {
            it.copy(isVaultUnlocked = app.credentialVaultRepository.isSessionUnlocked())
        }
    }

    private fun registerVoiceReceiver() {
        val context = getApplication<Application>()
        val filter = IntentFilter(VoiceCommandService.ACTION_VOICE_COMMAND)
        ContextCompat.registerReceiver(
            context,
            voiceReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun registerNotificationActionReceiver() {
        val context = getApplication<Application>()
        val filter = IntentFilter().apply {
            addAction(PlanNotificationActionReceiver.ACTION_CONFIRM_PLAN)
            addAction(PlanNotificationActionReceiver.ACTION_CANCEL_PLAN)
        }
        ContextCompat.registerReceiver(
            context,
            notificationActionReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun selectedModelId(provider: AiProvider, state: McpUiState): String {
        return when (provider) {
            AiProvider.OPENROUTER -> state.settings.openRouterModelId
            AiProvider.NVIDIA -> state.settings.nvidiaModelId
        }
    }

    private fun isVoiceWorkflowBusy(): Boolean {
        val state = _uiState.value
        if (state.isPlanning) return true
        return when (state.executionState) {
            ExecutionState.PLANNING,
            ExecutionState.READY_FOR_CONFIRMATION,
            ExecutionState.RUNNING,
            ExecutionState.AWAITING_USER -> true

            else -> false
        }
    }

    private fun hasMicrophonePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun generatePlanFromCommand(
        command: String,
        commandSource: CommandSource,
        templateId: Long?,
        providerOverride: AiProvider? = null,
        modelIdOverride: String? = null,
        maxStepsOverride: Int? = null,
        retryContext: com.android.ai.mcp.ai.AiPlanner.RetryContext? = null
    ) {
        val trimmedCommand = command.trim()
        if (trimmedCommand.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Enter a command first") }
            return
        }

        val state = _uiState.value
        val provider = providerOverride ?: state.settings.selectedProvider
        val modelId = modelIdOverride ?: selectedModelId(provider, state)
        val maxSteps = maxStepsOverride ?: state.settings.maxPlanSteps
        val apiKey = app.secureStore.getApiKey(provider)
        if (apiKey.isNullOrBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Missing API key for ${provider.value}. Add it in Setup.")
            }
            return
        }

        viewModelScope.launch {
            if (provider == AiProvider.OPENROUTER) {
                val validation = app.modelCatalogRepository.validateOpenRouterModel(modelId, apiKey)
                if (!validation.allowed) {
                    _uiState.update {
                        it.copy(errorMessage = validation.message ?: "Selected OpenRouter model is not allowed")
                    }
                    return@launch
                }
                _uiState.update { it.copy(modelValidationMessage = validation.message) }
            }

            _uiState.update {
                it.copy(
                    isPlanning = true,
                    errorMessage = null,
                    validationErrors = emptyList(),
                    executionState = ExecutionState.PLANNING,
                    executionMessage = "Generating action plan..."
                )
            }

            try {
                val screenContext = app.screenContextReader.buildContextForPlanner()
                val planningResult = app.aiPlanner.generatePlan(
                    provider = provider,
                    modelId = modelId,
                    apiKey = apiKey,
                    command = trimmedCommand,
                    screenContext = screenContext,
                    maxSteps = maxSteps,
                    retryContext = retryContext
                )

                val validation = app.actionValidator.validate(
                    plan = planningResult.actionPlan,
                    maxSteps = maxSteps
                )

                if (!validation.isValid) {
                    app.logsRepository.createRun(
                        commandText = trimmedCommand,
                        provider = provider,
                        modelId = modelId,
                        commandSource = commandSource,
                        templateId = templateId,
                        rawPlanJson = planningResult.extractedPlanJson,
                        validatedPlan = planningResult.actionPlan,
                        maxPlanSteps = maxSteps,
                        status = ActionExecutor.STATUS_VALIDATION_FAILED,
                        errorMessage = validation.errors.joinToString("; ")
                    )

                    _uiState.update {
                        it.copy(
                            isPlanning = false,
                            screenContextPreview = screenContext,
                            validationErrors = validation.errors,
                            executionState = ExecutionState.IDLE,
                            executionMessage = "Plan rejected by validator"
                        )
                    }
                    return@launch
                }

                val runId = app.logsRepository.createRun(
                    commandText = trimmedCommand,
                    provider = provider,
                    modelId = planningResult.modelId,
                    commandSource = commandSource,
                    templateId = templateId,
                    rawPlanJson = planningResult.extractedPlanJson,
                    validatedPlan = planningResult.actionPlan,
                    maxPlanSteps = maxSteps,
                    status = ActionExecutor.STATUS_PREVIEW_READY
                )

                _uiState.update {
                    it.copy(
                        isPlanning = false,
                        screenContextPreview = screenContext,
                        pendingPlan = planningResult.actionPlan,
                        pendingPlanJson = planningResult.normalizedPlanJson,
                        pendingRunId = runId,
                        pendingCommandSource = commandSource,
                        pendingTemplateId = templateId,
                        pendingMaxPlanSteps = maxSteps,
                        pendingResumeStepIndex = null,
                        validationErrors = emptyList(),
                        executionState = ExecutionState.READY_FOR_CONFIRMATION,
                        executionMessage = "Preview ready"
                    )
                }

                if (commandSource == CommandSource.VOICE) {
                    PlanReadyNotifier.show(
                        context = getApplication(),
                        commandText = trimmedCommand
                    )
                }

                _events.emit(UiEvent.NavigateToPreview)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isPlanning = false,
                        executionState = ExecutionState.IDLE,
                        executionMessage = "",
                        errorMessage = e.message ?: "Failed to generate plan"
                    )
                }
            }
        }
    }
}
