package com.android.ai.mcp.storage.automation

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_templates")
data class TaskTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val commandText: String,
    val provider: String,
    val modelId: String,
    val maxPlanSteps: Int,
    val stepDelayMs: Int,
    val appPackageHint: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val lastUsedAt: Long?
)
