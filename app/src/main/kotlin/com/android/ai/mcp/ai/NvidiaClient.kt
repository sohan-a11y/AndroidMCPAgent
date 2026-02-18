package com.android.ai.mcp.ai

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class NvidiaClient(
    httpClient: OkHttpClient,
    json: Json
) : BaseChatClient(httpClient, json) {

    override val endpointUrl: String = "https://integrate.api.nvidia.com/v1/chat/completions"
}
