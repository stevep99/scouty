package io.github.stevep99.scouty.command

import io.github.stevep99.scouty.core.Action

data class ParsedResponse(
    val speech: String,
    val actions: List<Action> = emptyList()
)

interface CommandParser {
    /** Parses raw user voice input (keywords, aliases, command chaining). */
    fun parse(input: String): ParsedResponse

    /**
     * Parses free-form LLM output. Only the structured movement JSON (e.g.
     * `{"motions": ["left","forward"]}`) is treated as an action; any other
     * conversational text is returned verbatim as speech and never keyword-scanned,
     * so natural phrases like "how are you today?" don't trigger false actions.
     */
    fun parseLlmOutput(llmText: String): ParsedResponse
}
