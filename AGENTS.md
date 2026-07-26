# AGENTS.md

> **Maintain this file.** Keep AGENTS.md in sync with the codebase: update it whenever the project structure, build commands, models/actions, or conventions change, so future agents always have an accurate reference.

## Project Overview

Scouty is an Android app for a children's robot companion. It uses a remote LLM behind an OpenAI-compatible Chat Completions endpoint (any provider or local server — the robot side is endpoint-agnostic), Android speech recognition (the system `SpeechRecognizer`), Android TTS, and controls a differential-drive robot via a proprietary SDK. Built with Kotlin and Jetpack Compose. The `armeabi-v7a` (32-bit ARM) target is **a133-specific**; the default `generic`/tablet build ships all ABIs.

## Build Commands

```bash
./gradlew assembleGenericDebug     # Build default (tablet/generic) debug APK — no robot movement
./gradlew assembleA133Debug        # Build A133 robot debug APK (movement + native SDK)
./gradlew testGenericDebugUnitTest # Run unit tests for the generic flavor
./gradlew testA133DebugUnitTest    # Run unit tests for the A133 flavor
./gradlew :app:compileGenericDebugKotlin   # Quick compile check (generic)
./gradlew :app:compileA133DebugKotlin      # Quick compile check (a133)
```

The app uses **product flavors** on a single `robot` dimension. `generic` is the default (declared first) flavor and builds a version with no robot movement (runs on a plain tablet); `a133` adds the A133 movement SDK and native libraries. Add new robot flavors as new `productFlavors` entries pointing at their own SDK, following the A133 pattern.

## Project Structure

- `app/src/main/java/io/github/stevep99/scouty/` — main app source
  - `MainActivity.kt` — thin Activity shell (permissions, SDK binding, Compose setup, screen navigation)
  - `MainViewModel.kt` — owns all UI state, business logic, and service orchestration
  - `core/` — `Action`/`Motion` sealed interfaces; `ScoutyState`, `MovementMode`, `Screen` enums
  - `command/` — `CommandParser` interface and `RobotCommandParser` (regex-based)
  - `speech/` — `SpeechRecognizer` interface, `AndroidSpeechRecognizer` (wraps `android.speech.SpeechRecognizer`)
  - `llm/` — `LlmService` interface, `RemoteLlmService` (OpenAI-compatible HTTP client), `OpenAiApi.kt` (kotlinx.serialization DTOs), `StubLlmService`, `LlmSettings` (SharedPreferences-backed endpoint config: base URL, API key, model name)
  - `tts/` — `TextToSpeechService` interface, `AndroidTtsService`
  - `robot/` — `RobotService` interface, `StubRobotService`
  - `conversation/` — `ConversationManager` interface, `RobotConversationManager`
  - `ui/menu/` — Menu screen with Face / Movement / Settings buttons; hub for screen navigation (Movement button hidden when the build has no robot)
  - `ui/settings/` — Settings screen (formerly Debug) with logs, LLM endpoint panel, and test controls
  - `ui/face/` — Robot face main screen (pure Compose Canvas, state-driven animations; tap to talk/stop)
  - `ui/movement/` — Movement screen with D-pad, voice queue, QUEUE/IMMEDIATE modes; bound to global queue (only reachable on robot flavors)
  - `ui/theme/` — Compose theme (Color, Theme, Type)
- `app/src/generic/` and `app/src/a133/` — product-flavor source sets, each supplying its own `motors/sdk/Scouty...RobotSdkConfig` (`ScoutyGenericRobotSdkConfig` / `ScoutyA133RobotSdkConfig`), resolved at runtime via the `ROBOT_SDK` build parameter
- `sdk/common/` — Robot SDK interfaces (`SdkService`, `SdkCommon`, `SdkEventListener`, `RobotSdkConfig`)
- `sdk/a133/` — A133-specific SDK implementation (a dependency of the `a133` flavor). The native binary `libjnicmd_a133.so` is **not in the repo** (unknown license); restore your own vendor copy into `sdk/a133/src/main/jniLibs/armeabi-v7a/` to run the a133 robot flavor.
- `docs/` — This file

## Key Conventions

