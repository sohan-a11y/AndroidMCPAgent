package com.android.ai.mcp.storage.automation

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {

    @Query("SELECT * FROM credential_entries ORDER BY appPackage, accountLabel")
    fun observeCredentials(): Flow<List<CredentialEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCredential(entry: CredentialEntryEntity): Long

    @Query(
        """
        SELECT * FROM credential_entries
        WHERE appPackage = :appPackage
        ORDER BY updatedAt DESC
        """
    )
    suspend fun findCredentialsByPackage(appPackage: String): List<CredentialEntryEntity>

    @Query("DELETE FROM credential_entries WHERE id = :id")
    suspend fun deleteCredential(id: Long)

    @Query("SELECT * FROM task_templates ORDER BY updatedAt DESC")
    fun observeTemplates(): Flow<List<TaskTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TaskTemplateEntity): Long

    @Update
    suspend fun updateTemplate(template: TaskTemplateEntity)

    @Query("SELECT * FROM task_templates WHERE id = :id LIMIT 1")
    suspend fun getTemplateById(id: Long): TaskTemplateEntity?

    @Query("DELETE FROM task_templates WHERE id = :id")
    suspend fun deleteTemplate(id: Long)
}
