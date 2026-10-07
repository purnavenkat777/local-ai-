package com.example.repository

import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import com.example.data.local.MemoryDao
import com.example.data.local.MemoryEntity
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import com.example.data.local.SettingDao
import com.example.data.local.SettingEntity
import com.example.model.ConnectionStatus
import com.example.model.ModelInfo
import com.example.model.RuntimeConfig
import com.example.network.LlamaClient
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class DiyaRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val memoryDao: MemoryDao,
    private val settingDao: SettingDao,
    private val llamaClient: LlamaClient = LlamaClient()
) {

    val allConversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()
    val allMemories: Flow<List<MemoryEntity>> = memoryDao.getAllMemories()

    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForConversation(conversationId)

    suspend fun getMessagesSnapshot(conversationId: String): List<MessageEntity> =
        messageDao.getMessagesSnapshot(conversationId)

    suspend fun getAllMemoriesSnapshot(): List<MemoryEntity> =
        memoryDao.getAllMemoriesSnapshot()

    suspend fun createConversation(title: String = "New Conversation"): String {
        val id = UUID.randomUUID().toString()
        val conv = ConversationEntity(
            id = id,
            title = title,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        conversationDao.insertOrUpdate(conv)
        return id
    }

    suspend fun updateConversationTitle(id: String, title: String) {
        val existing = conversationDao.getConversationById(id)
        if (existing != null) {
            conversationDao.insertOrUpdate(existing.copy(title = title, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteConversation(id: String) {
        messageDao.deleteMessagesForConversation(id)
        conversationDao.deleteById(id)
    }

    suspend fun insertMessage(conversationId: String, role: String, content: String): String {
        val messageId = UUID.randomUUID().toString()
        val msg = MessageEntity(
            id = messageId,
            conversationId = conversationId,
            role = role,
            content = content,
            createdAt = System.currentTimeMillis()
        )
        messageDao.insertMessage(msg)

        val conv = conversationDao.getConversationById(conversationId)
        if (conv != null) {
            val updatedTitle = if (conv.title == "New Conversation" && role == "user") {
                if (content.length > 30) content.take(30) + "..." else content
            } else conv.title
            conversationDao.insertOrUpdate(conv.copy(title = updatedTitle, updatedAt = System.currentTimeMillis()))
        }
        return messageId
    }

    suspend fun updateMessage(message: MessageEntity) {
        messageDao.insertMessage(message)
    }

    suspend fun deleteMessage(id: String) {
        messageDao.deleteMessageById(id)
    }

    suspend fun insertMemory(key: String, value: String, category: String, importance: Int = 3): Long {
        return memoryDao.insertMemory(
            MemoryEntity(
                key = key,
                value = value,
                category = category,
                importance = importance,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateMemory(memory: MemoryEntity) {
        memoryDao.updateMemory(memory.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteMemory(id: Long) {
        memoryDao.deleteMemoryById(id)
    }

    suspend fun markMemoryUsed(id: Long) {
        memoryDao.markLastUsed(id, System.currentTimeMillis())
    }

    suspend fun clearAllConversations() {
        messageDao.clearAll()
        conversationDao.clearAll()
    }

    suspend fun clearAllMemories() {
        memoryDao.clearAll()
    }

    suspend fun loadSavedConfig(): RuntimeConfig {
        val bridgeUrl = settingDao.getSetting("bridge_url") ?: "http://127.0.0.1:8787"
        val serverUrl = settingDao.getSetting("server_url") ?: "http://127.0.0.1:8080"
        val modelName = settingDao.getSetting("model_name") ?: "diya.gguf"
        val modelPath = settingDao.getSetting("model_path") ?: "~/models/diya.gguf"
        val contextSize = settingDao.getSetting("context_size")?.toIntOrNull() ?: 4096
        val temperature = settingDao.getSetting("temperature")?.toFloatOrNull() ?: 0.7f
        val maxTokens = settingDao.getSetting("max_tokens")?.toIntOrNull() ?: 1024
        val threads = settingDao.getSetting("threads")?.toIntOrNull() ?: 4
        val memExtraction = settingDao.getSetting("mem_extraction")?.toBooleanStrictOrNull() ?: true

        return RuntimeConfig(
            bridgeUrl = bridgeUrl,
            serverUrl = serverUrl,
            activeModelName = modelName,
            activeModelPath = modelPath,
            contextSize = contextSize,
            temperature = temperature,
            maxTokens = maxTokens,
            threads = threads,
            memoryExtractionEnabled = memExtraction
        )
    }

    suspend fun saveConfig(config: RuntimeConfig) {
        settingDao.setSetting(SettingEntity("bridge_url", config.bridgeUrl))
        settingDao.setSetting(SettingEntity("server_url", config.serverUrl))
        settingDao.setSetting(SettingEntity("model_name", config.activeModelName))
        settingDao.setSetting(SettingEntity("model_path", config.activeModelPath))
        settingDao.setSetting(SettingEntity("context_size", config.contextSize.toString()))
        settingDao.setSetting(SettingEntity("temperature", config.temperature.toString()))
        settingDao.setSetting(SettingEntity("max_tokens", config.maxTokens.toString()))
        settingDao.setSetting(SettingEntity("threads", config.threads.toString()))
        settingDao.setSetting(SettingEntity("mem_extraction", config.memoryExtractionEnabled.toString()))
    }

    suspend fun checkHealth(config: RuntimeConfig): ConnectionStatus {
        return llamaClient.checkHealth(config)
    }

    suspend fun runDiagnostics(config: RuntimeConfig): ConnectionStatus {
        return llamaClient.runDiagnostics(config)
    }

    suspend fun discoverModels(config: RuntimeConfig): List<ModelInfo> {
        return llamaClient.discoverModels(config)
    }

    fun streamChat(
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        config: RuntimeConfig
    ): Flow<String> {
        return llamaClient.streamChat(systemPrompt, messages, config)
    }

    suspend fun searchContent(query: String): Pair<List<MessageEntity>, List<MemoryEntity>> {
        val msgs = messageDao.searchMessages(query)
        val mems = memoryDao.searchMemories(query)
        return msgs to mems
    }
}
