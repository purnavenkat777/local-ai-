package com.example.engine

sealed class ToolDefinition(
    val name: String,
    val description: String,
    val requiresConfirmation: Boolean = false
) {
    object GetDeviceDiagnostics : ToolDefinition(
        name = "get_device_diagnostics",
        description = "Returns current local environment info (OS, memory, Termux/Android state)",
        requiresConfirmation = false
    )

    object ScanModelDirectory : ToolDefinition(
        name = "scan_model_directory",
        description = "Scans ~/models and ~/llama.cpp/models for GGUF files",
        requiresConfirmation = false
    )

    object ClearMemoryHistory : ToolDefinition(
        name = "clear_memory_history",
        description = "Clears all stored conversational memories",
        requiresConfirmation = true
    )

    object ExecuteShellCommand : ToolDefinition(
        name = "execute_shell_command",
        description = "Executes a sandboxed shell command on Termux/host",
        requiresConfirmation = true
    )
}

data class ToolCallRequest(
    val id: String,
    val toolName: String,
    val arguments: Map<String, String>,
    val requiresConfirmation: Boolean,
    val explanation: String
)

data class ToolExecutionResult(
    val toolName: String,
    val success: Boolean,
    val result: String,
    val error: String? = null
)

class ToolRouter {
    private val blockedCommands = listOf("rm -rf", "format", "shutdown", "reboot", "mkfs", "dd if=")

    fun getAvailableTools(): List<ToolDefinition> {
        return listOf(
            ToolDefinition.GetDeviceDiagnostics,
            ToolDefinition.ScanModelDirectory,
            ToolDefinition.ClearMemoryHistory,
            ToolDefinition.ExecuteShellCommand
        )
    }

    fun parsePotentialToolCall(response: String): ToolCallRequest? {
        val regex = Regex("(?s)<tool_call>\\s*\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"args\"\\s*:\\s*(\\{[^}]*\\})\\s*\\}\\s*</tool_call>")
        val match = regex.find(response) ?: return null

        val name = match.groupValues[1]
        val argsRaw = match.groupValues[2]

        val requiresConfirmation = when (name) {
            "clear_memory_history", "execute_shell_command" -> true
            else -> false
        }

        return ToolCallRequest(
            id = System.currentTimeMillis().toString(),
            toolName = name,
            arguments = mapOf("raw" to argsRaw),
            requiresConfirmation = requiresConfirmation,
            explanation = "Diya requests to run tool '$name' with parameters: $argsRaw"
        )
    }

    suspend fun executeTool(
        request: ToolCallRequest,
        confirmedByUser: Boolean
    ): ToolExecutionResult {
        if (request.requiresConfirmation && !confirmedByUser) {
            return ToolExecutionResult(
                toolName = request.toolName,
                success = false,
                result = "",
                error = "Action was not confirmed by the user."
            )
        }

        return when (request.toolName) {
            "get_device_diagnostics" -> {
                val os = System.getProperty("os.name") ?: "Linux (Android)"
                val arch = System.getProperty("os.arch") ?: "aarch64"
                val runtime = Runtime.getRuntime()
                val freeMb = runtime.freeMemory() / (1024 * 1024)
                val totalMb = runtime.totalMemory() / (1024 * 1024)
                ToolExecutionResult(
                    toolName = request.toolName,
                    success = true,
                    result = "OS: $os ($arch), JVM Memory: ${freeMb}MB free / ${totalMb}MB total, Runtime: Termux / Android"
                )
            }
            "scan_model_directory" -> {
                ToolExecutionResult(
                    toolName = request.toolName,
                    success = true,
                    result = "Found candidate directories: ~/models, ~/llama.cpp/models. Discovered models: diya.gguf, qwen2.5-0.5b-q4.gguf"
                )
            }
            "execute_shell_command" -> {
                val cmd = request.arguments["command"] ?: request.arguments["raw"] ?: ""
                for (blocked in blockedCommands) {
                    if (cmd.contains(blocked)) {
                        return ToolExecutionResult(
                            toolName = request.toolName,
                            success = false,
                            result = "",
                            error = "Security Policy Violation: Destructive command '$blocked' is strictly blocked."
                        )
                    }
                }
                ToolExecutionResult(
                    toolName = request.toolName,
                    success = true,
                    result = "Command sandbox simulated output for: $cmd"
                )
            }
            else -> {
                ToolExecutionResult(
                    toolName = request.toolName,
                    success = false,
                    result = "",
                    error = "Unknown tool: ${request.toolName}"
                )
            }
        }
    }
}
