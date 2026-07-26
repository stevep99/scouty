# AGENTS.md

## Project Overview

Scouty is an Android app for a children's robot companion. It runs a local LLM (Qwen 2.5 0.5B via llama.cpp), speech recognition (Android STT or Sherpa-ONNX), TTS, and controls a differential-drive robot via a proprietary SDK. Built with Kotlin and Jetpack Compose, targeting armeabi-v7a (32-bit ARM).

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew testDebugUnitTest      # Run unit tests
```

## Project Structure

- `app/src/main/java/io/github/stevep99/scouty/` — main app source
  - `MainActivity.kt` — thin Activity shell (permissions, SDK binding, Compose setup)
  - `MainViewModel.kt` — owns all UI state, business logic, and service orchestration
  - `core/` — `Action` sealed interface, `ScoutyState` enum
  - `command/` — `CommandParser` interface and `RobotCommandParser` (regex-based)
  - `speech/` — `SpeechRecognizer` interface, `AndroidSpeechRecognizer`, `SherpaSpeechRecognizer`
  - `llm/` — `LlmService` interface, `LlamaCppLlmService` (llama.cpp JNI bridge)
  - `tts/` — `TextToSpeechService` interface, `AndroidTtsService`
  - `robot/` — `RobotService` interface, `StubRobotService`
  - `conversation/` — `ConversationManager` interface, `RobotConversationManager`
  - `ui/debug/` — Debug screen with logs and test controls
  - `ui/movement/` — Movement test screen with D-pad, voice queue, execution
  - `ui/theme/` — Compose theme (Color, Theme, Type)
- `app/libs/` — Local AARs: `llama-android-armv7.aar`, `sherpa-onnx-armv7.aar`
- `app/src/main/assets/sherpa-onnx/` — Sherpa-ONNX streaming zipformer model (int8, ~43MB)
- `sdk/common/` — Robot SDK interfaces (`SdkService`, `SdkCommon`, `SdkEventListener`)
- `sdk/a133/` — A133-specific SDK implementation, includes `com.cloudring.commonlib.cmd.JniCmd` (JNI bridge, do not modify package)
- `docs/` — Plan document and this file

## Key Conventions

- **Package:** `io.github.stevep99.scouty`
- **minSdk:** 26, **targetSdk:** 36, **ABI:** armeabi-v7a only
- **Interfaces for all services:** `SpeechRecognizer`, `LlmService`, `TextToSpeechService`, `RobotService`, `ConversationManager`, `CommandParser` — each with a stub and real implementation
- **STT engine switchable** via `SpeechRecognizer.ENGINE` constant (Android or Sherpa-ONNX)
- **`Action` is a sealed interface** — add new actions there; `RobotCommandParser` uses regex patterns mapped to actions
- **Robot SDK methods are `suspend` functions** with coroutine delays for timing
- **Native lib conflicts:** Sherpa-ONNX's `libonnxruntime.so` and llama.cpp's `libggml-cpu.so` conflict when both load (cpuinfo symbol clash). Currently using Android STT to avoid this. If switching back to Sherpa-ONNX, load it in a separate process or lazy-init after LLM.
- **LLM config:** CONTEXT_SIZE=2048, MAX_TOKENS=256, THREADS=4. Use `q4_0` quantized model (not `q4_k_m` which causes SIGABRT).

## Testing

Unit tests are in `app/src/test/java/io/github/stevep99/scouty/command/`. The `RobotCommandParserTest` covers all voice command patterns. Run with `./gradlew testDebugUnitTest`.
