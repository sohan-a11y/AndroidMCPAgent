package com.android.ai.mcp.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.ai.mcp.domain.AiProvider
import com.android.ai.mcp.domain.AppSettings
import com.android.ai.mcp.domain.WakeScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "mcp_app_settings")

class SettingsRepository(
    private val context: Context
) {

    companion object {
        private val KEY_SELECTED_PROVIDER = stringPreferencesKey("selected_provider")
        private val KEY_MAX_PLAN_STEPS = intPreferencesKey("max_plan_steps")
        private val KEY_STEP_DELAY_MS = intPreferencesKey("step_delay_ms")
        private val KEY_OPENROUTER_MODEL_ID = stringPreferencesKey("openrouter_model_id")
        private val KEY_NVIDIA_MODEL_ID = stringPreferencesKey("nvidia_model_id")
        private val KEY_WAKE_WORD = stringPreferencesKey("wake_word")
        private val KEY_WAKE_ENABLED = booleanPreferencesKey("wake_enabled")
        private val KEY_WAKE_SCOPE = stringPreferencesKey("wake_scope")
        private val KEY_VAULT_SESSION_TIMEOUT_MINUTES = intPreferencesKey("vault_session_timeout_minutes")
    }

    fun getSettings(): Flow<AppSettings> {
        return context.dataStore.data
            .catch { throwable ->
                if (throwable is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw throwable
                }
            }
            .map { prefs -> prefs.toAppSettings() }
    }

    suspend fun setSelectedProvider(provider: AiProvider) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_PROVIDER] = provider.value
        }
    }

    suspend fun setMaxPlanSteps(value: Int) {
        val safeValue = AppSettings.sanitizeMaxPlanSteps(value)
        context.dataStore.edit { prefs ->
            prefs[KEY_MAX_PLAN_STEPS] = safeValue
        }
    }

    suspend fun setStepDelayMs(value: Int) {
        val safeValue = AppSettings.sanitizeStepDelayMs(value)
        context.dataStore.edit { prefs ->
            prefs[KEY_STEP_DELAY_MS] = safeValue
        }
    }

    suspend fun setOpenRouterModelId(value: String) {
        val safeValue = AppSettings.sanitizeModelId(value, AppSettings.DEFAULT_OPENROUTER_MODEL_ID)
        context.dataStore.edit { prefs ->
            prefs[KEY_OPENROUTER_MODEL_ID] = safeValue
        }
    }

    suspend fun setNvidiaModelId(value: String) {
        val safeValue = AppSettings.sanitizeModelId(value, AppSettings.DEFAULT_NVIDIA_MODEL_ID)
        context.dataStore.edit { prefs ->
            prefs[KEY_NVIDIA_MODEL_ID] = safeValue
        }
    }

    suspend fun setWakeWord(value: String) {
        val safeValue = AppSettings.sanitizeWakeWord(value)
        context.dataStore.edit { prefs ->
            prefs[KEY_WAKE_WORD] = safeValue
        }
    }

    suspend fun setWakeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WAKE_ENABLED] = enabled
        }
    }

    suspend fun setWakeScope(scope: WakeScope) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WAKE_SCOPE] = scope.value
        }
    }

    suspend fun setVaultSessionTimeoutMinutes(value: Int) {
        val safeValue = AppSettings.sanitizeVaultSessionTimeoutMinutes(value)
        context.dataStore.edit { prefs ->
            prefs[KEY_VAULT_SESSION_TIMEOUT_MINUTES] = safeValue
        }
    }

    private fun Preferences.toAppSettings(): AppSettings {
        val selectedProvider = AiProvider.fromValue(this[KEY_SELECTED_PROVIDER])
        val maxPlanSteps = AppSettings.sanitizeMaxPlanSteps(
            this[KEY_MAX_PLAN_STEPS] ?: AppSettings.DEFAULT_MAX_PLAN_STEPS
        )
        val stepDelayMs = AppSettings.sanitizeStepDelayMs(
            this[KEY_STEP_DELAY_MS] ?: AppSettings.DEFAULT_STEP_DELAY_MS
        )
        val openRouterModelId = AppSettings.sanitizeModelId(
            this[KEY_OPENROUTER_MODEL_ID],
            AppSettings.DEFAULT_OPENROUTER_MODEL_ID
        )
        val nvidiaModelId = AppSettings.sanitizeModelId(
            this[KEY_NVIDIA_MODEL_ID],
            AppSettings.DEFAULT_NVIDIA_MODEL_ID
        )
        val wakeWord = AppSettings.sanitizeWakeWord(this[KEY_WAKE_WORD])
        val wakeEnabled = this[KEY_WAKE_ENABLED] ?: AppSettings.DEFAULT_WAKE_ENABLED
        val wakeScope = WakeScope.fromValue(this[KEY_WAKE_SCOPE])
        val vaultSessionTimeoutMinutes = AppSettings.sanitizeVaultSessionTimeoutMinutes(
            this[KEY_VAULT_SESSION_TIMEOUT_MINUTES]
                ?: AppSettings.DEFAULT_VAULT_SESSION_TIMEOUT_MINUTES
        )

        return AppSettings(
            selectedProvider = selectedProvider,
            maxPlanSteps = maxPlanSteps,
            stepDelayMs = stepDelayMs,
            openRouterModelId = openRouterModelId,
            nvidiaModelId = nvidiaModelId,
            wakeWord = wakeWord,
            wakeEnabled = wakeEnabled,
            wakeScope = wakeScope,
            vaultSessionTimeoutMinutes = vaultSessionTimeoutMinutes
        )
    }
}
