package io.github.stevep99.scouty.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import co.touchlab.kermit.Logger
import java.util.Locale

private val log = Logger.withTag("AndroidTtsService")

class AndroidTtsService(context: Context) : TextToSpeechService {

    private var tts: TextToSpeech? = null
    private var onReadyListener: (() -> Unit)? = null
    private var onUtteranceDoneListener: (() -> Unit)? = null
    override var isReady: Boolean = false
        private set

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        log.d("Utterance done: $utteranceId")
                        onUtteranceDoneListener?.invoke()
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        log.e("Utterance error: $utteranceId")
                        onUtteranceDoneListener?.invoke()
                    }
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        log.e("Utterance error: $utteranceId, code=$errorCode")
                        onUtteranceDoneListener?.invoke()
                    }
                })
                isReady = true
                log.d("TTS initialised")
                onReadyListener?.invoke()
            } else {
                log.e("TTS init failed with status $status")
            }
        }
    }

    override fun setOnReadyListener(listener: (() -> Unit)?) {
        onReadyListener = listener
        if (isReady) listener?.invoke()
    }

    override fun setOnUtteranceDoneListener(listener: (() -> Unit)?) {
        onUtteranceDoneListener = listener
    }

    override fun speak(text: String) {
        if (!isReady) {
            log.w("TTS not ready, queuing: $text")
            return
        }
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utterance_${System.currentTimeMillis()}")
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "utterance_${System.currentTimeMillis()}")
        log.d("Speaking: $text")
    }

    override fun stop() {
        tts?.stop()
    }

    override fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
        log.d("TTS shut down")
    }
}
