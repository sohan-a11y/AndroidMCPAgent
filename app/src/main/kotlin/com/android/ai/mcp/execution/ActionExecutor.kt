package com.android.ai.mcp.execution

import com.android.ai.mcp.domain.ActionPlan
import com.android.ai.mcp.domain.ExecutionState
import com.android.ai.mcp.storage.logs.LogsRepository
import kotlinx.coroutines.delay

class ActionExecutor(
    private val uiActionPerformer: UiActionPerformer,
    private val logsRepository: LogsRepository
) {

    data class ExecutionSummary(
        val state: ExecutionState,
        val completedSteps: Int,
        val errorMessage: String?
    )

    suspend fun execute(
        runId: Long,
        actionPlan: ActionPlan,
        stepDelayMs: Int,
        shouldStop: () -> Boolean,
        onStepStarted: (currentStep: Int, totalSteps: Int, action: String) -> Unit
    ): ExecutionSummary {
        logsRepository.updateRunStatus(runId = runId, status = STATUS_RUNNING, endedAt = null)

        val totalSteps = actionPlan.steps.size
        var completedSteps = 0

        actionPlan.steps.forEachIndexed { index, step ->
            if (shouldStop()) {
                logsRepository.updateRunStatus(runId = runId, status = STATUS_STOPPED)
                return ExecutionSummary(
                    state = ExecutionState.STOPPED,
                    completedSteps = completedSteps,
                    errorMessage = "Execution stopped by user"
                )
            }

            onStepStarted(index + 1, totalSteps, step.action)

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
                logsRepository.updateRunStatus(
                    runId = runId,
                    status = STATUS_FAILED,
                    errorMessage = result.errorMessage
                )
                return ExecutionSummary(
                    state = ExecutionState.FAILED,
                    completedSteps = completedSteps,
                    errorMessage = result.errorMessage
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

    companion object {
        const val STATUS_PREVIEW_READY = "preview_ready"
        const val STATUS_RUNNING = "running"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_FAILED = "failed"
        const val STATUS_STOPPED = "stopped"
        const val STATUS_VALIDATION_FAILED = "validation_failed"

        const val STEP_STATUS_SUCCESS = "success"
        const val STEP_STATUS_ERROR = "error"
    }
}
