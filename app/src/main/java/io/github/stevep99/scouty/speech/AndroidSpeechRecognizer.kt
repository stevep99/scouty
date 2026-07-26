package io.github.stevep99.scouty.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as AndroidSTT
import co.touchlab.kermit.Logger

private val log = Logger.withTag("AndroidSpeechRecognizer")

class AndroidSpeechRecognizer(context: Context) : SpeechRecognizer {

    private val recognizer: AndroidSTT = AndroidSTT.createSpeechRecognizer(context)
    private var currentListener: SpeechResultListener? = null
    private var listening = false

    override val isListening: Boolean
        get() = listening

    override fun startListening(listener: SpeechResultListener) {
        currentListener = listener

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                log.d("Ready for speech")
            }

            override fun onBeginningOfSpeech() {
                log.d("Speech started")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                log.d("Speech ended")
            }

            override fun onError(error: Int) {
                listening = false
                val msg = when (error) {
                    AndroidSTT.ERROR_NO_MATCH -> "No speech detected"
                    AndroidSTT.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    AndroidSTT.ERROR_AUDIO -> "Audio error"
                    AndroidSTT.ERROR_CLIENT -> "Client error"
                    AndroidSTT.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing microphone permission"
                    AndroidSTT.ERROR_NETWORK -> "Network error"
                    AndroidSTT.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    AndroidSTT.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    else -> "Speech error $error"
                }
                log.e("Error: $msg ($error)")
                currentListener?.onError(msg)
            }

            override fun onResults(results: Bundle?) {
                listening = false
                val matches = results?.getStringArrayList(AndroidSTT.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                log.d("Result: '$text'")
                if (text.isNotBlank()) {
                    currentListener?.onResult(text)
                } else {
                    currentListener?.onError("No speech detected")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra("android.speech.extras.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 2000L)
            putExtra("android.speech.extras.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 2000L)
            putExtra("android.speech.extras.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 1000L)
        }

        recognizer.startListening(intent)
        listening = true
        log.d("Started listening")
    }

    override fun stopListening() {
        listening = false
        recognizer.stopListening()
        log.d("Stopped listening")
    }

    override fun destroy() {
        listening = false
        recognizer.cancel()
        recognizer.destroy()
        currentListener = null
        log.d("Destroyed")
    }
}
