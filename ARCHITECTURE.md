# Technical Architecture Diagrams

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         User's Android Device                    │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Any Android App                         │  │
│  │              (Gmail, Messages, Chrome, etc.)               │  │
│  │                                                             │  │
│  │        ┌─────────────────────────────────┐                │  │
│  │        │  Text Field (EditText/WebView)   │                │  │
│  │        │                                  │                │  │
│  │        │  [User long-presses here]       │                │  │
│  │        └─────────────┬───────────────────┘                │  │
│  └──────────────────────┼─────────────────────────────────────┘  │
│                         │                                         │
│                         │ (Accessibility Event)                   │
│                         ▼                                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │          Our Voice-to-Text Accessibility Service          │  │
│  │                                                             │  │
│  │  ┌─────────────┐    ┌──────────────┐    ┌──────────────┐ │  │
│  │  │   Gesture   │───▶│    Audio     │───▶│     STT      │ │  │
│  │  │  Detector   │    │   Capture    │    │    Engine    │ │  │
│  │  └─────────────┘    └──────────────┘    └──────┬───────┘ │  │
│  │                                                  │         │  │
│  │                                                  │         │  │
│  │                                         ┌────────▼───────┐ │  │
│  │                                         │  LLM Post-     │ │  │
│  │                                         │  Processor     │ │  │
│  │                                         └────────┬───────┘ │  │
│  │                                                  │         │  │
│  │  ┌─────────────┐                       ┌────────▼───────┐ │  │
│  │  │   Overlay   │◀──────────────────────│  Text Insert   │ │  │
│  │  │     UI      │                       │    Module      │ │  │
│  │  └─────────────┘                       └────────────────┘ │  │
│  │                                                             │  │
│  └──────────────────────────────────────────────────────────┘  │
│                         │                                         │
│                         │ (Insert cleaned text)                   │
│                         ▼                                         │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │               Text appears in original app                 │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Component Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Accessibility Service                         │
│                         (Main Entry Point)                           │
└────────────────────────────┬────────────────────────────────────────┘
                             │
            ┌────────────────┼────────────────┐
            │                │                │
┌───────────▼──────────┐ ┌──▼──────────────┐ ┌▼─────────────────────┐
│  Gesture Detection   │ │  Event Monitor   │ │   Overlay Manager    │
│      Module          │ │                  │ │                      │
├──────────────────────┤ ├──────────────────┤ ├──────────────────────┤
│ - Long-press detect  │ │ - Focus tracking │ │ - Show/hide overlay  │
│ - Haptic feedback    │ │ - Text changed   │ │ - Position overlay   │
│ - Activation trigger │ │ - Window state   │ │ - User interactions  │
└───────────┬──────────┘ └──┬───────────────┘ └─────────┬────────────┘
            │                │                           │
            └────────────────┼───────────────────────────┘
                             │
                  ┌──────────▼──────────┐
                  │  Recording Workflow  │
                  │    Coordinator       │
                  └──────────┬───────────┘
                             │
         ┌───────────────────┼───────────────────┐
         │                   │                   │
┌────────▼────────┐  ┌───────▼────────┐  ┌──────▼─────────┐
│  Audio Capture  │  │   STT Engine   │  │  LLM Processor │
│     Module      │  │                │  │                │
├─────────────────┤  ├────────────────┤  ├────────────────┤
│ - MediaRecorder │  │ - Whisper.cpp  │  │ - Prompt mgmt  │
│ - AudioRecord   │  │ - Google STT   │  │ - Model loader │
│ - Buffer mgmt   │  │ - Vosk (opt)   │  │ - Inference    │
│ - Noise reduce  │  │ - Streaming    │  │ - Caching      │
└────────┬────────┘  └───────┬────────┘  └────────┬───────┘
         │                   │                     │
         └───────────────────┼─────────────────────┘
                             │
                   ┌─────────▼──────────┐
                   │   Text Processor   │
                   └─────────┬──────────┘
                             │
         ┌───────────────────┼────────────────────┐
         │                   │                    │
┌────────▼──────────┐ ┌──────▼────────────┐ ┌───▼──────────────┐
│ Custom Dictionary │ │  Text Insertion   │ │  History/Stats   │
│                   │ │     Module        │ │                  │
├───────────────────┤ ├───────────────────┤ ├──────────────────┤
│ - Word frequency  │ │ - ACTION_SET_TEXT │ │ - Usage tracking │
│ - User corrections│ │ - Cursor position │ │ - Word count     │
│ - Context aware   │ │ - Undo support    │ │ - Accuracy logs  │
└───────────────────┘ └───────────────────┘ └──────────────────┘
         │                   │                    │
         └───────────────────┼────────────────────┘
                             │
                  ┌──────────▼───────────┐
                  │   Data Persistence   │
                  │                      │
                  │  - Room Database     │
                  │  - Encrypted storage │
                  │  - Preferences       │
                  └──────────────────────┘
