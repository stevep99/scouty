package io.github.stevep99.scouty.conversation

interface ConversationManager {
    suspend fun sendMessage(userMessage: String): String
    fun getHistory(): List<Pair<String, String>>
    fun clearHistory()
}
