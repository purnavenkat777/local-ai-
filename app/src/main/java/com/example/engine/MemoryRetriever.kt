package com.example.engine

import com.example.data.local.MemoryEntity
import java.util.Locale

class MemoryRetriever {

    /**
     * Given user query and all stored memories, selects only memories relevant
     * to the user's intent. Never injects all memories blindly to avoid token waste.
     */
    fun retrieveRelevantMemories(
        userMessage: String,
        allMemories: List<MemoryEntity>,
        maxMemories: Int = 4
    ): List<MemoryEntity> {
        if (allMemories.isEmpty() || userMessage.isBlank()) return emptyList()

        val tokens = tokenize(userMessage)
        if (tokens.isEmpty()) return emptyList()

        val scored = allMemories.map { memory ->
            val memTokens = tokenize("${memory.key} ${memory.value} ${memory.category}")
            var matchCount = 0
            for (t in tokens) {
                if (memTokens.contains(t) || memTokens.any { it.contains(t) || t.contains(it) }) {
                    matchCount++
                }
            }

            // Weighted score combining lexical overlap and base importance
            val score = matchCount * 10 + memory.importance
            memory to score
        }

        // Only take memories that actually have matches (score > importance alone)
        return scored
            .filter { (_, score) -> score > 5 }
            .sortedByDescending { it.second }
            .take(maxMemories)
            .map { it.first }
    }

    /**
     * Extracts potential long-term personal facts from user message.
     * Selectively filters out commands, short greetings, and transient questions.
     */
    fun extractLearnedFact(userMessage: String): Pair<String, String>? {
        val trimmed = userMessage.trim()
        val lower = trimmed.lowercase(Locale.ROOT)

        // Negative filters: ignore questions, transient commands, credentials
        if (lower.startsWith("how ") || lower.startsWith("what ") || lower.startsWith("why ") ||
            lower.startsWith("where ") || lower.startsWith("when ") || lower.startsWith("who ") ||
            lower.contains("password") || lower.contains("token") || lower.contains("secret") ||
            lower.contains("api key") || trimmed.endsWith("?")) {
            return null
        }

        // Positive patterns
        // e.g. "my project is called X", "i prefer X", "i am learning X", "i work with X"
        val projectMatch = Regex("(?i)my project (?:is|called)\\s+([a-zA-Z0-9_ -]+)").find(trimmed)
        if (projectMatch != null) {
            val name = projectMatch.groupValues[1].trim()
            return "Project" to "User's project is called $name"
        }

        val learnMatch = Regex("(?i)i(?:'m| am) (?:learning|studying)\\s+([a-zA-Z0-9_#+ -]+)").find(trimmed)
        if (learnMatch != null) {
            val subject = learnMatch.groupValues[1].trim()
            return "Learning Interest" to "User is learning $subject"
        }

        val preferMatch = Regex("(?i)i prefer\\s+([a-zA-Z0-9_ -]+)").find(trimmed)
        if (preferMatch != null) {
            val preference = preferMatch.groupValues[1].trim()
            return "Preference" to "User prefers $preference"
        }

        val livingMatch = Regex("(?i)i live in\\s+([a-zA-Z0-9_ -]+)").find(trimmed)
        if (livingMatch != null) {
            val location = livingMatch.groupValues[1].trim()
            return "Location" to "User lives in $location"
        }

        return null
    }

    private fun tokenize(text: String): Set<String> {
        val stopWords = setOf(
            "a", "an", "the", "and", "or", "but", "if", "because", "as", "what",
            "which", "this", "that", "these", "those", "then", "just", "so", "than",
            "such", "both", "through", "about", "for", "is", "of", "while", "during",
            "to", "from", "in", "out", "on", "off", "again", "further", "then", "once",
            "here", "there", "when", "where", "why", "how", "all", "any", "both", "each",
            "few", "more", "most", "other", "some", "such", "no", "nor", "not", "only",
            "own", "same", "so", "than", "too", "very", "can", "will", "just", "don",
            "should", "now", "are", "was", "were", "been", "have", "has", "had", "do", "does", "did"
        )
        return text.lowercase(Locale.ROOT)
            .split(Regex("[^a-zA-Z0-9+#_]+"))
            .filter { it.length > 2 && !stopWords.contains(it) }
            .toSet()
    }
}
