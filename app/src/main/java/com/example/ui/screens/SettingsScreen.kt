package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConnectionStatus
import com.example.model.DiagnosticReport
import com.example.model.RuntimeConfig
import com.example.ui.theme.DiyaAmberPrimary
import com.example.ui.theme.DiyaCyanAccent
import com.example.ui.theme.DiyaEmeraldGreen
import com.example.ui.theme.DiyaRoseError

@Composable
fun SettingsScreen(
    config: RuntimeConfig,
    status: ConnectionStatus,
    isRunningDiagnostics: Boolean,
    onSaveConfig: (RuntimeConfig) -> Unit,
    onClearConversations: () -> Unit,
    onClearMemories: () -> Unit,
    onRunDiagnostics: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var serverUrl by remember(config.serverUrl) { mutableStateOf(config.serverUrl) }
    var bridgeUrl by remember(config.bridgeUrl) { mutableStateOf(config.bridgeUrl) }
    var modelPath by remember(config.activeModelPath) { mutableStateOf(config.activeModelPath) }
    var contextSize by remember(config.contextSize) { mutableStateOf(config.contextSize.toString()) }
    var temperature by remember(config.temperature) { mutableStateOf(config.temperature.toString()) }
    var maxTokens by remember(config.maxTokens) { mutableStateOf(config.maxTokens.toString()) }
    var threads by remember(config.threads) { mutableStateOf(config.threads.toString()) }
    var memoryExtractionEnabled by remember(config.memoryExtractionEnabled) { mutableStateOf(config.memoryExtractionEnabled) }
    var showDeveloperSettings by remember { mutableStateOf(false) }

    var showClearConversationsDialog by remember { mutableStateOf(false) }
    var showClearMemoriesDialog by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("close_settings_button")) {
                    Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close")
                }
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = DiyaAmberPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = {
                        val newConfig = config.copy(
                            serverUrl = serverUrl.trim(),
                            bridgeUrl = bridgeUrl.trim(),
                            activeModelPath = modelPath.trim(),
                            contextSize = contextSize.toIntOrNull() ?: 4096,
                            temperature = temperature.toFloatOrNull() ?: 0.7f,
                            maxTokens = maxTokens.toIntOrNull() ?: 1024,
                            threads = threads.toIntOrNull() ?: 4,
                            memoryExtractionEnabled = memoryExtractionEnabled
                        )
                        onSaveConfig(newConfig)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiyaAmberPrimary),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("save_settings_button")
                ) {
                    Icon(imageVector = Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", fontSize = 12.sp)
                }
            }
        }

        // Settings Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            // Section: Diagnostics Panel
            Text(
                text = "DIYA DIAGNOSTICS",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = DiyaCyanAccent,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val report = status.lastReport

                    DiagnosticRow(
                        title = "Android → Bridge",
                        isSuccess = report?.androidToBridgeOk ?: false,
                        detail = report?.androidToBridgeMessage ?: "Tested against ${config.bridgeUrl}"
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    DiagnosticRow(
                        title = "Bridge → llama.cpp",
                        isSuccess = report?.bridgeToLlamaOk ?: false,
                        detail = report?.bridgeToLlamaMessage ?: "Testing against ${config.serverUrl}"
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    DiagnosticRow(
                        title = "Model (${status.modelName})",
                        isSuccess = report?.modelAvailable ?: status.isModelLoaded,
                        detail = if (report?.modelAvailable == true) "Available & ready for inference" else "Model file or runtime unavailable"
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    DiagnosticRow(
                        title = "Overall Status",
                        isSuccess = status.state.name == "READY",
                        detail = "● ${status.state.name}"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onRunDiagnostics,
                        enabled = !isRunningDiagnostics,
                        colors = ButtonDefaults.buttonColors(containerColor = DiyaAmberPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("check_connection_button")
                    ) {
                        if (isRunningDiagnostics) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing Connection...")
                        } else {
                            Icon(imageVector = Icons.Outlined.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Check Connection (${config.bridgeUrl})", fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Connection Endpoints
            Text(
                text = "CONNECTION & RUNTIME",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = DiyaCyanAccent,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = bridgeUrl,
                        onValueChange = { bridgeUrl = it },
                        label = { Text("Bridge URL (e.g. http://127.0.0.1:8787)") },
                        modifier = Modifier.fillMaxWidth().testTag("bridge_url_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = { serverUrl = it },
                        label = { Text("llama-server URL (e.g. http://127.0.0.1:8080)") },
                        modifier = Modifier.fillMaxWidth().testTag("server_url_input"),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Local AI Model
            Text(
                text = "LOCAL AI INFERENCE",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = DiyaCyanAccent,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    OutlinedTextField(
                        value = modelPath,
                        onValueChange = { modelPath = it },
                        label = { Text("GGUF Model Path (e.g. ~/models/diya.gguf)") },
                        modifier = Modifier.fillMaxWidth().testTag("model_path_input"),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = contextSize,
                            onValueChange = { contextSize = it },
                            label = { Text("Context Size") },
                            modifier = Modifier.weight(1f).testTag("context_size_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedTextField(
                            value = threads,
                            onValueChange = { threads = it },
                            label = { Text("CPU Threads") },
                            modifier = Modifier.weight(1f).testTag("threads_input"),
                            singleLine = true
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = temperature,
                            onValueChange = { temperature = it },
                            label = { Text("Temperature") },
                            modifier = Modifier.weight(1f).testTag("temperature_input"),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedTextField(
                            value = maxTokens,
                            onValueChange = { maxTokens = it },
                            label = { Text("Max Tokens") },
                            modifier = Modifier.weight(1f).testTag("max_tokens_input"),
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Memory Settings
            Text(
                text = "PERSISTENT MEMORY",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = DiyaCyanAccent,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Automatic Memory Extraction",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Extract durable personal preferences and project facts automatically from chat",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = memoryExtractionEnabled,
                        onCheckedChange = { memoryExtractionEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = DiyaAmberPrimary),
                        modifier = Modifier.testTag("memory_extraction_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Data & Storage
            Text(
                text = "DATA MANAGEMENT",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = DiyaCyanAccent,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Button(
                        onClick = { showClearConversationsDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().testTag("clear_conversations_button")
                    ) {
                        Icon(imageVector = Icons.Outlined.DeleteForever, contentDescription = null, tint = DiyaRoseError)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Conversations", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showClearMemoriesDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().testTag("clear_memories_button")
                    ) {
                        Icon(imageVector = Icons.Outlined.DeleteForever, contentDescription = null, tint = DiyaRoseError)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Persistent Memories", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Developer Diagnostics (Expandable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Outlined.DeveloperMode, contentDescription = null, tint = DiyaAmberPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Developer & Diagnostics",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = { showDeveloperSettings = !showDeveloperSettings },
                    modifier = Modifier.testTag("toggle_dev_settings")
                ) {
                    Text(if (showDeveloperSettings) "Hide" else "Show", fontSize = 11.sp)
                }
            }

            if (showDeveloperSettings) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Active Server URL: ${config.serverUrl}", fontSize = 11.sp)
                        Text(text = "Active Bridge URL: ${config.bridgeUrl}", fontSize = 11.sp)
                        Text(text = "Active Model: ${config.activeModelName}", fontSize = 11.sp)
                        Text(text = "Active Mode: ${status.activeMode}", fontSize = 11.sp)
                        Text(text = "Runtime: ${status.runtime}", fontSize = 11.sp)
                        Text(text = "Model Loaded: ${status.isModelLoaded}", fontSize = 11.sp)
                        if (status.errorMessage != null) {
                            Text(text = "Last Diagnostic Error: ${status.errorMessage}", fontSize = 11.sp, color = DiyaRoseError)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showClearConversationsDialog) {
        AlertDialog(
            onDismissRequest = { showClearConversationsDialog = false },
            title = { Text("Clear All Conversations?") },
            text = { Text("This will permanently delete all chat history from local SQLite storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearConversations()
                        showClearConversationsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiyaRoseError)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConversationsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showClearMemoriesDialog) {
        AlertDialog(
            onDismissRequest = { showClearMemoriesDialog = false },
            title = { Text("Clear All Memories?") },
            text = { Text("This will permanently reset all stored memories in the local database.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearMemories()
                        showClearMemoriesDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DiyaRoseError)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearMemoriesDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DiagnosticRow(
    title: String,
    isSuccess: Boolean,
    detail: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (isSuccess) "✓" else "✗",
            color = if (isSuccess) DiyaEmeraldGreen else DiyaRoseError,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = detail,
                fontSize = 11.sp,
                color = if (isSuccess) MaterialTheme.colorScheme.onSurfaceVariant else DiyaRoseError
            )
        }
    }
}