```

---

## Data Flow Sequence

```
User                App           Service         Audio        STT         LLM        Dictionary
 │                   │               │              │           │           │             │
 │  1. Long-press    │               │              │           │           │             │
 │──────────────────▶│               │              │           │           │             │
 │                   │               │              │           │           │             │
 │                   │ 2. Event      │              │           │           │             │
 │                   │──────────────▶│              │           │           │             │
 │                   │               │              │           │           │             │
 │                   │               │ 3. Detect    │           │           │             │
 │                   │               │   gesture    │           │           │             │
 │                   │               │──────────────│           │           │             │
 │                   │               │              │           │           │             │
 │                   │◀─ 4. Show overlay ──────────│           │           │             │
 │◀──────────────────│               │              │           │           │             │
 │  (Mic icon)       │               │              │           │           │             │
 │                   │               │              │           │           │             │
 │  5. Speak         │               │              │           │           │             │
 │──────────────────────────────────▶│              │           │           │             │
 │                   │               │              │           │           │             │
 │                   │               │ 6. Record    │           │           │             │
 │                   │               │─────────────▶│           │           │             │
 │                   │               │              │           │           │             │
 │  7. Release       │               │              │           │           │             │
 │──────────────────────────────────▶│              │           │           │             │
 │                   │               │              │           │           │             │
 │                   │               │ 8. Stop      │           │           │             │
 │                   │               │─────────────▶│           │           │             │
 │                   │               │              │           │           │             │
 │                   │               │◀─ 9. Audio ──│           │           │             │
 │                   │               │    buffer    │           │           │             │
 │                   │               │              │           │           │             │
 │                   │               │ 10. Transcribe          │           │             │
 │                   │               │─────────────────────────▶│           │             │
 │                   │               │              │           │           │             │
 │                   │               │◀─ 11. Raw text ─────────│           │             │
 │                   │               │  "um so like..."         │           │             │
 │                   │               │              │           │           │             │
 │◀─ 12. Show preview ───────────────│              │           │           │             │
 │  (optional)       │               │              │           │           │             │
 │                   │               │              │           │           │             │
 │                   │               │ 13. Enhance text        │           │             │
 │                   │               │─────────────────────────────────────▶│             │
 │                   │               │              │           │           │             │
 │                   │               │              │           │           │ 14. Check   │
 │                   │               │              │           │           │  custom     │
 │                   │               │              │           │           │  words      │
 │                   │               │              │           │           │────────────▶│
 │                   │               │              │           │           │             │
 │                   │               │              │           │           │◀─ 15. Context
 │                   │               │              │           │           │    words    │
 │                   │               │              │           │           │             │
 │                   │               │◀─ 16. Clean text ────────────────────│             │
 │                   │               │  "So I think we should..." │         │             │
 │                   │               │              │           │           │             │
 │                   │               │ 17. Insert   │           │           │             │
 │                   │               │   to field   │           │           │             │
 │                   │               │──────────────│           │           │             │
 │                   │               │              │           │           │             │
 │                   │◀─ 18. Update text field ────│           │           │             │
 │◀──────────────────│               │              │           │           │             │
 │  (Text appears)   │               │              │           │           │             │
 │                   │               │              │           │           │             │
 │                   │               │ 19. Save to history     │           │             │
 │                   │               │─────────────────────────────────────────────────▶DB
 │                   │               │              │           │           │             │
 │                   │               │ 20. Hide overlay        │           │           │
 │◀──────────────────────────────────│              │           │           │             │
 │                   │               │              │           │           │             │
