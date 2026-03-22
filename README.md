# Local AI Hub MVP

Android Native scaffold for an on-device multi-model assistant.

## Implemented

- `Chat` tab with local chat runtime controls and a real `llama.cpp` bridge for imported `GGUF` models.
- `Images` tab with a demo OCR flow routed through a LiteRT-style engine.
- `Voice` tab with a demo speech-to-text flow routed through an ONNX Runtime-style engine.
- `Models` tab with a persistent model registry, activation, preview-vs-real readiness, `GGUF` import for chat models, deletion, and on-demand download scheduling.
- Room-backed model storage, WorkManager download jobs, Hilt DI, and device telemetry sampling.
- Vendored `llama.cpp` source under `third_party/llama.cpp`, compiled into the Android app through CMake.

## Project Shape

- `app/src/main/java/com/localai/hub/core/modelregistry`
  - built-in model catalog and model metadata
- `app/src/main/java/com/localai/hub/core/storage`
  - Room database, DAO, repository, and mappers
- `app/src/main/java/com/localai/hub/core/inference`
  - orchestrator, runtime profiles, and engine abstractions
- `app/src/main/java/com/localai/hub/core/importing`
  - local `GGUF` import and validation
- `app/src/main/java/com/localai/hub/core/download`
  - deferred model download worker and scheduler
- `app/src/main/java/com/localai/hub/core/telemetry`
  - RAM / thermal / battery-saver sampling
- `app/src/main/java/com/localai/hub/feature/*`
  - UI + ViewModels for chat, models, vision, and speech
- `app/src/main/java/com/arm/aichat` and `app/src/main/cpp`
  - Android `llama.cpp` wrapper adapted from the upstream example

## Run

```powershell
.\gradlew.bat :app:assembleDebug
```

Generated APK:

- `app/build/outputs/apk/debug/app-debug.apk`

## Notes

- `LlamaCppEngine.kt` now uses a preview fallback while the active chat model still points at the bundled placeholder path.
- Import a real `.gguf` file from the `Models` tab to switch chat onto actual on-device inference.
- `LiteRtEngine.kt`: replace demo OCR text with LiteRT/TFLite inference.
- `OnnxRuntimeEngine.kt`: replace demo transcript with ONNX Runtime Mobile inference.
