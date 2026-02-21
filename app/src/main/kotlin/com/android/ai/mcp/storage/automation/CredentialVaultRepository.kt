package com.android.ai.mcp.storage.automation

import kotlinx.coroutines.flow.Flow

class CredentialVaultRepository(
    private val automationDao: AutomationDao,
    private val cipherManager: CredentialCipherManager,
    private val sessionManager: VaultSessionManager
) {

    data class ResolvedCredential(
        val id: Long,
        val appPackage: String,
        val fieldHint: String?,
        val accountLabel: String,
        val username: String?,
        val password: String
    )

    fun observeCredentials(): Flow<List<CredentialEntryEntity>> = automationDao.observeCredentials()

    suspend fun upsertCredential(
        id: Long?,
        appPackage: String,
        fieldHint: String?,
        accountLabel: String,
        username: String?,
        passwordPlaintext: String
    ) {
        val now = System.currentTimeMillis()
        val ciphertext = cipherManager.encrypt(passwordPlaintext)
        val entity = CredentialEntryEntity(
            id = id ?: 0,
            appPackage = appPackage.trim(),
            fieldHint = fieldHint?.trim()?.takeIf { it.isNotEmpty() },
            accountLabel = accountLabel.trim(),
            username = username?.trim()?.takeIf { it.isNotEmpty() },
            passwordCiphertext = ciphertext,
            updatedAt = now
        )
        automationDao.upsertCredential(entity)
    }

    suspend fun deleteCredential(id: Long) {
        automationDao.deleteCredential(id)
    }

    fun isSessionUnlocked(): Boolean = sessionManager.isUnlocked()

    fun unlockSession(timeoutMinutes: Int) {
        sessionManager.unlockForMinutes(timeoutMinutes)
    }

    fun lockSession() {
        sessionManager.lockNow()
    }

    suspend fun resolveCredential(
        appPackage: String,
        fieldHint: String?,
        accountHint: String?
    ): ResolvedCredential? {
        if (!isSessionUnlocked()) {
            return null
        }

        val candidates = automationDao.findCredentialsByPackage(appPackage)
        if (candidates.isEmpty()) return null

        val normalizedFieldHint = fieldHint?.trim()?.lowercase()
        val normalizedAccountHint = accountHint?.trim()?.lowercase()

        val matched = candidates.firstOrNull { candidate ->
            val fieldMatch = if (normalizedFieldHint.isNullOrEmpty()) {
                true
            } else {
                candidate.fieldHint?.lowercase()?.contains(normalizedFieldHint) == true
            }
            val accountMatch = if (normalizedAccountHint.isNullOrEmpty()) {
                true
            } else {
                candidate.accountLabel.lowercase().contains(normalizedAccountHint) ||
                    (candidate.username?.lowercase()?.contains(normalizedAccountHint) == true)
            }
            fieldMatch && accountMatch
        } ?: candidates.first()

        val password = try {
            cipherManager.decrypt(matched.passwordCiphertext)
        } catch (_: Exception) {
            return null
        }
        return ResolvedCredential(
            id = matched.id,
            appPackage = matched.appPackage,
            fieldHint = matched.fieldHint,
            accountLabel = matched.accountLabel,
            username = matched.username,
            password = password
        )
    }
}
