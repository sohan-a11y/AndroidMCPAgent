package com.android.ai.mcp.execution

import com.android.ai.mcp.domain.ActionPlan
import com.android.ai.mcp.domain.ExecutionState
import com.android.ai.mcp.domain.PlanStep
import com.android.ai.mcp.storage.logs.LogsRepository
import kotlinx.coroutines.delay

class ActionExecutor(
    private val uiActionPerformer: UiActionPerformer,
    private val logsRepository: LogsRepository
) {

    data class ExecutionSummary(
        val state: ExecutionState,
        val completedSteps: Int,
        val errorMessage: String?,
        val failedStepIndex: Int? = null
    )

    suspend fun execute(
        runId: Long,
        actionPlan: ActionPlan,
        stepDelayMs: Int,
        startStepIndex: Int = 0,
        allowManualHandoff: Boolean = true,
        shouldStop: () -> Boolean,
        onStepStarted: (currentStep: Int, totalSteps: Int, action: String) -> Unit,
        onCredentialFillRequested: suspend (step: PlanStep, currentStep: Int, totalSteps: Int) -> Boolean = { _, _, _ -> true }
    ): ExecutionSummary {
        logsRepository.updateRunStatus(runId = runId, status = STATUS_RUNNING, endedAt = null)

        val totalSteps = actionPlan.steps.size
        var completedSteps = startStepIndex

        for (index in startStepIndex until totalSteps) {
            val step = actionPlan.steps[index]
            if (shouldStop()) {
                logsRepository.updateRunStatus(runId = runId, status = STATUS_STOPPED)
                return ExecutionSummary(
                    state = ExecutionState.STOPPED,
                    completedSteps = completedSteps,
                    errorMessage = "Execution stopped by user"
                )
            }

            onStepStarted(index + 1, totalSteps, step.action)

            if (step.action == ActionValidator.ACTION_FILL_SAVED_PASSWORD) {
                val approved = onCredentialFillRequested(step, index + 1, totalSteps)
                if (!approved) {
                    val message = "Credential fill was declined by user"
                    logsRepository.addStepExecution(
                        runId = runId,
                        stepIndex = index,
                        step = step,
                        status = STEP_STATUS_ERROR,
                        durationMs = 0,
                        resultJson = null,
                        errorMessage = message
                    )
                    logsRepository.updateRunStatus(
                        runId = runId,
                        status = STATUS_FAILED,
                        errorMessage = message
                    )
                    return ExecutionSummary(
                        state = ExecutionState.FAILED,
                        completedSteps = completedSteps,
                        errorMessage = message,
                        failedStepIndex = index
                    )
                }
            }

            val startedAt = System.currentTimeMillis()
            val result = uiActionPerformer.execute(step)
            val duration = System.currentTimeMillis() - startedAt

            logsRepository.addStepExecution(
                runId = runId,
                stepIndex = index,
                step = step,
                status = if (result.success) STEP_STATUS_SUCCESS else STEP_STATUS_ERROR,
                durationMs = duration,
                resultJson = result.resultJson,
                errorMessage = result.errorMessage
            )

            if (!result.success) {
                if (allowManualHandoff && shouldPauseForManualHandoff(step, result.errorMessage)) {
                    logsRepository.updateRunStatus(
                        runId = runId,
                        status = STATUS_AWAITING_USER,
                        errorMessage = result.errorMessage
                    )
                    return ExecutionSummary(
                        state = ExecutionState.AWAITING_USER,
                        completedSteps = completedSteps,
                        errorMessage = result.errorMessage,
                        failedStepIndex = index
                    )
                }

                logsRepository.updateRunStatus(
                    runId = runId,
                    status = STATUS_FAILED,
                    errorMessage = result.errorMessage
                )
                return ExecutionSummary(
                    state = ExecutionState.FAILED,
                    completedSteps = completedSteps,
                    errorMessage = result.errorMessage,
                    failedStepIndex = index
                )
            }

            completedSteps += 1
            if (index < totalSteps - 1) {
                delay(stepDelayMs.toLong())
            }
        }

        logsRepository.updateRunStatus(runId = runId, status = STATUS_COMPLETED)
        return ExecutionSummary(
            state = ExecutionState.COMPLETED,
            completedSteps = completedSteps,
            errorMessage = null
        )
    }

    private fun shouldPauseForManualHandoff(step: PlanStep, errorMessage: String?): Boolean {
        val lowered = errorMessage?.lowercase().orEmpty()
        val sensitiveAction = step.action in setOf(
            ActionValidator.ACTION_CLICK_BY_TEXT,
            ActionValidator.ACTION_INPUT_TEXT,
            ActionValidator.ACTION_FILL_SAVED_PASSWORD
        )
        val blockedSignal = lowered.contains("unavailable") ||
            lowered.contains("no editable field") ||
            lowered.contains("no element matched") ||
            lowered.contains("locked")
        return sensitiveAction && blockedSignal
    }

    companion object {
        const val STATUS_PREVIEW_READY = "preview_ready"
        const val STATUS_RUNNING = "running"
        const val STATUS_AWAITING_USER = "awaiting_user"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_FAILED = "failed"
        const val STATUS_STOPPED = "stopped"
        const val STATUS_VALIDATION_FAILED = "validation_failed"

        const val STEP_STATUS_SUCCESS = "success"
        const val STEP_STATUS_ERROR = "error"
    }
}
