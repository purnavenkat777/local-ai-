package com.example.model

data class ModelInfo(
    val name: String,
    val path: String,
    val sizeBytes: Long = 0L,
    val isAvailable: Boolean = true,
    val format: String = "GGUF"
)

enum class ModelStatusState {
    OFFLINE,
    CONNECTING,
    LOADING,
    READY,
    GENERATING,
    ERROR
}

data class DiagnosticReport(
    val androidToBridgeOk: Boolean = false,
    val androidToBridgeMessage: String = "Not tested",
    val bridgeToLlamaOk: Boolean = false,
    val bridgeToLlamaMessage: String = "Not tested",
    val modelAvailable: Boolean = false,
    val modelName: String = "None",
    val inferenceReady: Boolean = false,
    val testedUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class ConnectionStatus(
    val state: ModelStatusState = ModelStatusState.OFFLINE,
    val runtime: String = "llama.cpp",
    val modelName: String = "Not selected",
    val modelPath: String = "",
    val isModelLoaded: Boolean = false,
    val errorMessage: String? = null,
    val serverUrl: String = "http://127.0.0.1:8080",
    val bridgeUrl: String = "http://127.0.0.1:8787",
    val activeMode: String = "bridge", // "bridge" or "llama-server"
    val lastReport: DiagnosticReport? = null
)

data class RuntimeConfig(
    val serverUrl: String = "http://127.0.0.1:8080",
    val bridgeUrl: String = "http://127.0.0.1:8787",
    val activeModelName: String = "diya.gguf",
    val activeModelPath: String = "~/models/diya.gguf",
    val contextSize: Int = 4096,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1024,
    val threads: Int = 4,
    val memoryExtractionEnabled: Boolean = true,
    val systemPromptTemplate: String = """You are Diya, Venkat's personal AI assistant.

Be friendly, calm, practical, direct and natural.
Talk like a trusted personal assistant rather than a generic chatbot.
Keep simple answers short. Expand when useful.
Do not use meaningless filler such as:
"Certainly!"
"Absolutely!"
"Great question!"
"I'd be happy to help!"

Do not invent facts.
Do not invent memories.
If you do not know something, say so.

If a technical problem occurs:
1. Explain the likely cause.
2. Give one useful diagnostic step.
3. Give the fix.
4. Explain how to verify the fix.

Prefer free, local, offline and open-source solutions when they meet the user's needs.
Respect the user's limited budget.
Avoid unnecessary cloud services and subscriptions.
When giving commands, make clear where the command should be run.
When helping build software, prioritize real working functionality over mock data or impressive-looking placeholders.
Ask a clarification question only when it is genuinely necessary.
When enough information is available, make a reasonable assumption and continue.
Never claim that an action was performed unless the application actually performed it."""
)
