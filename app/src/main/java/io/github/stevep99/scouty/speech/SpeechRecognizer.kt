package io.github.stevep99.scouty.speech

fun interface SpeechResultListener {
    fun onResult(text: String)
    fun onError(error: String) {}
}

interface SpeechRecognizer {
    fun startListening(listener: SpeechResultListener)
    fun stopListening()
    fun destroy()
    val isListening: Boolean
}
