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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ModelStatusState
import com.example.ui.DiyaScreen
import com.example.ui.DiyaUiState
import com.example.ui.components.MessageBubble
import com.example.ui.components.StatusBadge
import com.example.ui.components.ToolConfirmationCard
import com.example.ui.theme.DiyaAmberPrimary
import com.example.ui.theme.DiyaRoseError

@Composable
fun ChatScreen(
    state: DiyaUiState,
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit,
    onRetryHealth: () -> Unit,
    onOpenNav: () -> Unit,
    onNewChat: () -> Unit,
    onConfirmTool: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scroll to bottom on new messages or streaming tokens
    LaunchedEffect(state.currentMessages.size, state.streamingChunk) {
        if (state.currentMessages.isNotEmpty() || state.streamingChunk.isNotEmpty()) {
            val targetIndex = (state.currentMessages.size + if (state.streamingChunk.isNotEmpty()) 1 else 0) - 1
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

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
                IconButton(
                    onClick = onOpenNav,
                    modifier = Modifier.testTag("nav_menu_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Menu,
                        contentDescription = "Open navigation",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column {
                    Text(
                        text = "Diya",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = state.connectionStatus.modelName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                StatusBadge(
                    status = state.connectionStatus,
                    onRetryClick = onRetryHealth
                )

                IconButton(
                    onClick = onNewChat,
                    modifier = Modifier.testTag("top_new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "New Chat",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Offline Diagnostic Banner if Local AI is not reachable
        if (state.connectionStatus.state == ModelStatusState.OFFLINE || state.connectionStatus.state == ModelStatusState.ERROR) {
            Surface(
                color = DiyaRoseError.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DiyaRoseError.copy(alpha = 0.3f))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = DiyaRoseError,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Local AI is offline.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = state.connectionStatus.errorMessage
                                ?: "llama.cpp server is not reachable on ${state.runtimeConfig.serverUrl}.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("open_settings_from_banner")
                    ) {
                        Text("Settings", color = DiyaAmberPrimary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Pending Tool Confirmation Card
        if (state.activePendingToolCall != null) {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ToolConfirmationCard(
                    request = state.activePendingToolCall,
                    onConfirm = { onConfirmTool(true) },
                    onCancel = { onConfirmTool(false) }
                )
            }
        }

        // Conversation Message List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (state.currentMessages.isEmpty() && state.streamingChunk.isEmpty()) {
                // Empty state (per prompt: clean, minimal, no fake conversation)
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "D",
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = DiyaAmberPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Diya",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Local-first personal AI assistant",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Powered by llama.cpp and local GGUF models",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.currentMessages) { message ->
                        MessageBubble(
                            role = message.role,
                            content = message.content
                        )
                    }

                    if (state.streamingChunk.isNotEmpty()) {
                        item {
                            MessageBubble(
                                role = "assistant",
                                content = state.streamingChunk,
                                isStreaming = true
                            )
                        }
                    }
                }
            }
        }

        // Input Area
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (state.connectionStatus.state == ModelStatusState.OFFLINE)
                                "Local AI is offline (check server)..." else "Message Diya...",
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DiyaAmberPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    maxLines = 4
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (state.isGenerating) {
                    // Real Stop generation button
                    Button(
                        onClick = onStopGeneration,
                        colors = ButtonDefaults.buttonColors(containerColor = DiyaRoseError),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("stop_generation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Stop,
                            contentDescription = "Stop generation",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = DiyaAmberPrimary),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("send_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = "Send message",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
