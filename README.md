# Scouty

Scouty is an AI robot companion Android app for children. It understands spoken commands and answers back by voice while driving a differential-drive robot.

## Features

- **Voice control** via Android's built-in speech recognizer — say "turn left", "go forward", "dance", or "wiggle"
- **Remote LLM** behind any OpenAI-compatible endpoint — a local server on your network or a cloud provider
- **Voice responses** through Android TTS, spoken concurrently with robot movements
- **Movement screen** with an on-screen D-pad and voice queue:
  - **QUEUE mode** — commands are queued and executed in sequence
  - **IMMEDIATE mode** — each command runs immediately, and "stop" halts the robot
- **Time & date** support: ask "what time is it?" or "what's today's date?"
- **Voice navigation**: say "go to the movement screen" or "show your face" to switch screens by voice
- **Face screen** — a face bled out to the screen edges, with eyes and mouth that react to listening, thinking, speaking, and moving
- Continuous listening, so commands flow naturally without repeated button presses

## Architecture

- `MainViewModel` orchestrates the voice pipeline: speech → command parsing → robot actions + spoken confirmation → listen again
- `RobotCommandParser` turns recognized text into `Action`s (`MovementAction` holding an ordered `List<Motion>`, `NavigateTo`, `Stop`, `GetTime`, `GetDate`, `Undo`, `Execute`, `Clear`) via regex, merging consecutive movements into one sequence and parsing LLM-returned `{"motions": [...]}` JSON. Full keyword matching applies to voice input (`parse`); LLM replies use `parseLlmOutput`, which only honors the movement JSON and speaks free-text chat verbatim — so casual replies like "how are you today?" never trigger a spurious action.
- A global movement queue (`MainViewModel`) collects actions in QUEUE mode or runs them immediately in IMMEDIATE mode, from both voice and the Movement-screen D-pad
- Remote LLM behind any OpenAI-compatible Chat Completions endpoint; configured in the Settings screen
- Robot control goes through a proprietary SDK (`sdk/`) with suspend methods per movement, wired in per flavor via `RobotSdkConfig`. Each flavor sets a `ROBOT_SDK` build parameter; `RobotSdkConfigProvider` reflectively instantiates it at runtime. `supportsMovement` gates movement UI and SDK binding, so the `generic` build has no robot at all.

## Build

Scouty uses two build flavors on a `robot` dimension:

```bash
./gradlew assembleGenericDebug    # Default (tablet/generic) build — no robot movement
./gradlew assembleA133Debug       # A133 robot build — movement + proprietary native SDK
```

`generic` (the default) runs on any tablet with no robot attached and ships all ABIs (minSdk 26, targetSdk 36); `a133` adds the A133 movement SDK and native libraries, restricted to `armeabi-v7a` (32-bit ARM) since the A133 native SDK is only built for that ABI.

> **Note:** the A133 robot build depends on a native binary,
> `libjnicmd_a133.so`, that is **not included** in this repository (unknown license).
> The `a133` flavor compiles without it but won't drive a robot until you restore your
> own vendor copy into `sdk/a133/src/main/jniLibs/armeabi-v7a/`. The `generic` 
> build is unaffected.

Unit tests: `./gradlew testGenericDebugUnitTest` (generic) or `./gradlew testA133DebugUnitTest` (a133).

## LLM

For natural language chat, Scouty needs access to a running LLM server speaking the OpenAI Chat Completions API. You can run this locally on a machine in the same network or use a cloud-based service.

To configure Scouty, open the **Settings** screen and tap the **LLM Settings** button (top-right, left of Menu) to open the endpoint panel. Fill in:

- **Base URL** — e.g. `http://your-local-ip-address:8080/v1`
- **API key** — only needed for cloud services; leave blank for local servers
- **Model** — optional for local servers serving a single model (auto-detected); required by most cloud providers

Tap **Connect**. The settings are saved on the device and restored on startup.

### Local server (llama.cpp)

On a machine in the same network as the robot:

```bash
# Install llama.cpp, then fetch and serve a model in one go:
llama-server --host 0.0.0.0 --port 8080 -hf Qwen/Qwen3-14B-GGUF:Q4_K_M
```

Then enter `http://<machine-ip-address>:8080/v1` as the Base URL in Scouty.

Other OpenAI-compatible servers work the same way, e.g. Ollama (`http://<ip>:11434/v1`) or LM Studio (`http://<ip>:1234/v1`).

### Cloud provider

Any OpenAI-compatible provider works, e.g. Groq (`https://api.groq.com/openai/v1`), Google Gemini (`https://generativelanguage.googleapis.com/v1beta/openai`), or Mistral (`https://api.mistral.ai/v1`). Sign up with the provider, create an API key, then enter the Base URL, the key, and a model name in Scouty.

Note: using a cloud provider sends the child's spoken requests to that provider.

