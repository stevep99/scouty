package io.github.stevep99.scouty.llm

interface LlmService {
    suspend fun loadModel()
    suspend fun generate(prompt: String): String
    fun unloadModel()
    val isReady: Boolean
    fun setOnReadyListener(listener: (() -> Unit)?) {}
}
