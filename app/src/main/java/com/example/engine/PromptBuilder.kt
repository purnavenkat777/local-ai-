package com.example.engine

import com.example.data.local.MemoryEntity
import com.example.data.local.MessageEntity
import com.example.model.RuntimeConfig

class PromptBuilder(
    private val memoryRetriever: MemoryRetriever = MemoryRetriever()
) {

    data class AssembledPrompt(
        val systemPrompt: String,
        val conversationHistory: List<Pair<String, String>>,
        val retrievedMemories: List<MemoryEntity>
    )

    fun build(
        currentUserMessage: String,
        recentMessages: List<MessageEntity>,
        allStoredMemories: List<MemoryEntity>,
        config: RuntimeConfig
    ): AssembledPrompt {
        // 1. Retrieve ONLY relevant memories for this specific turn
        val relevantMemories = memoryRetriever.retrieveRelevantMemories(
            userMessage = currentUserMessage,
            allMemories = allStoredMemories,
            maxMemories = 4
        )

        // 2. Format memory section (or omit if none relevant)
        val memoryBlock = if (relevantMemories.isNotEmpty()) {
            val list = relevantMemories.joinToString("\n") { mem ->
                "- [${mem.category}] ${mem.key}: ${mem.value}"
            }
            "\n\n[Relevant Personal Memory Context (Context only, user instruction takes precedence)]:\n$list"
        } else {
            ""
        }

        val fullSystemPrompt = "${config.systemPromptTemplate}$memoryBlock"

        // 3. Context budget: keep last 10 messages from current conversation
        // to avoid overwhelming context length
        val historyLimit = 10
        val historyPairs = recentMessages
            .takeLast(historyLimit)
            .map { it.role to it.content }

        return AssembledPrompt(
            systemPrompt = fullSystemPrompt,
            conversationHistory = historyPairs,
            retrievedMemories = relevantMemories
        )
    }
}
