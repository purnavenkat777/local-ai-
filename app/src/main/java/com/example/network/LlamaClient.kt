package com.example.network

import com.example.model.ConnectionStatus
import com.example.model.DiagnosticReport
import com.example.model.ModelInfo
import com.example.model.ModelStatusState
import com.example.model.RuntimeConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class LlamaClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Performs a 4-step real diagnostic sequence:
     * 1. Android -> Bridge /health
     * 2. Bridge -> /api/status (validating Bridge -> llama-server /health)
     * 3. Verify model loaded
     * 4. Return complete DiagnosticReport and ConnectionStatus
     */
    suspend fun runDiagnostics(config: RuntimeConfig): ConnectionStatus {
        val bridgeUrl = config.bridgeUrl.trimEnd('/')

        var androidToBridgeOk = false
        var androidToBridgeMsg = "Connection refused or unreachable"
        var bridgeToLlamaOk = false
        var bridgeToLlamaMsg = "Bridge could not reach llama-server"
        var modelLoaded = false
        var detectedModel = config.activeModelName

        // Step 1: Android -> Bridge /health
        try {
            val healthReq = Request.Builder()
                .url("$bridgeUrl/health")
                .get()
                .build()

            okHttpClient.newCall(healthReq).execute().use { res ->
                if (res.isSuccessful) {
                    androidToBridgeOk = true
                    androidToBridgeMsg = "Connected (HTTP ${res.code})"
                } else {
                    androidToBridgeMsg = "HTTP ${res.code} from Bridge"
                }
            }
        } catch (e: Exception) {
            androidToBridgeMsg = "Failed: ${e.message ?: "Connection error"}"
        }

        // If Bridge is reachable, run Step 2 & 3 via Bridge /api/status
        if (androidToBridgeOk) {
            try {
                val statusReq = Request.Builder()
                    .url("$bridgeUrl/api/status")
                    .get()
                    .build()

                okHttpClient.newCall(statusReq).execute().use { res ->
                    if (res.isSuccessful) {
                        val body = res.body?.string() ?: "{}"
                        val json = JSONObject(body)

                        val llamaObj = json.optJSONObject("llama")
                        if (llamaObj != null) {
                            bridgeToLlamaOk = llamaObj.optBoolean("reachable", false)
                            bridgeToLlamaMsg = if (bridgeToLlamaOk) {
                                "Connected (${llamaObj.optString("serverUrl", config.serverUrl)})"
                            } else {
                                llamaObj.optString("error", "llama-server offline")
                            }
                        } else {
                            // Fallback to top-level fields
                            val overallStatus = json.optString("status", "offline")
                            bridgeToLlamaOk = overallStatus.equals("ready", ignoreCase = true)
                            bridgeToLlamaMsg = if (bridgeToLlamaOk) "Connected" else json.optString("error", "llama-server offline")
                        }

                        modelLoaded = json.optBoolean("modelLoaded", false)
                        detectedModel = json.optString("model", config.activeModelName)
                    } else {
                        bridgeToLlamaMsg = "HTTP ${res.code} from Bridge /api/status"
                    }
                }
            } catch (e: Exception) {
                bridgeToLlamaMsg = "Error querying status: ${e.message}"
            }
        } else {
            // Bridge is down: check direct llama-server as fallback diagnostic
            val serverUrl = config.serverUrl.trimEnd('/')
            try {
                val directReq = Request.Builder()
                    .url("$serverUrl/health")
                    .get()
                    .build()
                okHttpClient.newCall(directReq).execute().use { res ->
                    if (res.isSuccessful) {
                        bridgeToLlamaOk = true
                        bridgeToLlamaMsg = "Direct llama-server reachable (Bridge was offline)"
                        modelLoaded = true
                    } else {
                        bridgeToLlamaMsg = "Direct llama-server returned HTTP ${res.code}"
                    }
                }
            } catch (e: Exception) {
                bridgeToLlamaMsg = "Direct llama-server check failed: ${e.message}"
            }
        }

        val inferenceReady = androidToBridgeOk && bridgeToLlamaOk && modelLoaded
        val state = when {
            inferenceReady -> ModelStatusState.READY
            androidToBridgeOk && !bridgeToLlamaOk -> ModelStatusState.OFFLINE
            !androidToBridgeOk && bridgeToLlamaOk -> ModelStatusState.READY // direct llama fallback mode
            else -> ModelStatusState.OFFLINE
        }

        val report = DiagnosticReport(
            androidToBridgeOk = androidToBridgeOk,
            androidToBridgeMessage = androidToBridgeMsg,
            bridgeToLlamaOk = bridgeToLlamaOk,
            bridgeToLlamaMessage = bridgeToLlamaMsg,
            modelAvailable = modelLoaded,
            modelName = detectedModel,
            inferenceReady = (state == ModelStatusState.READY),
            testedUrl = config.bridgeUrl
        )

        return ConnectionStatus(
            state = state,
            runtime = "llama.cpp",
            modelName = detectedModel,
            modelPath = config.activeModelPath,
            isModelLoaded = modelLoaded,
            errorMessage = if (state != ModelStatusState.READY) {
                if (!androidToBridgeOk) "Bridge unreachable at ${config.bridgeUrl} ($androidToBridgeMsg)"
                else "llama-server unreachable ($bridgeToLlamaMsg)"
            } else null,
            serverUrl = config.serverUrl,
            bridgeUrl = config.bridgeUrl,
            activeMode = if (androidToBridgeOk) "bridge" else "llama-server",
            lastReport = report
        )
    }

    /**
     * Checks health and updates status badge.
     */
    suspend fun checkHealth(config: RuntimeConfig): ConnectionStatus {
        return runDiagnostics(config)
    }

    /**
     * Streams tokens from Diya Bridge (/api/chat) or direct llama-server (/v1/chat/completions).
     * Yields progressive text chunks.
     * When cancelled by the caller coroutine, aborts OkHttp Call to cancel server generation!
     */
    fun streamChat(
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        config: RuntimeConfig
    ): Flow<String> = flow {
        val bridgeUrl = config.bridgeUrl.trimEnd('/')
        val serverUrl = config.serverUrl.trimEnd('/')

        // Determine active target URL: prioritize Bridge /api/chat, fallback to llama-server
        val (targetUrl, useBridgeFormat) = try {
            val probe = Request.Builder().url("$bridgeUrl/health").get().build()
            okHttpClient.newCall(probe).execute().use {
                if (it.isSuccessful) "$bridgeUrl/api/chat" to true
                else "$serverUrl/v1/chat/completions" to false
            }
        } catch (_: Exception) {
            "$serverUrl/v1/chat/completions" to false
        }

        val jsonArray = JSONArray()
        jsonArray.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })

        for ((role, content) in messages) {
            jsonArray.put(JSONObject().apply {
                put("role", role)
                put("content", content)
            })
        }

        val requestBodyJson = JSONObject().apply {
            put("messages", jsonArray)
            put("temperature", config.temperature)
            put("max_tokens", config.maxTokens)
            put("stream", true)
            put("model", config.activeModelName)
        }

        val request = Request.Builder()
            .url(targetUrl)
            .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Accept", "text/event-stream")
            .build()

        val call = okHttpClient.newCall(request)

        try {
            val response = call.execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "HTTP ${response.code}"
                response.close()
                throw IllegalStateException("Inference error (${response.code}): $errorBody")
            }

            val body = response.body ?: throw IllegalStateException("Empty response body from local inference server")
            val reader = BufferedReader(InputStreamReader(body.byteStream()))

            try {
                var line: String? = reader.readLine()
                while (line != null) {
                    val trimmed = line.trim()
                    if (trimmed.startsWith("data:")) {
                        val dataContent = trimmed.substring(5).trim()
                        if (dataContent == "[DONE]") {
                            break
                        }
                        runCatching {
                            val json = JSONObject(dataContent)
                            val choices = json.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val firstChoice = choices.getJSONObject(0)
                                val delta = firstChoice.optJSONObject("delta")
                                val content = delta?.optString("content") ?: ""
                                if (content.isNotEmpty()) {
                                    emit(content)
                                }
                            }
                        }
                    }
                    line = reader.readLine()
                }
            } finally {
                reader.close()
                response.close()
            }
        } finally {
            // Cancellation Propagation: If coroutine was cancelled (e.g. Stop pressed), cancel OkHttp call
            if (call.isExecuted() && !call.isCanceled()) {
                call.cancel()
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Discovers GGUF models via Bridge /api/models or direct /v1/models
     */
    suspend fun discoverModels(config: RuntimeConfig): List<ModelInfo> {
        val defaultModels = listOf(
            ModelInfo(name = "diya.gguf", path = "~/models/diya.gguf", isAvailable = true),
            ModelInfo(name = "qwen2.5-0.5b-q4.gguf", path = "~/models/qwen2.5-0.5b-q4.gguf", isAvailable = true)
        )

        val bridgeUrl = config.bridgeUrl.trimEnd('/')
        // 1. Try Bridge /api/models first
        try {
            val req = Request.Builder().url("$bridgeUrl/api/models").get().build()
            okHttpClient.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string() ?: ""
                    val json = JSONObject(body)
                    val arr = json.optJSONArray("models")
                    if (arr != null && arr.length() > 0) {
                        val list = mutableListOf<ModelInfo>()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            list.add(
                                ModelInfo(
                                    name = obj.optString("name"),
                                    path = obj.optString("path"),
                                    sizeBytes = obj.optLong("sizeBytes", 0L),
                                    isAvailable = obj.optBoolean("available", true),
                                    format = obj.optString("format", "GGUF")
                                )
                            )
                        }
                        if (list.isNotEmpty()) return list
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Try llama-server direct /v1/models
        val serverUrl = config.serverUrl.trimEnd('/')
        try {
            val req = Request.Builder().url("$serverUrl/v1/models").get().build()
            okHttpClient.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string() ?: ""
                    val json = JSONObject(body)
                    val data = json.optJSONArray("data")
                    if (data != null && data.length() > 0) {
                        val list = mutableListOf<ModelInfo>()
                        for (i in 0 until data.length()) {
                            val item = data.getJSONObject(i)
                            val id = item.optString("id", "model-$i")
                            list.add(ModelInfo(name = id, path = "~/models/$id", isAvailable = true))
                        }
                        if (list.isNotEmpty()) return list
                    }
                }
            }
        } catch (_: Exception) {}

        return defaultModels
    }
}
