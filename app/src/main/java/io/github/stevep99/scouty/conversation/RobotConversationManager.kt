package io.github.stevep99.scouty.conversation

import co.touchlab.kermit.Logger
import io.github.stevep99.scouty.llm.LlmService

private val log = Logger.withTag("RobotConversationManager")

class RobotConversationManager(
    private val llm: LlmService
) : ConversationManager {

    private val history = mutableListOf<Pair<String, String>>()

    override suspend fun sendMessage(userMessage: String): String {
        val response = llm.generate(userMessage)
        history.add(userMessage to response)
        log.d("Message: $userMessage -> Response: $response")
        return response
    }

    override fun getHistory(): List<Pair<String, String>> = history.toList()

    override fun clearHistory() {
        history.clear()
    }
}
