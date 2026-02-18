package com.android.ai.mcp.ai

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.android.ai.mcp.domain.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.math.BigDecimal

private val Context.modelCatalogStore by preferencesDataStore(name = "mcp_model_catalog")

class ModelCatalogRepository(
    private val context: Context,
    private val httpClient: OkHttpClient,
    private val json: Json
) {

    @Serializable
    data class CatalogModel(
        val id: String,
        val name: String
    )

    data class ModelValidationResult(
        val allowed: Boolean,
        val isVerified: Boolean,
        val message: String? = null
    )

    companion object {
        private const val OPENROUTER_MODELS_URL = "https://openrouter.ai/api/v1/models"
        private val KEY_OPENROUTER_FREE_MODELS_JSON = stringPreferencesKey("openrouter_free_models_json")
        private val KEY_OPENROUTER_CACHED_AT = longPreferencesKey("openrouter_cached_at")
    }

    suspend fun getCachedOpenRouterFreeModels(): List<CatalogModel> {
        return context.modelCatalogStore.data
            .map { it.toCachedModels() }
            .first()
    }

    suspend fun refreshOpenRouterFreeModels(apiKey: String?): List<CatalogModel> {
        return withContext(Dispatchers.IO) {
            val requestBuilder = Request.Builder()
                .url(OPENROUTER_MODELS_URL)
                .header("Accept", "application/json")
            if (!apiKey.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer ${apiKey.trim()}")
            }

            val request = requestBuilder
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IOException("OpenRouter model catalog request failed (${response.code})")
                }

                val freeModels = parseFreeModels(body)
                cacheModels(freeModels)
                freeModels
            }
        }
    }

    suspend fun validateOpenRouterModel(modelId: String, apiKey: String?): ModelValidationResult {
        val normalized = AppSettings.sanitizeModelId(
            modelId,
            AppSettings.DEFAULT_OPENROUTER_MODEL_ID
        )

        val cached = getCachedOpenRouterFreeModels()
        if (cached.any { it.id == normalized }) {
            return ModelValidationResult(
                allowed = true,
                isVerified = true
            )
        }

        val refreshed = try {
            refreshOpenRouterFreeModels(apiKey)
        } catch (_: Exception) {
            null
        }

        if (refreshed != null) {
            if (refreshed.any { it.id == normalized }) {
                return ModelValidationResult(
                    allowed = true,
                    isVerified = true
                )
            }
            return ModelValidationResult(
                allowed = false,
                isVerified = true,
                message = "OpenRouter model '$normalized' is not free in the latest catalog"
            )
        }

        if (normalized.endsWith(":free", ignoreCase = true)) {
            return ModelValidationResult(
                allowed = true,
                isVerified = false,
                message = "Model accepted with :free suffix (catalog unavailable, unverified)"
            )
        }

        return ModelValidationResult(
            allowed = false,
            isVerified = false,
            message = "Catalog unavailable. Only model IDs ending with :free are allowed."
        )
    }

    private suspend fun cacheModels(models: List<CatalogModel>) {
        val payload = json.encodeToString(models)
        context.modelCatalogStore.edit { prefs ->
            prefs[KEY_OPENROUTER_FREE_MODELS_JSON] = payload
            prefs[KEY_OPENROUTER_CACHED_AT] = System.currentTimeMillis()
        }
    }

    private fun parseFreeModels(rawJson: String): List<CatalogModel> {
        val root = json.parseToJsonElement(rawJson) as? JsonObject ?: return emptyList()
        val data = root["data"] as? JsonArray ?: return emptyList()

        return data.mapNotNull { entry ->
            val obj = entry as? JsonObject ?: return@mapNotNull null
            val id = (obj["id"] as? JsonPrimitive)?.contentOrNull?.trim().orEmpty()
            if (id.isEmpty()) return@mapNotNull null

            val pricing = obj["pricing"] as? JsonObject
            val promptPrice = pricing?.numericString("prompt")
            val completionPrice = pricing?.numericString("completion")
            val isFree = promptPrice?.isZeroPrice() == true && completionPrice?.isZeroPrice() == true
            if (!isFree) return@mapNotNull null

            val name = (obj["name"] as? JsonPrimitive)?.contentOrNull?.trim().takeUnless { it.isNullOrEmpty() }
                ?: id

            CatalogModel(id = id, name = name)
        }.sortedBy { it.id }
    }

    private fun Preferences.toCachedModels(): List<CatalogModel> {
        val encoded = this[KEY_OPENROUTER_FREE_MODELS_JSON] ?: return emptyList()
        return try {
            json.decodeFromString(encoded)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun JsonObject.numericString(key: String): String? {
        return (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun String.isZeroPrice(): Boolean {
        return try {
            BigDecimal(this) <= BigDecimal.ZERO
        } catch (_: Exception) {
            false
        }
    }
}
