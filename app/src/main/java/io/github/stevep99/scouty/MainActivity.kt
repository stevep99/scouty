package io.github.stevep99.scouty

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import io.github.stevep99.scouty.core.Action
import io.github.stevep99.scouty.core.Screen
import io.github.stevep99.scouty.motors.sdk.RobotSdkConfigProvider
import io.github.stevep99.scouty.motors.sdk.SdkCommon
import io.github.stevep99.scouty.motors.sdk.SdkService
import io.github.stevep99.scouty.ui.face.RobotFaceScreen
import io.github.stevep99.scouty.ui.menu.MenuScreen
import io.github.stevep99.scouty.ui.movement.MovementScreen
import io.github.stevep99.scouty.ui.settings.SettingsScreen
import io.github.stevep99.scouty.ui.theme.ScoutyTheme

class MainActivity : ComponentActivity() {

    private lateinit var vm: MainViewModel
    private var sdkService: SdkCommon? = null
    private var sdkBound = false

    // Flavor-specific: which robot SDK service (if any) is available in this build,
    // resolved from the build parameter ROBOT_SDK.
    private val robotSdkConfig = RobotSdkConfigProvider.instance
    private val supportsMovement = robotSdkConfig.supportsMovement

    private val sdkConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val svc = (service as SdkService.LocalBinder).getService()
            sdkService = svc
            vm.setSdkService(svc)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            sdkService = null
            vm.clearSdkService()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            vm.initSpeechRecognizer()
        } else {
            vm.onMicPermissionDenied()
            Toast.makeText(this, "Microphone permission required for voice", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        vm = ViewModelProvider(this)[MainViewModel::class.java]

        requestMicPermission()

        bindRobotSdk()

        setContent {
            ScoutyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (vm.currentScreen) {
                        Screen.Face -> RobotFaceScreen(
                            currentState = vm.currentState,
                            ready = vm.sttReady && vm.ttsReady,
                            onStartListening = { vm.startListeningFromButton() },
                            onStopListening = { vm.stopVoiceInteraction() },
                            onOpenMenu = { vm.navigateTo(Screen.Menu) },
                            modifier = Modifier.padding(innerPadding)
                        )
                        Screen.Menu -> MenuScreen(
                            onFace = { vm.navigateTo(Screen.Face) },
                            onMovement = { vm.navigateTo(Screen.Movement) },
                            onSettings = { vm.navigateTo(Screen.Settings) },
                            showMovement = supportsMovement,
                            modifier = Modifier.padding(innerPadding)
                        )
                        Screen.Movement -> MovementScreen(
                            onAction = { action -> vm.submitMovementAction(action) },
                            onBack = {
                                vm.stopMovementListeningOnBack()
                                vm.navigateTo(Screen.Menu)
                            },
                            onStartListening = { vm.startMovementListening() },
                            onStopListening = { vm.stopMovementListening() },
                            isListening = vm.movementListening,
                            voiceText = vm.movementVoiceText,
                            onVoiceTextConsumed = { vm.consumeVoiceText() },
                            movementMode = vm.movementMode,
                            onMovementModeChange = { mode -> vm.changeMovementMode(mode) },
                            queue = vm.movementQueue,
                            executing = vm.movementExecuting,
                            onExecute = { vm.submitMovementAction(Action.Execute) },
                            onUndo = { vm.submitMovementAction(Action.Undo) },
                            onClear = { vm.submitMovementAction(Action.Clear) },
                            modifier = Modifier.padding(innerPadding)
                        )
                        else -> SettingsScreen(
                            ttsReady = vm.ttsReady,
                            llmReady = vm.llmReady,
                            sttReady = vm.sttReady,
                            currentState = vm.currentState,
                            llmBaseUrl = vm.llmBaseUrl,
                            llmApiKey = vm.llmApiKey,
                            llmModelName = vm.llmModelName,
                            onLlmBaseUrlChange = { vm.updateLlmBaseUrl(it) },
                            onLlmApiKeyChange = { vm.updateLlmApiKey(it) },
                            onLlmModelNameChange = { vm.updateLlmModelName(it) },
                            onConnectLlm = { vm.connectLlm() },
                            onSpeak = { vm.speak(it) },
                            onGenerate = { vm.generateResponse(it) },
                            onStartListening = { vm.startListeningFromButton() },
                            onOpenMenu = { vm.navigateTo(Screen.Menu) },
                            logs = vm.logs,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    private fun requestMicPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                vm.initSpeechRecognizer()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun bindRobotSdk() {
        if (!supportsMovement) return
        val serviceClass = robotSdkConfig.sdkServiceClass() ?: return
        Intent(this, serviceClass).also { intent ->
            sdkBound = bindService(intent, sdkConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        if (sdkBound) {
            unbindService(sdkConnection)
            sdkBound = false
        }
        super.onDestroy()
    }
}
