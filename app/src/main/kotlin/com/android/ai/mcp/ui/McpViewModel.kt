package com.android.ai.mcp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.android.ai.mcp.AndroidAiMcpApplication
import com.android.ai.mcp.domain.AiProvider
import com.android.ai.mcp.domain.ExecutionState
import com.android.ai.mcp.execution.ActionExecutor
import com.android.ai.mcp.system.ExecutionForegroundService
import com.android.ai.mcp.system.MCPAccessibilityService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class McpViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AndroidAiMcpApplication

    private val _uiState = MutableStateFlow(McpUiState())
    val uiState: StateFlow<McpUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    private var executionJob: Job? = null

    @Volatile
    private var stopRequested = false

    init {
        observeSettings()
        observeLogs()
        refreshApiKeyState()
        refreshAccessibilityStatus()
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

    fun cancelPreview() {
        _uiState.update {
            it.copy(
                pendingPlan = null,
                pendingPlanJson = "",
                pendingRunId = null,
                validationErrors = emptyList(),
                executionState = ExecutionState.IDLE,
                executionMessage = ""
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

    fun generatePlan() {
        val state = _uiState.value
        val command = state.commandText.trim()
        if (command.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Enter a command first") }
            return
        }

        val provider = state.settings.selectedProvider
        val apiKey = app.secureStore.getApiKey(provider)
        if (apiKey.isNullOrBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Missing API key for ${provider.value}. Add it in Setup.")
            }
            return
        }

        viewModelScope.launch {
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
                val settings = _uiState.value.settings
                val screenContext = app.screenContextReader.buildContextForPlanner()
                val planningResult = app.aiPlanner.generatePlan(
                    provider = provider,
                    apiKey = apiKey,
                    command = command,
                    screenContext = screenContext,
                    maxSteps = settings.maxPlanSteps
                )

                val validation = app.actionValidator.validate(
                    plan = planningResult.actionPlan,
                    maxSteps = settings.maxPlanSteps
                )

                if (!validation.isValid) {
                    app.logsRepository.createRun(
                        commandText = command,
                        provider = provider,
                        rawPlanJson = planningResult.extractedPlanJson,
                        validatedPlan = planningResult.actionPlan,
                        maxPlanSteps = settings.maxPlanSteps,
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
                    commandText = command,
                    provider = provider,
                    rawPlanJson = planningResult.extractedPlanJson,
                    validatedPlan = planningResult.actionPlan,
                    maxPlanSteps = settings.maxPlanSteps,
                    status = ActionExecutor.STATUS_PREVIEW_READY
                )

                _uiState.update {
                    it.copy(
                        isPlanning = false,
                        screenContextPreview = screenContext,
                        pendingPlan = planningResult.actionPlan,
                        pendingPlanJson = planningResult.normalizedPlanJson,
                        pendingRunId = runId,
                        validationErrors = emptyList(),
                        executionState = ExecutionState.READY_FOR_CONFIRMATION,
                        executionMessage = "Preview ready"
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

    fun confirmAndExecutePlan() {
        val state = _uiState.value
        val plan = state.pendingPlan ?: return
        val runId = state.pendingRunId ?: return

        val validation = app.actionValidator.validate(
            plan = plan,
            maxSteps = state.settings.maxPlanSteps
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
                executionCurrentStep = 0,
                executionTotalSteps = plan.steps.size,
                executionMessage = "Execution started"
            )
        }

        ExecutionForegroundService.start(getApplication())

        executionJob = viewModelScope.launch {
            try {
                val summary = app.actionExecutor.execute(
                    runId = runId,
                    actionPlan = plan,
                    stepDelayMs = state.settings.stepDelayMs,
                    shouldStop = { stopRequested },
                    onStepStarted = { currentStep, totalSteps, action ->
                        _uiState.update {
                            it.copy(
                                executionCurrentStep = currentStep,
                                executionTotalSteps = totalSteps,
                                executionMessage = "Executing $action ($currentStep/$totalSteps)"
                            )
                        }
                    }
                )

                _uiState.update {
                    it.copy(
                        pendingPlan = null,
                        pendingPlanJson = "",
                        pendingRunId = null,
                        executionState = summary.state,
                        executionMessage = when (summary.state) {
                            ExecutionState.COMPLETED -> "Execution completed"
                            ExecutionState.STOPPED -> "Execution stopped"
                            ExecutionState.FAILED -> summary.errorMessage ?: "Execution failed"
                            else -> "Execution finished"
                        }
                    )
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

    private fun observeSettings() {
        viewModelScope.launch {
            app.settingsRepository.getSettings().collectLatest { settings ->
                _uiState.update { state ->
                    state.copy(settings = settings)
                }
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

    private fun refreshApiKeyState() {
        _uiState.update {
            it.copy(
                hasOpenRouterKey = app.secureStore.hasApiKey(AiProvider.OPENROUTER),
                hasNvidiaKey = app.secureStore.hasApiKey(AiProvider.NVIDIA)
            )
        }
    }
}
