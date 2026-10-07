package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ConversationEntity
import com.example.data.local.MemoryEntity
import com.example.data.local.MessageEntity
import com.example.engine.MemoryRetriever
import com.example.engine.PromptBuilder
import com.example.engine.ToolCallRequest
import com.example.engine.ToolRouter
import com.example.model.ConnectionStatus
import com.example.model.ModelInfo
import com.example.model.ModelStatusState
import com.example.model.RuntimeConfig
import com.example.repository.DiyaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DiyaScreen {
    CHAT,
    CONVERSATIONS,
    MEMORY,
    MODELS,
    SETTINGS
}

data class DiyaUiState(
    val currentScreen: DiyaScreen = DiyaScreen.CHAT,
    val selectedConversationId: String? = null,
    val currentMessages: List<MessageEntity> = emptyList(),
    val connectionStatus: ConnectionStatus = ConnectionStatus(state = ModelStatusState.CONNECTING),
    val runtimeConfig: RuntimeConfig = RuntimeConfig(),
    val discoveredModels: List<ModelInfo> = emptyList(),
    val isGenerating: Boolean = false,
    val streamingChunk: String = "",
    val activePendingToolCall: ToolCallRequest? = null,
    val searchQuery: String = "",
    val searchResults: Pair<List<MessageEntity>, List<MemoryEntity>>? = null,
    val infoBanner: String? = null,
    val isRunningDiagnostics: Boolean = false
)