- **Package:** `io.github.stevep99.scouty`
- **minSdk:** 26, **targetSdk:** 36. The `armeabi-v7a` (32-bit ARM) ABI restriction is **a133-specific** (its native SDK is only built for 32-bit ARM); the `generic`/tablet build ships all ABIs.
- **Interfaces for all services:** `SpeechRecognizer`, `LlmService`, `TextToSpeechService`, `RobotService`, `ConversationManager`, `CommandParser` — each with an interface and implementation
- **Logging:** Kermit (`co.touchlab.kermit.Logger`); use a per-class tag via `Logger.withTag("<Name>")`
- **`Action` model:** `sealed interface Motion` (Forward, Backward, Left, Right, Wiggle, Dance) and `sealed interface Action` (`MovementAction(motions: List<Motion>)`, `NavigateTo(screen)`, Stop, FlashLed, GetTime, GetDate, Undo, Execute, Clear). `MovementAction` holds an ordered `List<Motion>` to represent sequences like "go left, then go forward" (`Action.MovementAction(motion)` single-motion constructor also available). Add new actions in `core/Action.kt`. `RobotCommandParser` maps regex patterns to actions, merges consecutive movements into one `MovementAction(List<Motion>)`, and also parses an LLM-returned JSON `{"motions": [...]}` into a sequence.
- **Global movement queue:** `MainViewModel` owns `movementQueue: List<Action>`, `movementMode` (QUEUE/IMMEDIATE global toggle), and `movementExecuting`. All movement input — voice from any screen (`runPipeline` / `startMovementListening`) and the Movement-screen D‑pad — funnels through `MainViewModel.submitMovementAction(action)`. In **QUEUE** mode actions append to the queue (Undo/Clear/Execute/Stop manage it; Execute runs the drained queue). In **IMMEDIATE** mode actions run serially via a Channel (`runImmediateExecutor`), with Stop cancelling the in-flight one. Speech for a movement is spoken before execution; `executeRobotAction` runs each motion in sequence through the SDK.
- **Voice pipeline:** `MainViewModel.runPipeline` parses input, then `handlePipelineActions` routes movement/queue actions through `submitMovementAction` (mode-aware), GetTime/GetDate return their text for speaking, and `NavigateTo` switches screens via `navigateTo`. After actions it returns to listening. Input is parsed with `RobotCommandParser.parse` (full keyword/command matching); **LLM responses are parsed with `parseLlmOutput`** which only honors the structured movement JSON — free-text chat answers are spoken verbatim and never keyword-scanned (so casual replies like "how are you today?" don't trigger false actions).
- **Movement screen:** binds to the global `movementQueue`/`movementMode`/`movementExecuting` from `MainViewModel` (state is global across screens). QUEUE mode shows the pending chips; IMMEDIATE executes right away. Buttons are equivalent to voice commands — both go through `submitMovementAction`. Mode toggle is disabled while executing.
- **Navigation:** `Screen` enum (`Face`, `Menu`, `Movement`, `Settings`) + `MainViewModel.navigateTo`. Menu is the hub; Face is the default screen. Every screen's top-right button returns to the Menu; on Settings a dedicated "LLM Settings" button (left of Menu, highlighted when the panel is open) toggles the LLM endpoint panel.
- **Face is the default/main screen** (`Screen.Face`): a full-bleed Canvas face (grey background bleeding to the screen edges — no drawn head outline, ears, or antenna, since the physical robot already has a head) reacting to `ScoutyState` (idle blink + smile, listening halo + wide eyes, thinking darting eyes + line mouth, speaking equalizer mouth, moving happy arcs). Eyes and mouth are expanded to fill the face space. Tapping it triggers the voice pipeline via `startListeningFromButton()`; top-right button returns to the Menu.
- **Robot SDK methods are `suspend` functions** with coroutine delays for timing
- **Flavor gating via `RobotSdkConfig` + build parameter:** each flavor source set defines its own `Scouty...RobotSdkConfig : RobotSdkConfig` under distinct names (`ScoutyGenericRobotSdkConfig` on `generic`, `ScoutyA133RobotSdkConfig` on `a133`). Each `productFlavor` sets a BuildConfig string field `ROBOT_SDK` to that class's fully-qualified name. Main source never references a flavor class directly — `RobotSdkConfigProvider.instance` (in `app/src/main/.../motors/sdk/RobotSdkConfigProvider.kt`) reads `BuildConfig.ROBOT_SDK_CLASS` and reflectively instantiates it as `RobotSdkConfig` (falling back to a `supportsMovement=false` config on failure). The config exposes `supportsMovement` (`true` for robot flavors) and `sdkServiceClass(): Class<out SdkService>?` (`SdkA133Service::class.java` on `a133`, `null` on `generic`). `MainActivity.bindRobotSdk()` uses `supportsMovement` to gate SDK binding and passes `showMovement` to the Menu; `MainViewModel.supportsMovement` comes from the same provider; `navigateTo` blocks `Screen.Movement` when unsupported. `executeRobotAction` already no-ops when `sdkService == null`. `SdkA133Service` is registered in the `sdk:a133` library manifest (merged for the `a133` flavor, absent for `generic`). To add a new robot flavor: add its config class, point `ROBOT_SDK_CLASS` at it, and add the flavor dependency.
- **LLM is remote and endpoint-agnostic:** any OpenAI-compatible server works (`{base_url}/chat/completions`). Point at Ollama (`http://<ip>:11434/v1`), llama.cpp `llama-server` (`http://<ip>:8080/v1`), LM Studio (`http://<ip>:1234/v1`), or cloud free tiers (Groq, Gemini's OpenAI-compat endpoint, Mistral) with an API key. Endpoint configured in Settings screen ("Endpoint" panel → Base URL / API key / Model → Connect); persisted via `LlmSettings`. Blank Model field is auto-detected from `GET {base}/models` (first entry) on Connect. Request/response JSON uses kotlinx.serialization DTOs in `llm/OpenAiApi.kt`. Scouty persona system prompt lives in `RemoteLlmService.SYSTEM_PROMPT`; it instructs the model that movement requests must return JSON `{"motions": ["left","forward","left"]}` (which `RobotCommandParser` maps to a `MovementAction`), while time, date, and "open the movement screen"/"show your face" requests get ordinary short spoken answers (the parser also handles these deterministically). `MAX_TOKENS=256`. Cleartext HTTP is enabled in the manifest for LAN servers.

## Launcher Icon

Custom Scouty robot-head icon: adaptive icon (`mipmap-anydpi-v26/ic_launcher.xml`) composed of `drawable/ic_launcher_foreground.xml` (white robot-head vector) and `drawable/ic_launcher_background.xml` (solid `#1565C0`), with legacy webp mipmaps in `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/`.

## Testing

Unit tests are in `app/src/test/java/io/github/stevep99/scouty/command/`. The `RobotCommandParserTest` covers all voice command patterns. Run with `./gradlew testGenericDebugUnitTest` (generic) or `./gradlew testA133DebugUnitTest` (a133).
