package io.github.stevep99.scouty.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    @SerialName("max_tokens") val maxTokens: Int
)

@Serializable
data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList()
)

@Serializable
data class ChatChoice(
    @SerialName("message") val message: AssistantMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class AssistantMessage(
    val role: String? = null,
    val content: String? = null,
    /** Reasoning/thinking trace emitted by hybrid models (Qwen3, DeepSeek-R1, ...). Counts against max_tokens. */
    @SerialName("reasoning_content") val reasoningContent: String? = null
)

@Serializable
data class ModelsListResponse(
    val `data`: List<ModelEntry> = emptyList()
)

@Serializable
data class ModelEntry(
    val id: String? = null
)

@Serializable
data class ErrorResponse(
    val error: ErrorBody? = null
)

@Serializable
data class ErrorBody(
    val message: String? = null
)