class DiyaViewModel(
    private val repository: DiyaRepository,
    private val promptBuilder: PromptBuilder = PromptBuilder(),
    private val memoryRetriever: MemoryRetriever = MemoryRetriever(),
    private val toolRouter: ToolRouter = ToolRouter()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiyaUiState())
    val uiState: StateFlow<DiyaUiState> = _uiState.asStateFlow()

    val conversations: StateFlow<List<ConversationEntity>> = repository.allConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var activeGenerationJob: Job? = null
    private var healthPollJob: Job? = null

    init {
        // Load persistent settings from database first, then verify health
        viewModelScope.launch {
            val savedConfig = repository.loadSavedConfig()
            _uiState.value = _uiState.value.copy(runtimeConfig = savedConfig)
            checkConnectionHealth()
            startHealthPolling()
            loadInitialConversation()
        }
    }

    private fun loadInitialConversation() {
        viewModelScope.launch {
            delay(150)
            val convs = conversations.value
            if (convs.isNotEmpty()) {
                selectConversation(convs.first().id)
            }
        }
    }

    private fun startHealthPolling() {
        healthPollJob?.cancel()
        healthPollJob = viewModelScope.launch {
            while (true) {
                delay(12000)
                if (!_uiState.value.isGenerating && !_uiState.value.isRunningDiagnostics) {
                    val status = repository.checkHealth(_uiState.value.runtimeConfig)
                    _uiState.value = _uiState.value.copy(connectionStatus = status)
                }
            }
        }
    }

    fun checkConnectionHealth() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                connectionStatus = _uiState.value.connectionStatus.copy(state = ModelStatusState.CONNECTING)
            )
            val status = repository.runDiagnostics(_uiState.value.runtimeConfig)
            val models = repository.discoverModels(_uiState.value.runtimeConfig)
            _uiState.value = _uiState.value.copy(
                connectionStatus = status,
                discoveredModels = models
            )
        }
    }

    fun runDiagnostics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isRunningDiagnostics = true,
                connectionStatus = _uiState.value.connectionStatus.copy(state = ModelStatusState.CONNECTING)
            )
            val status = repository.runDiagnostics(_uiState.value.runtimeConfig)
            _uiState.value = _uiState.value.copy(
                isRunningDiagnostics = false,
                connectionStatus = status
            )
        }
    }

    fun navigateTo(screen: DiyaScreen) {
        _uiState.value = _uiState.value.copy(currentScreen = screen)
    }

    fun selectConversation(conversationId: String) {
        _uiState.value = _uiState.value.copy(
            selectedConversationId = conversationId,
            currentScreen = DiyaScreen.CHAT,
            streamingChunk = ""
        )
        observeMessages(conversationId)
    }

    private fun observeMessages(conversationId: String) {
        viewModelScope.launch {
            repository.getMessagesForConversation(conversationId).collect { msgList ->
                if (_uiState.value.selectedConversationId == conversationId) {
                    _uiState.value = _uiState.value.copy(currentMessages = msgList)
                }
            }
        }
    }

    fun createNewChat() {
        viewModelScope.launch {
            val newId = repository.createConversation("New Conversation")
            selectConversation(newId)
        }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch {
            repository.deleteConversation(id)
            if (_uiState.value.selectedConversationId == id) {
                val remaining = conversations.value.filter { it.id != id }
                if (remaining.isNotEmpty()) {
                    selectConversation(remaining.first().id)
                } else {
                    _uiState.value = _uiState.value.copy(
                        selectedConversationId = null,
                        currentMessages = emptyList()
                    )
                }
            }
        }
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            // If no active conversation, create one now
            var convId = _uiState.value.selectedConversationId
            if (convId == null) {
                convId = repository.createConversation("New Conversation")
                selectConversation(convId)
            }

            // Save user message to database
            repository.insertMessage(convId, "user", trimmed)

            // Automatic memory extraction (if enabled)
            if (_uiState.value.runtimeConfig.memoryExtractionEnabled) {
                val extracted = memoryRetriever.extractLearnedFact(trimmed)
                if (extracted != null) {
                    val (key, value) = extracted
                    repository.insertMemory(key, value, "Personal", 3)
                }
            }

            // Execute real generation
            startRealInference(convId, trimmed)
        }
    }

    private fun startRealInference(convId: String, currentMessage: String) {
        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                streamingChunk = "",
                connectionStatus = _uiState.value.connectionStatus.copy(state = ModelStatusState.GENERATING)
            )

            try {
                // Fetch context snapshot
                val recentMessages = repository.getMessagesSnapshot(convId)
                val allMemories = repository.getAllMemoriesSnapshot()

                // Assemble prompt with budgeted context & relevant memories
                val prompt = promptBuilder.build(
                    currentUserMessage = currentMessage,
                    recentMessages = recentMessages,
                    allStoredMemories = allMemories,
                    config = _uiState.value.runtimeConfig
                )

                // Track used memories
                prompt.retrievedMemories.forEach { mem ->
                    repository.markMemoryUsed(mem.id)
                }

                val accumulatedResponse = StringBuilder()

                // Real streaming from llama-server or Diya local bridge
                repository.streamChat(
                    systemPrompt = prompt.systemPrompt,
                    messages = prompt.conversationHistory,
                    config = _uiState.value.runtimeConfig
                ).catch { error ->
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        streamingChunk = "",
                        connectionStatus = _uiState.value.connectionStatus.copy(
                            state = ModelStatusState.OFFLINE,
                            errorMessage = error.localizedMessage ?: "Inference connection error"
                        )
                    )
                    // Persist error message into conversation so user sees diagnostics
                    repository.insertMessage(
                        convId,
                        "assistant",
                        "Local AI is offline.\n\nReason: ${error.localizedMessage ?: "llama.cpp connection failed"}\n\nPlease verify that the Diya Bridge (${_uiState.value.runtimeConfig.bridgeUrl}) or llama-server (${_uiState.value.runtimeConfig.serverUrl}) is running."
                    )
                }.collect { token ->
                    accumulatedResponse.append(token)
                    _uiState.value = _uiState.value.copy(streamingChunk = accumulatedResponse.toString())
                }

                // Finish generation
                val finalContent = accumulatedResponse.toString().trim()
                if (finalContent.isNotEmpty()) {
                    // Check for potential tool call
                    val potentialTool = toolRouter.parsePotentialToolCall(finalContent)
                    if (potentialTool != null) {
                        _uiState.value = _uiState.value.copy(activePendingToolCall = potentialTool)
                    }
                    repository.insertMessage(convId, "assistant", finalContent)
                }

                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    streamingChunk = "",
                    connectionStatus = _uiState.value.connectionStatus.copy(state = ModelStatusState.READY)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    streamingChunk = "",
                    connectionStatus = _uiState.value.connectionStatus.copy(
                        state = ModelStatusState.ERROR,
                        errorMessage = e.localizedMessage
                    )
                )
            }
        }
    }

    fun stopGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null

        val currentStream = _uiState.value.streamingChunk.trim()
        val convId = _uiState.value.selectedConversationId

        if (currentStream.isNotEmpty() && convId != null) {
            viewModelScope.launch {
                repository.insertMessage(convId, "assistant", "$currentStream [Stopped]")
            }
        }

        _uiState.value = _uiState.value.copy(
            isGenerating = false,
            streamingChunk = "",
            connectionStatus = _uiState.value.connectionStatus.copy(state = ModelStatusState.READY)
        )
    }

    fun confirmToolAction(confirmed: Boolean) {
        val request = _uiState.value.activePendingToolCall ?: return
        val convId = _uiState.value.selectedConversationId ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(activePendingToolCall = null)
            val result = toolRouter.executeTool(request, confirmed)
            val toolMessage = if (result.success) {
                "Tool '${result.toolName}' result: ${result.result}"
            } else {
                "Tool execution failed: ${result.error}"
            }
            repository.insertMessage(convId, "tool", toolMessage)
        }
    }

    fun addMemory(key: String, value: String, category: String, importance: Int) {
        viewModelScope.launch {
            repository.insertMemory(key, value, category, importance)
        }
    }

    fun updateMemory(memory: MemoryEntity) {
        viewModelScope.launch {
            repository.updateMemory(memory)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    fun updateConfig(newConfig: RuntimeConfig) {
        viewModelScope.launch {
            repository.saveConfig(newConfig)
            _uiState.value = _uiState.value.copy(runtimeConfig = newConfig)
            checkConnectionHealth()
        }
    }

    fun selectModel(model: ModelInfo) {
        val updated = _uiState.value.runtimeConfig.copy(
            activeModelName = model.name,
            activeModelPath = model.path
        )
        updateConfig(updated)
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            repository.clearAllConversations()
            _uiState.value = _uiState.value.copy(
                selectedConversationId = null,
                currentMessages = emptyList()
            )
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            repository.clearAllMemories()
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = null)
            return
        }
        viewModelScope.launch {
            val results = repository.searchContent(query)
            _uiState.value = _uiState.value.copy(searchResults = results)
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeGenerationJob?.cancel()
        healthPollJob?.cancel()
    }
}

class DiyaViewModelFactory(
    private val repository: DiyaRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DiyaViewModel(repository) as T
    }
}
