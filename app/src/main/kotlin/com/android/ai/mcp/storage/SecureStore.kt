package com.android.ai.mcp.storage

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.android.ai.mcp.domain.AiProvider

class SecureStore(context: Context) {

    companion object {
        private const val FILE_NAME = "secure_mcp_store"
        private const val KEY_OPENROUTER = "openrouter_api_key"
        private const val KEY_NVIDIA = "nvidia_api_key"
    }

    private val prefs = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun setApiKey(provider: AiProvider, apiKey: String) {
        val keyName = when (provider) {
            AiProvider.OPENROUTER -> KEY_OPENROUTER
            AiProvider.NVIDIA -> KEY_NVIDIA
        }
        prefs.edit().putString(keyName, apiKey.trim()).apply()
    }

    fun getApiKey(provider: AiProvider): String? {
        val keyName = when (provider) {
            AiProvider.OPENROUTER -> KEY_OPENROUTER
            AiProvider.NVIDIA -> KEY_NVIDIA
        }
        return prefs.getString(keyName, null)?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun hasApiKey(provider: AiProvider): Boolean {
        return !getApiKey(provider).isNullOrEmpty()
    }
}
