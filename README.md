# Scouty

Scouty is an AI robot companion Android app for children, running a local LLM (Qwen 2.5 0.5B via llama.cpp) 
for understanding commands, Android TTS for voice output, and a proprietary SDK to drive a 
differential-drive robot. It uses Android's built-in speech recognition for voice input 
(requires network for Google's cloud STT), with Sherpa-ONNX available as an offline alternative. 
Voice commands like "forward", "left", "dance", and "wiggle" can be queued and executed in sequence, 
with continuous listening so commands flow naturally without repeated button presses.
