package io.github.stevep99.scouty

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.stevep99.scouty.command.RobotCommandParser
import io.github.stevep99.scouty.core.Action
import io.github.stevep99.scouty.core.Motion
import io.github.stevep99.scouty.core.MovementMode
import io.github.stevep99.scouty.core.Screen
import io.github.stevep99.scouty.core.ScoutyState
import io.github.stevep99.scouty.llm.LlmSettings
import io.github.stevep99.scouty.llm.LlmService
import io.github.stevep99.scouty.llm.RemoteLlmService
import io.github.stevep99.scouty.motors.sdk.RobotSdkConfigProvider
import io.github.stevep99.scouty.motors.sdk.SdkCommon
import io.github.stevep99.scouty.speech.AndroidSpeechRecognizer
import io.github.stevep99.scouty.speech.SpeechResultListener
import io.github.stevep99.scouty.speech.SpeechRecognizer
import io.github.stevep99.scouty.tts.AndroidTtsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import co.touchlab.kermit.Logger

private val log = Logger.withTag("MainViewModel")

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val ttsService = AndroidTtsService(application)
    private val llmSettings = LlmSettings(application)
    private val commandParser = RobotCommandParser()
    private var speechRecognizer: SpeechRecognizer? = null
    private var llmService: LlmService? = null
    private var sdkService: SdkCommon? = null

    // Whether this build has a physical robot (movement SDK). Resolved from the build parameter ROBOT_SDK.
    val supportsMovement: Boolean = RobotSdkConfigProvider.instance.supportsMovement

    var ttsReady by mutableStateOf(false)
        private set
    var llmReady by mutableStateOf(false)
        private set
    var sttReady by mutableStateOf(false)
        private set
    var llmBaseUrl by mutableStateOf(llmSettings.baseUrl)
        private set
    var llmApiKey by mutableStateOf(llmSettings.apiKey)
        private set
    var llmModelName by mutableStateOf(llmSettings.modelName)
        private set
    var currentState by mutableStateOf(ScoutyState.Idle)
        private set
    val logs = mutableStateListOf<String>()
    var currentScreen by mutableStateOf(Screen.Face)
        private set
    var movementListening by mutableStateOf(false)
        private set
    var movementVoiceText by mutableStateOf<String?>(null)
        private set
    var movementMode by mutableStateOf(MovementMode.IMMEDIATE)
        private set
    val movementQueue = mutableStateListOf<Action>()
    var movementExecuting by mutableStateOf(false)
        private set

    private var silenceTimeoutJob: Job? = null
    private var voiceSession = 0
    private var stopRequested = false
    private val immediateActions = Channel<Action>(Channel.UNLIMITED)

    init {
        ttsService.setOnReadyListener { ttsReady = true }

        if (llmSettings.isConfigured()) {
            connectLlm()
        } else {
            addLog("[LLM] No endpoint configured. Enter a Base URL below.")
        }

        viewModelScope.launch { runImmediateExecutor() }
    }

    fun initSpeechRecognizer() {
        speechRecognizer = AndroidSpeechRecognizer(getApplication())
        sttReady = true
        addLog("[STT] Android speech recognizer ready")
    }

    fun setSdkService(service: SdkCommon) {
        sdkService = service
        addLog("[SDK] Robot service connected")
    }

    fun clearSdkService() {
        sdkService = null
        addLog("[SDK] Robot service disconnected")
    }

    fun navigateTo(screen: Screen) {
        if (screen == Screen.Movement && !supportsMovement) {
            addLog("[NAV] Movement screen not available in this build")
            return
        }
        currentScreen = screen
    }

    fun startListeningFromButton() {
        if (currentState != ScoutyState.Idle) return
        if (!sttReady || !ttsReady) {
            addLog("[ERROR] Not all services ready")
            return
        }
        addLog("[STT] Manual trigger")
        viewModelScope.launch { startListening() }
    }

    fun stopVoiceInteraction() {
        if (currentState != ScoutyState.Listening) return
        voiceSession++
        silenceTimeoutJob?.cancel()
        speechRecognizer?.stopListening()
        backToIdle()
        addLog("[STT] Stopped by tap")
    }

    fun updateLlmBaseUrl(value: String) { llmBaseUrl = value }
    fun updateLlmApiKey(value: String) { llmApiKey = value }
    fun updateLlmModelName(value: String) { llmModelName = value }

    fun connectLlm() {
        llmSettings.baseUrl = llmBaseUrl
        llmSettings.apiKey = llmApiKey
        llmSettings.modelName = llmModelName

        val service = RemoteLlmService(llmBaseUrl, llmApiKey, llmModelName)
        service.setOnReadyListener { llmReady = true }
        llmService = service

        addLog("[LLM] Connecting to ${llmBaseUrl.trimEnd('/')} (model: ${llmModelName.ifBlank { "auto-detect" }})...")
        viewModelScope.launch {
            withContext(Dispatchers.IO) { service.loadModel() }
            if (service.isReady) {
                service.resolvedModelName?.let { resolved ->
                    llmModelName = resolved
                    llmSettings.modelName = resolved
                }
            } else {
                addLog("[ERROR] Endpoint unreachable. Check URL/key and that the server is running.")
            }
        }
    }

    fun generateResponse(prompt: String) {
        val service = llmService
        if (service == null || !service.isReady) {
            addLog("[ERROR] LLM not ready")
            return
        }
        if (currentState == ScoutyState.Thinking) return

        addLog("[INPUT] $prompt")
        viewModelScope.launch {
            addLog("[LLM] Generating...")
            currentState = ScoutyState.Thinking
            try {
                val startTime = System.currentTimeMillis()
                val response = withContext(Dispatchers.IO) { service.generate(prompt) }
                val elapsed = System.currentTimeMillis() - startTime
                addLog("[LLM] Response (${elapsed}ms): $response")
            } catch (e: Exception) {
                addLog("[ERROR] ${e.message}")
            } finally {
                backToIdle()
            }
        }
    }

    fun speak(text: String) {
        addLog("[TTS] Speaking: $text")
        ttsService.speak(text)
    }

    fun onMicPermissionDenied() {
        addLog("[ERROR] Microphone permission denied")
    }

    fun changeMovementMode(mode: MovementMode) {
        movementMode = mode
        addLog("[MOVEMENT] Mode: ${mode.name.lowercase()}")
    }

    fun startMovementListening() {
        if (movementListening) return
        if (!sttReady) {
            addLog("[ERROR] STT not ready")
            return
        }

        movementListening = true
        addLog("[STT] Movement voice listening...")

        speechRecognizer?.startListening(object : SpeechResultListener {
            override fun onResult(text: String) {
                addLog("[SPEECH] $text")
                movementVoiceText = text

                val parsed = commandParser.parse(text)
                parsed.actions.forEach { submitMovementAction(it) }

                val isTerminating = parsed.actions.any { it in TERMINATING_ACTIONS }
                if (isTerminating) {
                    movementListening = false
                } else {
                    movementListening = false
                    viewModelScope.launch {
                        delay(100)
                        startMovementListening()
                    }
                }
            }

            override fun onError(error: String) {
                addLog("[STT] $error")
                if (error in CONTINUABLE_ERRORS && movementListening) {
                    movementListening = false
                    viewModelScope.launch {
                        delay(100)
                        startMovementListening()
                    }
                } else {
                    movementListening = false
                }
            }
        })

        silenceTimeoutJob?.cancel()
        silenceTimeoutJob = viewModelScope.launch {
            delay(SILENCE_TIMEOUT_MS)
            addLog("[STT] Silence timeout")
            speechRecognizer?.stopListening()
            movementListening = false
        }
    }

    fun stopMovementListening() {
        silenceTimeoutJob?.cancel()
        speechRecognizer?.stopListening()
        movementListening = false
    }

    fun stopMovementListeningOnBack() {
        if (movementListening) {
            speechRecognizer?.stopListening()
            movementListening = false
        }
    }

    fun consumeVoiceText() {
        movementVoiceText = null
    }

    fun submitMovementAction(action: Action) {
        when (action) {
            is Action.MovementAction, is Action.Stop -> when (movementMode) {
                MovementMode.QUEUE -> queueCommand(action)
                MovementMode.IMMEDIATE -> viewModelScope.launch { immediateActions.send(action) }
            }
            Action.Undo, Action.Execute, Action.Clear -> queueCommand(action)
            else -> {}
        }
    }

    private fun queueCommand(action: Action) {
        when (action) {
            is Action.MovementAction -> movementQueue.add(action)
            Action.Undo -> if (movementQueue.isNotEmpty()) {
                movementQueue.removeAt(movementQueue.lastIndex)
                addLog("[MOVEMENT] Undo — removed last action")
            }
            Action.Clear -> movementQueue.clear()
            Action.Execute -> runQueue()
            Action.Stop -> {
                movementQueue.clear()
                stopRequested = true
                viewModelScope.launch { executeStop() }
            }
            else -> {}
        }
    }

    private fun runQueue() {
        if (movementExecuting || movementQueue.isEmpty()) return
        viewModelScope.launch {
            movementExecuting = true
            try {
                while (movementQueue.isNotEmpty()) {
                    if (stopRequested) {
                        stopRequested = false
                        movementQueue.clear()
                        executeRobotAction(Action.Stop)
                        break
                    }
                    val action = movementQueue.removeAt(0)
                    executeSingleAction(action)
                    delay(ACTION_GAP_MS)
                }
            } catch (e: Exception) {
                addLog("[ERROR] Queue execution failed: ${e.message}")
            } finally {
                movementExecuting = false
            }
        }
    }

    private suspend fun runImmediateExecutor() {
        var currentJob: Job? = null
        for (action in immediateActions) {
            when (action) {
                is Action.Stop -> {
                    currentJob?.cancel()
                    currentJob = null
                    movementExecuting = false
                    executeStop()
                }
                else -> {
                    currentJob?.join()
                    movementExecuting = true
                    currentJob = viewModelScope.launch {
                        executeSingleAction(action)
                        movementExecuting = false
                    }
                }
            }
        }
    }

    private suspend fun executeStop() {
        actionSpeech(Action.Stop)?.let { ttsService.speak(it) }
        executeRobotAction(Action.Stop)
    }

    private suspend fun executeSingleAction(action: Action) {
        when (action) {
            is Action.MovementAction -> {
                executeRobotAction(action)
            }
            else -> {}
        }
    }

    suspend fun executeRobotAction(action: Action) {
        val sdk = sdkService ?: return
        when (action) {
            is Action.MovementAction -> action.motions.forEach { motion ->
                motionSpeech(motion)?.let { ttsService.speak(it) }
                when (motion) {
                    Motion.Forward -> {
                        addLog("[ROBOT] Move forward")
                        sdk.moveForwards()
                    }
                    Motion.Backward -> {
                        addLog("[ROBOT] Move backward")
                        sdk.moveBackwards()
                    }
                    Motion.Left -> {
                        addLog("[ROBOT] Turn left")
                        sdk.moveLeft()
                    }
                    Motion.Right -> {
                        addLog("[ROBOT] Turn right")
                        sdk.moveRight()
                    }
                    Motion.Wiggle -> {
                        addLog("[ROBOT] Wiggle")
                        sdk.performWiggle()
                    }
                    Motion.Dance -> {
                        addLog("[ROBOT] Dance")
                        sdk.performDanceDemo()
                    }
                }
                delay(ACTION_GAP_MS)
            }
            is Action.Stop -> { addLog("[ROBOT] Stop"); sdk.moveStop() }
            else -> {}
        }
    }

    private fun startListening() {
        silenceTimeoutJob?.cancel()
        currentState = ScoutyState.Listening
        addLog("[STT] Listening...")

        val session = ++voiceSession

        speechRecognizer?.startListening(object : SpeechResultListener {
            override fun onResult(text: String) {
                if (session != voiceSession) return
                addLog("[SPEECH] $text")
                silenceTimeoutJob?.cancel()
                runPipeline(text)
            }

            override fun onError(error: String) {
                if (session != voiceSession) return
                addLog("[STT] $error")
                silenceTimeoutJob?.cancel()
                backToIdle()
            }
        })

        silenceTimeoutJob = viewModelScope.launch {
            delay(SILENCE_TIMEOUT_MS)
            addLog("[STT] Silence timeout — going back to idle")
            speechRecognizer?.stopListening()
            backToIdle()
        }
    }

    private fun backToIdle() {
        currentState = ScoutyState.Idle
        silenceTimeoutJob?.cancel()
    }

    private fun runPipeline(userMessage: String) {
        currentState = ScoutyState.Thinking

        val response = commandParser.parse(userMessage)

        if (response.actions.isNotEmpty()) {
            addLog("[RESPONSE] $response")
            viewModelScope.launch {
                try {
                    val speech = handlePipelineActions(response.actions)
                    if (!speech.isNullOrBlank()) speakAndListen(speech) else {
                        delay(POST_ACTION_GRACE_MS)
                        startListening()
                    }
                } catch (e: Exception) {
                    addLog("[ERROR] Pipeline failed: ${e.message}")
                    backToIdle()
                }
            }
            return
        }

        val service = llmService
        if (service == null || !service.isReady) {
            addLog("[ERROR] LLM not ready")
            backToIdle()
            return
        }

        viewModelScope.launch {
            try {
                addLog("[LLM] Thinking...")
                val startTime = System.currentTimeMillis()
                val rawResponse = withContext(Dispatchers.IO) { service.generate(userMessage) }
                val elapsed = System.currentTimeMillis() - startTime
                addLog("[LLM] Raw (${elapsed}ms): $rawResponse")

                val llmParsed = commandParser.parseLlmOutput(rawResponse)
                addLog("[PARSE] speech=\"${llmParsed.speech}\" actions=${llmParsed.actions}")

                val speech = handlePipelineActions(llmParsed.actions)
                val finalSpeech = if (llmParsed.actions.isNotEmpty()) speech else llmParsed.speech.ifBlank { null }

                if (!finalSpeech.isNullOrBlank()) speakAndListen(finalSpeech) else {
                    delay(POST_ACTION_GRACE_MS)
                    startListening()
                }
            } catch (e: Exception) {
                addLog("[ERROR] Pipeline failed: ${e.message}")
                backToIdle()
            }
        }
    }

    private fun speakAndListen(text: String) {
        if (text.isBlank()) {
            addLog("[TTS] Empty speech, going back to idle")
            backToIdle()
            return
        }
        currentState = ScoutyState.Speaking
        addLog("[TTS] Speaking: $text")
        ttsService.setOnUtteranceDoneListener {
            viewModelScope.launch {
                addLog("[TTS] Done, listening again...")
                startListening()
            }
        }
        ttsService.speak(text)
    }

    private suspend fun handlePipelineActions(actions: List<Action>): String? {
        val speechParts = mutableListOf<String>()
        for (action in actions) {
            when (action) {
                is Action.MovementAction, is Action.Stop,
                is Action.Undo, is Action.Execute, is Action.Clear -> submitMovementAction(action)
                is Action.GetTime -> speechParts.add(handleTime())
                is Action.GetDate -> speechParts.add(handleDate())
                is Action.NavigateTo -> {
                    navigateTo(action.screen)
                    speechParts.add(navigateSpeech(action.screen))
                }
                else -> {}
            }
        }
        return speechParts.joinToString(" ").ifBlank { null }
    }

    private fun navigateSpeech(screen: Screen): String = when (screen) {
        Screen.Movement -> "Opening the movement screen."
        Screen.Face -> "Okay, back to my face."
        else -> ""
    }

    private fun handleTime(): String =
        "It's ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())} right now."

    private fun handleDate(): String =
        "Today is ${SimpleDateFormat("EEEE d MMMM yyyy", Locale.getDefault()).format(Date())}."

    private fun actionSpeech(action: Action): String? = when (action) {
        is Action.Stop -> "Stopping!"
        is Action.Undo -> "Undone!"
        is Action.Execute -> "Executing!"
        is Action.Clear -> "Cleared!"
        else -> null
    }

    private fun motionSpeech(motion: Motion): String? = when (motion) {
        Motion.Wiggle -> "Wiggle wiggle!"
        Motion.Dance -> "Let's dance!"
        Motion.Forward -> "Moving forward!"
        Motion.Backward -> "Moving backward!"
        Motion.Left -> "Turning left!"
        Motion.Right -> "Turning right!"
    }

    private fun addLog(message: String) {
        log.i(message)
        logs.add(message)
        if (logs.size > MAX_LOG_ENTRIES) {
            logs.removeRange(0, logs.size - MAX_LOG_ENTRIES)
        }
    }

    override fun onCleared() {
        silenceTimeoutJob?.cancel()
        speechRecognizer?.destroy()
        ttsService.shutdown()
        llmService?.unloadModel()
        super.onCleared()
    }

    companion object {
        private const val MAX_LOG_ENTRIES = 25
        const val ACTION_GAP_MS = 400L
        private const val POST_ACTION_GRACE_MS = 600L
        private const val SILENCE_TIMEOUT_MS = 15_000L
        private val TERMINATING_ACTIONS = setOf(Action.Execute, Action.Clear, Action.Stop)
        private val CONTINUABLE_ERRORS = setOf("No speech detected", "Speech timeout")
    }
}
