package com.android.ai.mcp.storage.logs

import com.android.ai.mcp.domain.ActionPlan
import com.android.ai.mcp.domain.AiProvider
import com.android.ai.mcp.domain.PlanStep
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LogsRepository(
    private val logsDao: LogsDao,
    private val json: Json
) {

    fun observeRuns(): Flow<List<CommandRunEntity>> = logsDao.observeRuns()

    fun observeStepsByRun(runId: Long): Flow<List<StepExecutionEntity>> = logsDao.observeStepsByRun(runId)

    suspend fun createRun(
        commandText: String,
        provider: AiProvider,
        rawPlanJson: String,
        validatedPlan: ActionPlan?,
        maxPlanSteps: Int,
        status: String,
        errorMessage: String? = null
    ): Long {
        val now = System.currentTimeMillis()
        val entity = CommandRunEntity(
            commandText = commandText,
            provider = provider.value,
            rawPlanJson = rawPlanJson,
            validatedPlanJson = validatedPlan?.let { json.encodeToString(it) },
            status = status,
            maxPlanSteps = maxPlanSteps,
            planStepCount = validatedPlan?.steps?.size ?: 0,
            startedAt = now,
            endedAt = if (status in FINAL_STATES) now else null,
            errorMessage = errorMessage
        )
        return logsDao.insertRun(entity)
    }

    suspend fun updateRunStatus(
        runId: Long,
        status: String,
        errorMessage: String? = null,
        endedAt: Long? = if (status in FINAL_STATES) System.currentTimeMillis() else null
    ) {
        logsDao.updateRunStatus(runId, status, endedAt, errorMessage)
    }

    suspend fun addStepExecution(
        runId: Long,
        stepIndex: Int,
        step: PlanStep,
        status: String,
        durationMs: Long,
        resultJson: String? = null,
        errorMessage: String? = null
    ) {
        val entity = StepExecutionEntity(
            runId = runId,
            stepIndex = stepIndex,
            action = step.action,
            paramsJson = json.encodeToString(step.params),
            status = status,
            durationMs = durationMs,
            resultJson = resultJson,
            errorMessage = errorMessage,
            createdAt = System.currentTimeMillis()
        )
        logsDao.insertStep(entity)
    }

    suspend fun clearAll() {
        logsDao.clearSteps()
        logsDao.clearRuns()
    }

    companion object {
        private val FINAL_STATES = setOf("completed", "failed", "stopped", "validation_failed")
    }
}
