package com.android.ai.mcp

import android.app.Application
import com.android.ai.mcp.ai.ActionPlanParser
import com.android.ai.mcp.ai.AiPlanner
import com.android.ai.mcp.ai.NvidiaClient
import com.android.ai.mcp.ai.OpenRouterClient
import com.android.ai.mcp.ai.PromptBuilder
import com.android.ai.mcp.execution.ActionExecutor
import com.android.ai.mcp.execution.ActionValidator
import com.android.ai.mcp.execution.UiActionPerformer
import com.android.ai.mcp.storage.SecureStore
import com.android.ai.mcp.storage.SettingsRepository
import com.android.ai.mcp.storage.logs.LogsDatabase
import com.android.ai.mcp.storage.logs.LogsRepository
import com.android.ai.mcp.system.ScreenContextReader
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class AndroidAiMcpApplication : Application() {

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    val secureStore by lazy { SecureStore(this) }

    val settingsRepository by lazy { SettingsRepository(this) }

    val logsRepository by lazy {
        val database = LogsDatabase.create(this)
        LogsRepository(database.logsDao(), json)
    }

    private val openRouterClient by lazy { OpenRouterClient(httpClient, json) }
    private val nvidiaClient by lazy { NvidiaClient(httpClient, json) }

    val aiPlanner by lazy {
        AiPlanner(
            openRouterClient = openRouterClient,
            nvidiaClient = nvidiaClient,
            promptBuilder = PromptBuilder(),
            actionPlanParser = ActionPlanParser(json)
        )
    }

    val actionValidator by lazy { ActionValidator() }

    val screenContextReader by lazy { ScreenContextReader() }

    private val uiActionPerformer by lazy {
        UiActionPerformer(
            context = this,
            screenContextReader = screenContextReader
        )
    }

    val actionExecutor by lazy {
        ActionExecutor(
            uiActionPerformer = uiActionPerformer,
            logsRepository = logsRepository
        )
    }
}