```

---

## Class Structure (Kotlin)

```
com.yourapp.voicemode
│
├── accessibility/
│   ├── VoiceModeAccessibilityService.kt    [Main service entry]
│   ├── GestureDetector.kt                  [Long-press detection]
│   ├── TextFieldMonitor.kt                 [Track focused fields]
│   └── TextInserter.kt                     [Insert processed text]
│
├── audio/
│   ├── AudioCaptureManager.kt              [Record audio]
│   ├── AudioBuffer.kt                      [Buffer management]
│   └── NoiseReducer.kt                     [Preprocessing]
│
├── transcription/
│   ├── TranscriptionEngine.kt              [Interface]
│   ├── WhisperTranscriber.kt               [Whisper.cpp impl]
│   ├── GoogleSttTranscriber.kt             [Google STT impl]
│   └── TranscriptionResult.kt              [Data class]
│
├── llm/
│   ├── LlmProcessor.kt                     [Interface]
│   ├── LocalLlmProcessor.kt                [ONNX/TFLite]
│   ├── CloudLlmProcessor.kt                [API calls]
│   ├── PromptBuilder.kt                    [Prompt engineering]
│   └── ModelManager.kt                     [Load/unload models]
│
├── dictionary/
│   ├── CustomDictionary.kt                 [User vocab]
│   ├── WordFrequencyTracker.kt             [Learning]
│   └── ContextAnalyzer.kt                  [App-aware]
│
├── ui/
│   ├── OverlayManager.kt                   [Show/hide overlay]
│   ├── RecordingOverlay.kt                 [Compose UI]
│   ├── SettingsActivity.kt                 [Configuration]
│   └── OnboardingActivity.kt               [First-time setup]
│
├── workflow/
│   ├── DictationWorkflow.kt                [Orchestrator]
│   ├── WorkflowState.kt                    [State machine]
│   └── WorkflowConfig.kt                   [User preferences]
│
├── data/
│   ├── database/
│   │   ├── AppDatabase.kt                  [Room DB]
│   │   ├── DictionaryDao.kt
│   │   ├── HistoryDao.kt
│   │   └── entities/
│   │       ├── CustomWord.kt
│   │       └── DictationHistory.kt
│   │
│   ├── repository/
│   │   ├── DictionaryRepository.kt
│   │   └── HistoryRepository.kt
│   │
│   └── preferences/
│       └── UserPreferences.kt              [DataStore]
│
├── di/
│   ├── AppModule.kt                        [Hilt modules]
│   ├── ServiceModule.kt
│   └── NetworkModule.kt
│
└── utils/
    ├── PermissionHelper.kt
    ├── Logger.kt
    └── Extensions.kt
```

---

## State Machine

```
┌─────────────┐
│    IDLE     │◀────────────────────────────┐
└──────┬──────┘                             │
       │                                    │
       │ User long-press detected           │
       │                                    │
       ▼                                    │
┌─────────────┐                             │
│  LISTENING  │                             │
└──────┬──────┘                             │
       │                                    │
       │ User releases / timeout            │
       │                                    │
       ▼                                    │
┌─────────────┐                             │
│ PROCESSING  │                             │
│   (STT)     │                             │
└──────┬──────┘                             │
       │                                    │
       │ Transcription complete             │
       │                                    │
       ▼                                    │
┌─────────────┐                             │
│ ENHANCING   │                             │
│   (LLM)     │                             │
└──────┬──────┘                             │
       │                                    │
       │ Enhancement complete / timeout     │
       │                                    │
       ▼                                    │
┌─────────────┐                             │
│  INSERTING  │                             │
└──────┬──────┘                             │
       │                                    │
       │ Text inserted successfully          │
       │                                    │
       └────────────────────────────────────┘

Error states:
  • Any state → ERROR → Show error message → IDLE
  • User cancels → CANCELLED → IDLE
```

---

## Permission Flow

```
┌───────────────┐
│  App Launch   │
└───────┬───────┘
        │
        ▼
┌──────────────────────────┐
│ Check Accessibility      │
│ Service Enabled?         │
└───────┬──────────────────┘
        │
        ├─ NO ──▶ ┌─────────────────────────┐
        │         │ Show setup instructions │
        │         │ Open Settings button    │
        │         └─────────────────────────┘
        │
        ▼ YES
┌──────────────────────────┐
│ Check Microphone         │
│ Permission?              │
└───────┬──────────────────┘
        │
        ├─ NO ──▶ ┌─────────────────────────┐
        │         │ Request RECORD_AUDIO    │
        │         └─────────────────────────┘
        │
        ▼ YES
┌──────────────────────────┐
│ Check Overlay            │
│ Permission?              │
└───────┬──────────────────┘
        │
        ├─ NO ──▶ ┌─────────────────────────┐
        │         │ Request SYSTEM_ALERT_   │
        │         │        WINDOW           │
        │         └─────────────────────────┘
        │
        ▼ YES
┌──────────────────────────┐
│ All permissions granted  │
│ Service ready            │
└──────────────────────────┘
```

---

## LLM Processing Options

```
┌─────────────────────────────────────────────────────────────┐
│                     User Configuration                       │
└────────────────┬────────────────────────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
        ▼                 ▼
┌───────────────┐   ┌──────────────┐
│  Cloud Mode   │   │  Local Mode  │
└───────┬───────┘   └──────┬───────┘
        │                  │
        │                  │
        ▼                  ▼
