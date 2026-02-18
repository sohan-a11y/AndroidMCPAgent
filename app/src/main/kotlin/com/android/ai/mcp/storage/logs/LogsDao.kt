package com.android.ai.mcp.storage.logs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: CommandRunEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: StepExecutionEntity)

    @Query("SELECT * FROM command_runs ORDER BY startedAt DESC")
    fun observeRuns(): Flow<List<CommandRunEntity>>

    @Query("SELECT * FROM step_executions WHERE runId = :runId ORDER BY stepIndex ASC")
    fun observeStepsByRun(runId: Long): Flow<List<StepExecutionEntity>>

    @Query("UPDATE command_runs SET status = :status, endedAt = :endedAt, errorMessage = :errorMessage WHERE id = :runId")
    suspend fun updateRunStatus(runId: Long, status: String, endedAt: Long?, errorMessage: String?)

    @Query("DELETE FROM step_executions")
    suspend fun clearSteps()

    @Query("DELETE FROM command_runs")
    suspend fun clearRuns()
}
