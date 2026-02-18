package com.android.ai.mcp.storage.logs

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_runs")
data class CommandRunEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val commandText: String,
    val provider: String,
    val rawPlanJson: String,
    val validatedPlanJson: String?,
    val status: String,
    val maxPlanSteps: Int,
    val planStepCount: Int,
    val startedAt: Long,
    val endedAt: Long?,
    val errorMessage: String?
)
