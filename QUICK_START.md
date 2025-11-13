# Quick Start Guide for Developers

This guide provides a rapid overview for developers who want to understand the project structure and start contributing quickly.

## Project Overview in 60 Seconds

**What**: Android app for system-wide speech-to-text with LLM enhancement
**How**: Accessibility Service + Audio Capture + STT + LLM Post-Processing
**Why**: Make dictation actually useful by cleaning up transcriptions automatically

## Core Concept

```
User speaks → Audio captured → STT transcribes → LLM enhances → Insert text
             "um so like..."               "I think..."
```

## Critical Android APIs

1. **AccessibilityService** - Intercept text field interactions system-wide
2. **AudioRecord** - Capture microphone input
3. **WindowManager** - Show overlay UI (TYPE_ACCESSIBILITY_OVERLAY)
4. **AccessibilityNodeInfo.ACTION_SET_TEXT** - Insert processed text

## Architecture at a Glance

```
┌─────────────────────────────────────────────┐
│  Accessibility Service (Always running)     │
│                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │ Gesture  │→ │  Audio   │→ │   STT    │ │
│  │ Detector │  │ Capture  │  │ Engine   │ │
│  └──────────┘  └──────────┘  └────┬─────┘ │
│                                    │       │
│                              ┌─────▼─────┐ │
│                              │    LLM    │ │
│                              │ Processor │ │
│                              └─────┬─────┘ │
│                                    │       │
│  ┌──────────┐                ┌────▼─────┐ │
│  │ Overlay  │◀───────────────│  Insert  │ │
│  │    UI    │                │  Module  │ │
│  └──────────┘                └──────────┘ │
└─────────────────────────────────────────────┘
```

## Key Files (When Implemented)

```
app/src/main/
├── accessibility/
│   └── VoiceModeAccessibilityService.kt    ← Main entry point
├── audio/
│   └── AudioCaptureManager.kt              ← Record voice
├── transcription/
│   └── WhisperTranscriber.kt               ← Speech-to-text
├── llm/
│   └── LlmProcessor.kt                     ← Clean up text
└── ui/
    └── RecordingOverlay.kt                 ← Visual feedback
```

## Development Phases

| Phase | Goal | Key Deliverable |
|-------|------|-----------------|
| 1 | Foundation | Working accessibility service |
| 2 | Audio | Recording & cloud STT |
| 3 | Enhancement | LLM integration |
| 4 | Local | Offline models |
| 5 | Learning | Custom dictionary |
| 6 | Polish | Production ready |

## Quick Commands

```bash
# Start development
./gradlew assembleDebug

# Run tests
./gradlew test

# Install on device
./gradlew installDebug

# Check code style
./gradlew ktlintCheck

# Build release
./gradlew assembleRelease
```

## Testing Strategy

1. **Unit tests**: Individual components (70% coverage target)
2. **Integration tests**: End-to-end flow
3. **Manual tests**: Real device testing in 20+ apps
4. **Performance tests**: Battery, memory, latency benchmarks

## Common Gotchas

### 1. Accessibility Service Won't Start
- Must be manually enabled in Settings → Accessibility
- Can't be programmatically enabled (Android security)
- User must grant permission explicitly

### 2. Long-Press Detection Issues
- Different apps handle touch events differently
- Fallback: Offer accessibility button or floating icon
- Make duration configurable (default 500ms)

### 3. Memory Issues with Local Models
- Models are 600MB-2GB
- Use lazy loading
- Unload after idle period (2 minutes)
- Consider device RAM before loading

### 4. Text Insertion Failures
- Some apps use non-standard text inputs (WebViews)
- Implement fallback: clipboard copy
- Test extensively across popular apps

### 5. Battery Drain
- Accessibility service runs continuously
- Lazy-load heavy components
- Use Doze mode exemption carefully
- Profile regularly with Battery Historian

## LLM Integration Quick Reference

### Cloud (Phase 3)
```kotlin
// OpenAI example
val enhanced = openAiClient.chat.completions.create(
    model = "gpt-4o-mini",
    messages = listOf(
        ChatMessage(role = "system", content = CLEANUP_PROMPT),
        ChatMessage(role = "user", content = rawTranscription)
    ),
    temperature = 0.3
)
```

### Local (Phase 4)
```kotlin
// ONNX Runtime example
val session = OrtSession(modelPath)
val input = tokenizer.encode(rawTranscription)
val output = session.run(input)
val enhanced = tokenizer.decode(output)
```

## Performance Targets

| Metric | Target |
|--------|--------|
| Gesture detection | <100ms |
| STT (cloud) | <1s per 10s audio |
| LLM (cloud) | <2s for 50 words |
| Total latency | <5s end-to-end |
| Battery drain | <5% per hour |
| Memory peak | <3GB |

## Useful Resources

### Android Accessibility
- [Official Accessibility Service Guide](https://developer.android.com/guide/topics/ui/accessibility/service)
- [Accessibility Codelab](https://codelabs.developers.google.com/codelabs/developing-android-a11y-service)

### Speech Recognition
- [Whisper.cpp Android](https://github.com/ggerganov/whisper.cpp/tree/master/examples/whisper.android)
- [Google Speech Recognition](https://developer.android.com/reference/android/speech/SpeechRecognizer)

### LLM Integration
- [ONNX Runtime Android](https://onnxruntime.ai/docs/tutorials/mobile/)
- [TensorFlow Lite](https://www.tensorflow.org/lite/guide/android)

### Jetpack Compose
- [Compose Documentation](https://developer.android.com/jetpack/compose)
- [Compose Samples](https://github.com/android/compose-samples)

## Getting Help

1. Check [PLAN.md](PLAN.md) for detailed implementation plan
2. Review [ARCHITECTURE.md](ARCHITECTURE.md) for technical diagrams
3. Open an issue on GitHub
4. Join discussions in GitHub Discussions

## Contributing Quick Start

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Follow Kotlin style guide and MVVM patterns
4. Write tests for new functionality
5. Update documentation if needed
6. Submit a pull request

## Debug Tips

### Enable Verbose Logging
```kotlin
// In debug builds
if (BuildConfig.DEBUG) {
    Log.v("VoiceMode", "Detailed log message")
}
```

### Test Accessibility Service
```bash
# Check if service is running
adb shell dumpsys accessibility

# View accessibility events
adb shell settings put secure enabled_accessibility_services com.yourapp.voicemode/.VoiceModeAccessibilityService
```

### Profile Memory Usage
```bash
# Dump memory info
adb shell dumpsys meminfo com.yourapp.voicemode

# Use Android Studio Profiler for visual analysis
```

### Monitor Battery Impact
```bash
# Reset battery stats
adb shell dumpsys batterystats --reset

# Use app, then generate report
adb bugreport
# Open in Battery Historian
```

## Code Style

- Follow [Kotlin style guide](https://kotlinlang.org/docs/coding-conventions.html)
- Use ktlint for formatting
- Prefer Kotlin over Java
- Use Kotlin coroutines for async (not callbacks)
- Document public APIs with KDoc

## Next Steps

1. ⭐ **Star the repository** to show support
2. 📖 **Read PLAN.md** for full context
3. 🏗️ **Set up dev environment** (Android Studio)
4. 🧪 **Write a proof-of-concept** for accessibility service
5. 💬 **Join discussions** to coordinate with other contributors

---

**Ready to build?** Start with Phase 1 in [PLAN.md](PLAN.md) and create your first accessibility service!

**Questions?** Open an issue or discussion on GitHub.

**Found a bug?** Even though we haven't started coding yet, documentation bugs count! Submit a PR.
