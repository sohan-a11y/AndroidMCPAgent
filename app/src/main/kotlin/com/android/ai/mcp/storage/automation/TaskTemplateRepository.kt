package com.android.ai.mcp.storage.automation

import com.android.ai.mcp.domain.AiProvider
import com.android.ai.mcp.domain.AppSettings
import kotlinx.coroutines.flow.Flow

class TaskTemplateRepository(
    private val automationDao: AutomationDao
) {

    fun observeTemplates(): Flow<List<TaskTemplateEntity>> = automationDao.observeTemplates()

    suspend fun saveTemplate(
        name: String,
        commandText: String,
        provider: AiProvider,
        modelId: String,
        maxPlanSteps: Int,
        stepDelayMs: Int,
        appPackageHint: String?
    ): Long {
        val now = System.currentTimeMillis()
        val template = TaskTemplateEntity(
            name = name.trim(),
            commandText = commandText.trim(),
            provider = provider.value,
            modelId = AppSettings.sanitizeModelId(modelId, AppSettings.DEFAULT_OPENROUTER_MODEL_ID),
            maxPlanSteps = AppSettings.sanitizeMaxPlanSteps(maxPlanSteps),
            stepDelayMs = AppSettings.sanitizeStepDelayMs(stepDelayMs),
            appPackageHint = appPackageHint?.trim()?.takeIf { it.isNotEmpty() },
            createdAt = now,
            updatedAt = now,
            lastUsedAt = null
        )
        return automationDao.insertTemplate(template)
    }

    suspend fun markTemplateUsed(id: Long) {
        val template = automationDao.getTemplateById(id) ?: return
        automationDao.updateTemplate(
            template.copy(
                updatedAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getTemplate(id: Long): TaskTemplateEntity? = automationDao.getTemplateById(id)

    suspend fun deleteTemplate(id: Long) {
        automationDao.deleteTemplate(id)
    }
}
