package io.github.stevep99.scouty.command

import io.github.stevep99.scouty.core.Action
import io.github.stevep99.scouty.core.Motion
import io.github.stevep99.scouty.core.Screen
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class RobotCommandParser : CommandParser {

    private data class Match(val index: Int, val action: Action)

    private val json = Json { ignoreUnknownKeys = true }

    override fun parse(message: String): ParsedResponse {
        val text = message.trim()

        parseMovementJson(text)?.let { motions ->
            if (motions.isNotEmpty()) {
                return ParsedResponse(speech = "", actions = listOf(Action.MovementAction(motions)))
            }
        }

        val matches = mutableListOf<Match>()

        for ((pattern, action) in PATTERNS) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            regex.findAll(text).forEach { result ->
                matches.add(Match(result.range.first, action))
            }
        }

        val ordered = matches.sortedBy { it.index }.map { it.action }
        return ParsedResponse(speech = text, actions = mergeMovements(ordered))
    }

    override fun parseLlmOutput(llmText: String): ParsedResponse {
        val text = llmText.trim()

        parseMovementJson(text)?.let { motions ->
            if (motions.isNotEmpty()) {
                return ParsedResponse(speech = "", actions = listOf(Action.MovementAction(motions)))
            }
        }

        return ParsedResponse(speech = text, actions = emptyList())
    }

    private fun parseMovementJson(text: String): List<Motion>? = runCatching {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start == -1 || end <= start) return null
        val jsonText = text.substring(start, end + 1)
        json.decodeFromString<MovementResponse>(jsonText).motions.mapNotNull { raw ->
            when (raw.trim().lowercase()) {
                "forward", "forwards", "ahead", "fwd" -> Motion.Forward
                "backward", "backwards", "back", "reverse" -> Motion.Backward
                "left", "turn left", "go left" -> Motion.Left
                "right", "turn right", "go right" -> Motion.Right
                "wiggle", "shake", "wobble" -> Motion.Wiggle
                "dance" -> Motion.Dance
                else -> null
            }
        }
    }.getOrNull()

    private fun mergeMovements(actions: List<Action>): List<Action> {
        val result = mutableListOf<Action>()
        var currentMotions = mutableListOf<Motion>()
        var hasPending = false

        fun flush() {
            if (hasPending) {
                result.add(Action.MovementAction(currentMotions.toList()))
                currentMotions = mutableListOf()
                hasPending = false
            }
        }

        for (action in actions) {
            when (action) {
                is Action.MovementAction -> {
                    currentMotions.addAll(action.motions)
                    hasPending = true
                }
                else -> {
                    flush()
                    result.add(action)
                }
            }
        }
        flush()
        return result
    }

    @Serializable
    private data class MovementResponse(
        @SerialName("motions") val motions: List<String> = emptyList()
    )

    companion object {
        private val PATTERNS = listOf(
            "\\b(get\\s*time|what(?:'s|is|\\s)+(?:the\\s+)?time|tell\\s+me\\s+the\\s+time|current\\s+time)\\b" to Action.GetTime,
            "\\b(get\\s*date|what[\\s']?s?\\s+(?:date|day)|tell\\s+me\\s+(?:the\\s+)?(?:date|day)|today)\\b" to Action.GetDate,
            "\\b(stop|halt|whoa|freeze|wait)\\b" to Action.Stop,
            "\\b(wiggle|shake|wobble)\\b" to Action.MovementAction(Motion.Wiggle),
            "\\b(dance)\\b" to Action.MovementAction(Motion.Dance),
            "\\b(turn\\s+left|go\\s+left|move\\s+left|lean\\s+left|left)\\b" to Action.MovementAction(Motion.Left),
            "\\b(turn\\s+right|go\\s+right|move\\s+right|lean\\s+right|right)\\b" to Action.MovementAction(Motion.Right),
            "\\b(go\\s+forward|move\\s+forward|forward|ahead|forward)\\b" to Action.MovementAction(Motion.Forward),
            "\\b(go\\s+back(?:ward)?|move\\s+back(?:ward)?|back(?:ward)?|back)\\b" to Action.MovementAction(Motion.Backward),
            "\\b(undo|remove\\s+last|take\\s+back|delete\\s+last)\\b" to Action.Undo,
            "\\b(execute|run|start|do\\s+it|begin)\\b" to Action.Execute,
            "\\b(clear|reset|empty)\\b" to Action.Clear,
            "\\b(go\\s+to\\s+(?:the\\s+)?(movement|motion|drive)\\s*screen)\\b" to Action.NavigateTo(Screen.Movement),
            "\\b(show\\s+me\\s+(?:the\\s+)?(movement|motion|drive)\\s*screen)\\b" to Action.NavigateTo(Screen.Movement),
            "\\b(open\\s+(?:the\\s+)?(movement|motion|drive)\\s*screen)\\b" to Action.NavigateTo(Screen.Movement),
            "\\b(go\\s+to\\s+(?:the\\s+)?(face|home)\\s*screen)\\b" to Action.NavigateTo(Screen.Face),
            "\\b(show\\s+me\\s+(?:the\\s+)?(face|home)\\s*screen)\\b" to Action.NavigateTo(Screen.Face),
            "\\b(go\\s+to\\s+movement)\\b" to Action.NavigateTo(Screen.Movement),
            "\\b(go\\s+(?:back\\s+)?home|go\\s+to\\s+face|show\\s+your\\s+face)\\b" to Action.NavigateTo(Screen.Face),
        )
    }
}