┌────────────────────────────────────┐
│        Hybrid Mode                 │
│                                    │
│  1. Try local first (fast)         │
│  2. If confidence low, use cloud   │
│  3. Cache results for future       │
└────────────────────────────────────┘

Cloud Options:
├─ OpenAI GPT-4o-mini
│  • Pros: Fast, accurate, affordable
│  • Cons: Requires internet, costs per request
│  • Latency: ~1-2 seconds
│
├─ Anthropic Claude Haiku
│  • Pros: Very fast, good at instruction following
│  • Cons: Requires internet, costs per request
│  • Latency: ~0.5-1 seconds
│
└─ Google Gemini Flash
   • Pros: Fast, multimodal capable
   • Cons: Requires internet, costs per request
   • Latency: ~1-2 seconds

Local Options:
├─ Phi-3-mini (3.8B params, ~2GB)
│  • Pros: Good quality, Microsoft-backed
│  • Cons: Large size, slower inference
│  • Latency: ~3-5 seconds
│
├─ Gemma-2B (2B params, ~1GB)
│  • Pros: Smaller, Google-backed
│  • Cons: Lower quality than larger models
│  • Latency: ~2-3 seconds
│
└─ TinyLlama (1.1B params, ~600MB)
   • Pros: Fast, small size
   • Cons: Lower quality, may miss nuances
   • Latency: ~1-2 seconds
```

---

## Memory Management Strategy

```
Application Lifecycle:

┌─────────────────────────────────────────────────────────┐
│                    App Started                          │
│                                                         │
│  Memory: ~50MB (base service + UI)                      │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              First Dictation Triggered                  │
│                                                         │
│  1. Load STT model (if local)                           │
│     Memory: +150-500MB (Whisper model)                  │
│                                                         │
│  2. Initialize audio buffers                            │
│     Memory: +10MB (audio buffers)                       │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              LLM Enhancement Needed                     │
│                                                         │
│  IF local mode:                                         │
│     Load LLM model (lazy loading)                       │
│     Memory: +600MB-2GB (quantized model)                │
│                                                         │
│  IF cloud mode:                                         │
│     Memory: +5MB (HTTP client + response buffer)        │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              After Idle Period (2 minutes)              │
│                                                         │
│  1. Unload LLM model                                    │
│     Memory: -600MB-2GB                                  │
│                                                         │
│  2. Keep STT model loaded (frequent use)                │
│                                                         │
│  Return to: ~200MB (service + STT + caches)             │
└─────────────────────────────────────────────────────────┘

Peak Memory Usage:
├─ Cloud mode: ~250MB
├─ Local STT only: ~700MB
└─ Full local mode: ~2.5-3GB (STT + LLM)

Recommendations:
• 4GB+ RAM devices: Full local mode supported
• 3GB RAM devices: Local STT + cloud LLM (hybrid)
• 2GB RAM devices: Cloud mode only
```

---

## Security Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    User's Device                        │
│                                                         │
│  ┌────────────────────────────────────────────────┐   │
│  │          Sensitive Data Handling                │   │
│  │                                                  │   │
│  │  1. Audio Recording                             │   │
│  │     ├─ Never stored to disk                     │   │
│  │     ├─ In-memory buffer only                    │   │
│  │     └─ Cleared immediately after processing     │   │
│  │                                                  │   │
│  │  2. Transcriptions (optional history)           │   │
│  │     ├─ Encrypted with AES-256                   │   │
│  │     ├─ SQLCipher database                       │   │
│  │     └─ User can disable history entirely        │   │
│  │                                                  │   │
│  │  3. Custom Dictionary                           │   │
│  │     ├─ Encrypted local storage                  │   │
│  │     └─ Never synced to cloud (by default)       │   │
│  │                                                  │   │
│  │  4. API Keys (if cloud mode)                    │   │
│  │     ├─ Android Keystore system                  │   │
│  │     └─ Never logged or exposed                  │   │
│  │                                                  │   │
│  └────────────────────────────────────────────────┘   │
│                                                         │
│  ┌────────────────────────────────────────────────┐   │
│  │         Sensitive Field Detection               │   │
│  │                                                  │   │
│  │  Auto-disable dictation in:                     │   │
│  │  • Password fields (inputType=textPassword)     │   │
│  │  • Banking apps (configurable blocklist)        │   │
│  │  • 2FA code fields                              │   │
│  │  • Credit card inputs                           │   │
│  │                                                  │   │
│  │  Indicators:                                     │   │
│  │  • AccessibilityNodeInfo.isPassword()           │   │
│  │  • inputType flags                              │   │
│  │  • App package name matching                    │   │
│  └────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                            │
                            │ (If cloud mode)
                            │
                            ▼
┌─────────────────────────────────────────────────────────┐
│                  Network Security                       │
│                                                         │
│  • TLS 1.3 for all API calls                            │
│  • Certificate pinning for known APIs                   │
│  • No data sent to analytics or tracking               │
│  • User consent required before any cloud processing   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## Testing Strategy

```
Unit Tests (70% coverage target)
├─ Audio Capture
│  ├─ Buffer management
│  ├─ Format conversion
│  └─ Error handling
│
├─ Transcription
│  ├─ Mock STT responses
│  ├─ Error recovery
│  └─ Timeout handling
│
├─ LLM Processing
│  ├─ Prompt generation
│  ├─ Response parsing
│  └─ Fallback logic
│
└─ Custom Dictionary
   ├─ Word learning
   ├─ Frequency tracking
   └─ Context matching

