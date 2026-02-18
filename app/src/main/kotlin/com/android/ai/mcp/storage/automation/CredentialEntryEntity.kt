package com.android.ai.mcp.storage.automation

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "credential_entries",
    indices = [
        Index(
            value = ["appPackage", "fieldHint", "accountLabel"],
            unique = true
        )
    ]
)
data class CredentialEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val appPackage: String,
    val fieldHint: String?,
    val accountLabel: String,
    val username: String?,
    val passwordCiphertext: String,
    val updatedAt: Long
)
