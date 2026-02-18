package com.android.ai.mcp.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.ai.mcp.domain.AiProvider
import com.android.ai.mcp.domain.AppSettings
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

    private fun Preferences.toAppSettings(): AppSettings {
        val selectedProvider = AiProvider.fromValue(this[KEY_SELECTED_PROVIDER])
        val maxPlanSteps = AppSettings.sanitizeMaxPlanSteps(
            this[KEY_MAX_PLAN_STEPS] ?: AppSettings.DEFAULT_MAX_PLAN_STEPS
        )
        val stepDelayMs = AppSettings.sanitizeStepDelayMs(
            this[KEY_STEP_DELAY_MS] ?: AppSettings.DEFAULT_STEP_DELAY_MS
        )

        return AppSettings(
            selectedProvider = selectedProvider,
            maxPlanSteps = maxPlanSteps,
            stepDelayMs = stepDelayMs
        )
    }
}
