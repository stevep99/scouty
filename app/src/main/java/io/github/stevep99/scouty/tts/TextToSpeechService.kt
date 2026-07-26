package io.github.stevep99.scouty.tts

interface TextToSpeechService {
    fun speak(text: String)
    fun stop()
    fun shutdown()
    val isReady: Boolean
    fun setOnReadyListener(listener: (() -> Unit)?) {}
    fun setOnUtteranceDoneListener(listener: (() -> Unit)?) {}
}
