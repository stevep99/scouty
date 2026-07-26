package io.github.stevep99.scouty.llm

import co.touchlab.kermit.Logger

private val log = Logger.withTag("StubLlmService")

class StubLlmService : LlmService {

    override var isReady: Boolean = false
        private set

    override suspend fun loadModel() {
        isReady = true
        log.d("Model loaded (stub)")
    }

    override suspend fun generate(prompt: String): String {
        log.d("Generating for: $prompt")
        return "Hello! I am Scouty, your robot friend. I am a stub right now, but soon I will be powered by a real AI!"
    }

    override fun unloadModel() {
        isReady = false
        log.d("Model unloaded (stub)")
    }
}