Integration Tests
├─ End-to-end dictation flow
├─ Accessibility service interaction
├─ Database operations
└─ API integrations (with mocks)

UI Tests (Espresso)
├─ Onboarding flow
├─ Settings configuration
└─ Permission requests

Manual Testing Matrix
├─ Top 20 apps (Gmail, Messages, Chrome, etc.)
├─ 3 device tiers (low/mid/high-end)
├─ 3 Android versions (10, 12, 14)
└─ 3 languages (English, Spanish, Mandarin)

Performance Tests
├─ Memory profiling during dictation
├─ Battery drain measurement
├─ Latency benchmarks (STT + LLM)
└─ Model loading time

Security Tests
├─ Permission validation
├─ Data encryption verification
├─ Sensitive field detection accuracy
└─ API key protection audit
```

---

## Deployment Architecture

```
Development Environment
├─ Android Studio (latest stable)
├─ Gradle 8.4+
├─ Kotlin 1.9+
└─ Java 17

Build Configurations
├─ Debug
│  ├─ Detailed logging
│  ├─ Debug models (smaller, faster)
│  └─ Test API keys
│
├─ Beta
│  ├─ Moderate logging
│  ├─ Production models
│  └─ Firebase Crashlytics
│
└─ Release
   ├─ Minimal logging
   ├─ Optimized models
   ├─ ProGuard/R8 obfuscation
   └─ APK signing

Distribution Channels
├─ Google Play Store
│  ├─ Internal testing (team)
│  ├─ Closed beta (50-100 users)
│  ├─ Open beta (public)
│  └─ Production release
│
├─ APK Direct Download
│  ├─ GitHub Releases
│  └─ Project website
│
└─ F-Droid (open source variant)
   └─ Fully FOSS version (no Google dependencies)

CI/CD Pipeline (GitHub Actions)
├─ On PR:
│  ├─ Lint checks
│  ├─ Unit tests
│  └─ Build APK
│
├─ On merge to main:
│  ├─ Full test suite
│  ├─ Integration tests
│  └─ Build signed APK
│
└─ On tag (vX.X.X):
   ├─ Build release APK
   ├─ Upload to Play Store (beta)
   └─ Create GitHub release
```

---

## Performance Targets

| Metric | Target | Measurement Method |
|--------|--------|-------------------|
| **Gesture detection latency** | <100ms | Time from touch to overlay shown |
| **Audio capture start** | <200ms | Time to first audio sample |
| **STT transcription (local)** | <2s per 10s audio | Benchmark with test clips |
| **STT transcription (cloud)** | <1s per 10s audio | API response time |
| **LLM enhancement (local)** | <3s for 50 words | Benchmark with test prompts |
| **LLM enhancement (cloud)** | <2s for 50 words | API response time |
| **Text insertion** | <50ms | Time from text ready to displayed |
| **Total latency (cloud)** | <5s for 10s speech | End-to-end measurement |
| **Total latency (local)** | <8s for 10s speech | End-to-end measurement |
| **Battery drain** | <5% per hour active use | Android Battery Historian |
| **Memory usage (peak)** | <3GB | Android Profiler |
| **APK size** | <100MB | Build output |
| **Model download size** | <500MB total | Network profiler |
| **Cold start time** | <2s | Time to service ready |

---

This technical architecture provides a comprehensive blueprint for implementing the voice-to-text application with LLM enhancement. Each diagram illustrates a different aspect of the system, from high-level data flow to detailed class structure.

The architecture prioritizes:
- **Modularity**: Each component is independent and testable
- **Performance**: Lazy loading, caching, and optimization throughout
- **Privacy**: Multiple modes to accommodate different user preferences
- **Reliability**: Fallback mechanisms and error handling at every layer
- **Maintainability**: Clear separation of concerns and well-defined interfaces
