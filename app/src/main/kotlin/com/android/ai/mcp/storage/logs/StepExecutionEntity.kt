package com.android.ai.mcp.storage.logs

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "step_executions",
    indices = [Index("runId")]
)
data class StepExecutionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val runId: Long,
    val stepIndex: Int,
    val action: String,
    val paramsJson: String,
    val status: String,
    val durationMs: Long,
    val resultJson: String?,
    val errorMessage: String?,
    val createdAt: Long
)
