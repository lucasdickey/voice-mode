# Android Voice-to-Text with LLM Enhancement - Implementation Plan

## Executive Summary

This plan outlines the development of a native Android application that provides system-wide speech-to-text functionality with LLM-enhanced transcription. Inspired by Wispr Flow on macOS, the app will use Android Accessibility Services to enable long-press activation in any text field, transcribe speech, and use LLM inference to clean up proper nouns, improve grammar, and enhance semantic coherence.

---

## 1. Core Features

### 1.1 Primary Features
- **System-wide activation**: Long-press gesture in any text field to trigger dictation
- **Speech-to-text transcription**: Real-time audio capture and transcription
- **LLM post-processing**: Clean up proper nouns, fix grammar, improve sentence structure
- **Universal compatibility**: Works in any app with text input (Messages, Gmail, Slack, browsers, etc.)
- **Background service**: Always-ready dictation without launching the app
- **Visual feedback**: Overlay UI showing recording status and transcription progress

### 1.2 Secondary Features
- **Filler word removal**: Automatically filter out "um", "uh", "like", etc.
- **Custom dictionary**: Learn user-specific vocabulary and proper nouns
- **Multiple LLM backends**: Support for local and cloud-based models
- **Privacy modes**: Option for fully offline processing
- **Voice commands**: Special commands for punctuation, formatting, navigation

---

## 2. Technical Architecture

### 2.1 Core Components

#### A. Accessibility Service Layer
**Purpose**: System-level access to text fields and input interception

**Key Responsibilities**:
- Detect long-press gestures on text fields across all apps
- Monitor focused text input fields using `AccessibilityEvent.TYPE_VIEW_FOCUSED`
- Insert processed text using `AccessibilityNodeInfo.ACTION_SET_TEXT`
- Maintain context about the current text field and cursor position

**Implementation Details**:
- Extend `AccessibilityService` class
- Register for `TYPE_VIEW_TEXT_CHANGED`, `TYPE_VIEW_FOCUSED`, `TYPE_WINDOW_STATE_CHANGED` events
- Use `AccessibilityServiceInfo` configuration for capturing gestures
- Request permissions: `BIND_ACCESSIBILITY_SERVICE`, `SYSTEM_ALERT_WINDOW`

#### B. Gesture Detection Module
**Purpose**: Detect long-press activation trigger

**Key Responsibilities**:
- Monitor long-press gestures (configurable duration, default 500ms)
- Distinguish between regular taps and activation gestures
- Provide haptic/visual feedback on activation
- Handle gesture cancellation if user scrolls or moves finger

**Implementation Details**:
- Use `GestureDetector` or custom touch event handling
- Alternative approach: Use accessibility button or custom overlay button
- Consider floating action button as alternative to long-press for reliability

#### C. Audio Capture Module
**Purpose**: Record user's voice input

**Key Responsibilities**:
- Start/stop audio recording on gesture trigger
- Stream audio to transcription service
- Handle microphone permissions and access
- Support background recording
- Audio preprocessing (noise reduction, normalization)

**Implementation Details**:
- Use `MediaRecorder` or `AudioRecord` API
- Format: 16kHz, mono, 16-bit PCM (standard for speech recognition)
- Buffer management for streaming or batch processing
- Request permission: `RECORD_AUDIO`

#### D. Speech-to-Text Engine
**Purpose**: Convert audio to raw text transcription

**Options**:
1. **Google Speech Recognition API** (Cloud)
   - Built-in Android `SpeechRecognizer`
   - Fast, accurate, free tier available
   - Requires internet connection

2. **OpenAI Whisper** (Local or Cloud)
   - State-of-the-art accuracy
   - Multilingual support
   - Can run locally using Whisper.cpp Android port
   - Models: tiny (~75MB), base (~150MB), small (~500MB)

3. **Vosk** (Local)
   - Offline-first speech recognition
   - Smaller models (~50MB)
   - Good for privacy-focused users

**Recommended Approach**: Hybrid
- Primary: Whisper (local small model)
- Fallback: Google Speech Recognition (cloud)
- User configurable in settings

#### E. LLM Post-Processing Pipeline
**Purpose**: Enhance raw transcription for clarity and correctness

**Processing Steps**:
1. **Normalization**: Convert to proper case, basic punctuation
2. **Filler word removal**: Strip out "um", "uh", "like", "you know"
3. **Grammar correction**: Fix subject-verb agreement, tense consistency
4. **Proper noun detection**: Capitalize names, places, brands
5. **Semantic coherence**: Improve sentence structure and flow
6. **Context awareness**: Consider the app context (email vs. chat)

**LLM Options**:
1. **Local models** (using ONNX Runtime or TensorFlow Lite):
   - Phi-3-mini (3.8B parameters, ~2GB)
   - Gemma-2B (optimized for mobile)
   - TinyLlama (1.1B parameters, ~600MB)

2. **Cloud models**:
   - OpenAI GPT-4o-mini (fast, affordable)
   - Anthropic Claude Haiku (very fast, cost-effective)
   - Google Gemini Flash (multimodal, good for context)

**Prompt Engineering**:
```
Clean up the following speech transcription:
- Fix grammar and punctuation
- Capitalize proper nouns
- Remove filler words
- Improve sentence structure
- Maintain the speaker's intended meaning

Transcription: [RAW_TEXT]

Only output the cleaned text, no explanations.
```

#### F. Custom Dictionary & Learning
**Purpose**: Personalize transcription for user's vocabulary

**Key Responsibilities**:
- Track frequently used words and proper nouns
- Build user-specific vocabulary database
- Provide correction interface for misrecognized terms
- Export/import custom dictionaries

**Implementation Details**:
- SQLite database with word frequency tracking
- Fuzzy matching for correction suggestions
- Context-aware suggestions based on app usage
- Privacy: all data stored locally, encrypted

#### G. Overlay UI System
**Purpose**: Visual interface for recording and feedback

**Components**:
1. **Recording indicator**: Pulsing mic icon showing active recording
2. **Transcription preview**: Real-time text preview as it's processed
3. **Status messages**: "Processing with AI...", "Inserting text..."
4. **Controls**: Cancel, manual edit, insert buttons

**Implementation Details**:
- Use `WindowManager` with `TYPE_ACCESSIBILITY_OVERLAY`
- Material Design 3 components
- Translucent background with blur effect
- Position near focused text field or bottom-center
- Smooth animations for show/hide

#### H. Background Service Manager
**Purpose**: Keep app responsive and always-ready

**Key Responsibilities**:
- Manage long-running accessibility service
- Handle app lifecycle and power management
- Optimize battery usage with Doze mode compatibility
- Queue and process transcription requests
- Manage LLM model loading/unloading

**Implementation Details**:
- Use foreground service with ongoing notification
- WorkManager for background LLM processing
- Memory management to prevent OOM errors
- Lazy loading of heavy models

---

## 3. System Requirements

### 3.1 Android Version Support
- **Minimum SDK**: Android 7.0 (API 24) - Accessibility service improvements
- **Target SDK**: Android 14 (API 34) - Latest features and security
- **Recommended**: Android 10+ (API 29) - Better gesture navigation compatibility

### 3.2 Permissions Required
1. **Accessibility Service** - System-level text field access
2. **Record Audio** - Capture voice input
3. **Internet** (optional) - Cloud LLM services
4. **Foreground Service** - Background operation
5. **System Alert Window** - Draw overlay UI
6. **Wake Lock** (optional) - Prevent sleep during recording

### 3.3 Hardware Requirements
- **Microphone**: Required
- **RAM**: 4GB minimum (6GB+ recommended for local LLM)
- **Storage**: 500MB-2GB depending on model choice
- **Processor**: ARM64 architecture for optimal performance

---

## 4. User Experience Flow

### 4.1 First-Time Setup
1. **Welcome screen**: Explain app features and permissions needed
2. **Enable accessibility service**: Guide user to Settings → Accessibility
3. **Grant microphone permission**: Standard Android permission flow
4. **Choose processing mode**: Cloud vs. Local vs. Hybrid
5. **Download models** (if local): Progress indicator for model download
6. **Gesture tutorial**: Interactive demo of long-press activation
7. **Test dictation**: Practice in sandbox text field

### 4.2 Standard Dictation Flow
1. **User long-presses in any text field** (500ms hold)
2. **Haptic feedback + overlay appears** showing mic icon
3. **User speaks their message**
4. **Real-time transcription preview** (optional, in overlay)
5. **User releases or taps "Done"**
6. **"Processing with AI..." status shown**
7. **LLM enhances transcription** (1-3 seconds)
8. **Cleaned text inserted** at cursor position
9. **Overlay fades out**

### 4.3 Alternative Activation Methods
- **Accessibility button**: Android's built-in accessibility shortcut
- **Floating button**: Persistent overlay icon (like Messenger Chat Heads)
- **Hardware button**: Volume key long-press (optional)
- **Voice activation**: "Hey Assistant" hotword (future feature)

---

## 5. Privacy & Security

### 5.1 Data Handling
- **Audio data**: Never stored permanently, cleared after processing
- **Transcriptions**: Optionally stored locally for history/learning (encrypted)
- **Custom dictionary**: Stored locally only, never synced to cloud
- **Cloud processing**: Opt-in, with clear data policy disclosure

### 5.2 Security Measures
- **Encrypted storage**: SQLCipher for sensitive data
- **Secure transmission**: TLS 1.3 for cloud API calls
- **Permission validation**: Runtime permission checks
- **Sensitive field detection**: Disable in password fields automatically
- **Compliance**: GDPR, CCPA considerations for data processing

### 5.3 Privacy Modes
1. **Fully Offline**: Local speech recognition + local LLM (no internet)
2. **Hybrid**: Local transcription + cloud LLM (minimal data sharing)
3. **Cloud-Enhanced**: Cloud transcription + cloud LLM (best accuracy)

---

## 6. Technology Stack

### 6.1 Development
- **Language**: Kotlin (primary), Java (interop if needed)
- **Build System**: Gradle with Kotlin DSL
- **Architecture**: MVVM with Clean Architecture principles
- **Dependency Injection**: Hilt (Dagger)
- **Async**: Kotlin Coroutines + Flow
- **UI**: Jetpack Compose (modern, declarative)

### 6.2 Key Libraries
```kotlin
// Core Android
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

// Accessibility
implementation("androidx.accessibility:accessibility:1.2.0")

// UI
implementation("androidx.compose.ui:ui:1.6.0")
implementation("androidx.compose.material3:material3:1.2.0")

// Audio Processing
implementation("com.arthenica:mobile-ffmpeg-audio:4.4")

// Speech Recognition
// Whisper.cpp Android binding (community library)
implementation("com.whispercpp:whisper-android:1.5.0")

// Local LLM Inference
implementation("com.google.ai.edge.litert:litert:1.0.0")
implementation("ai.onnxruntime:onnxruntime-android:1.17.0")

// Cloud LLM APIs
implementation("com.openai:openai-kotlin:1.0.0")
implementation("com.anthropic:anthropic-sdk-kotlin:0.1.0")

// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
implementation("net.zetetic:android-database-sqlcipher:4.5.4")

// Dependency Injection
implementation("com.google.dagger:hilt-android:2.50")

// Testing
testImplementation("junit:junit:4.13.2")
testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
androidTestImplementation("androidx.test.ext:junit:1.1.5")
```

### 6.3 External Services (Optional)
- **OpenAI API**: GPT-4o-mini for cloud LLM processing
- **Anthropic API**: Claude Haiku for fast cloud processing
- **Hugging Face**: Model hosting and inference API

---

## 7. Development Phases

### Phase 1: Foundation (Weeks 1-2)
**Goal**: Basic app structure and accessibility service

**Deliverables**:
- [ ] Project setup with Gradle and dependencies
- [ ] Basic Android app with Jetpack Compose UI
- [ ] Accessibility service scaffolding
- [ ] Permission handling (accessibility, microphone)
- [ ] Text field detection and focus monitoring
- [ ] Basic overlay window with test UI
- [ ] Text insertion using ACTION_SET_TEXT

**Testing**:
- Verify accessibility service can detect text fields
- Confirm overlay appears on correct apps
- Test text insertion in 5+ different apps (Messages, Chrome, etc.)

### Phase 2: Audio & Basic Transcription (Weeks 3-4)
**Goal**: Voice recording and cloud-based transcription

**Deliverables**:
- [ ] Audio capture module using AudioRecord
- [ ] Long-press gesture detection in text fields
- [ ] Integration with Google Speech Recognition API
- [ ] Basic transcription flow: press → record → transcribe → insert
- [ ] Recording UI overlay with visual feedback
- [ ] Audio buffering and streaming implementation

**Testing**:
- Test in noisy and quiet environments
- Verify transcription accuracy baseline
- Test gesture detection reliability
- Check audio quality at different sampling rates

### Phase 3: LLM Post-Processing (Weeks 5-6)
**Goal**: Enhance transcriptions with LLM

**Deliverables**:
- [ ] Cloud LLM integration (OpenAI/Anthropic)
- [ ] LLM prompt engineering for transcription cleanup
- [ ] Post-processing pipeline: transcribe → enhance → insert
- [ ] Error handling for LLM API failures
- [ ] A/B testing framework to compare raw vs. enhanced
- [ ] Loading indicators and user feedback

**Testing**:
- Compare raw vs. LLM-enhanced transcriptions
- Test with various speech patterns and accents
- Measure latency and optimize for speed
- Test grammar correction, proper noun handling

### Phase 4: Local Models & Optimization (Weeks 7-9)
**Goal**: Offline capability with local models

**Deliverables**:
- [ ] Whisper.cpp integration for local transcription
- [ ] Model download and management system
- [ ] Local LLM inference (Phi-3-mini or Gemma-2B)
- [ ] Model quantization for smaller size
- [ ] Memory optimization and caching
- [ ] Battery usage optimization
- [ ] Settings UI for model selection

**Testing**:
- Benchmark local vs. cloud accuracy
- Measure battery impact during extended use
- Test on different device tiers (low/mid/high-end)
- Verify offline functionality

### Phase 5: Custom Dictionary & Learning (Weeks 10-11)
**Goal**: Personalization and vocabulary learning

**Deliverables**:
- [ ] SQLite database schema for custom words
- [ ] Word frequency tracking system
- [ ] Correction interface for misrecognized words
- [ ] Auto-learning from user corrections
- [ ] Import/export dictionary functionality
- [ ] Context-aware suggestions

**Testing**:
- Test learning accuracy over time
- Verify correction persistence
- Test with domain-specific vocabulary
- Check database performance with 1000+ custom entries

### Phase 6: Polish & Production (Weeks 12-14)
**Goal**: Production-ready app with full features

**Deliverables**:
- [ ] Complete settings UI (all configuration options)
- [ ] Usage statistics and history
- [ ] Multiple activation methods (long-press, floating button)
- [ ] Sensitive field detection (disable in passwords)
- [ ] Comprehensive error handling
- [ ] Privacy policy and data handling documentation
- [ ] App icon, branding, onboarding flow
- [ ] Performance profiling and optimization
- [ ] Accessibility compliance (ironically important!)
- [ ] App signing and release build configuration

**Testing**:
- Full regression testing across all features
- Security audit of data handling
- User acceptance testing with beta testers
- Performance testing on various devices
- Play Store pre-launch report validation

---

## 8. Technical Challenges & Solutions

### Challenge 1: Long-Press Detection Reliability
**Problem**: Android's gesture system may conflict with app-specific gestures

**Solutions**:
- Implement multiple activation methods (accessibility button, floating icon)
- Make long-press duration configurable
- Add visual cue showing gesture is registered
- Fallback to overlay button if gesture detection fails

### Challenge 2: Text Field Context Awareness
**Problem**: Different apps structure text fields differently

**Solutions**:
- Use AccessibilityNodeInfo traversal to find editable fields
- Implement fallback strategies for non-standard text inputs
- Test extensively across popular apps (top 50 apps)
- Maintain compatibility database for problematic apps

### Challenge 3: LLM Latency
**Problem**: 2-5 second delay feels slow for dictation

**Solutions**:
- Show raw transcription immediately (with disclaimer)
- Process LLM enhancement in background, update text when ready
- Use streaming LLM APIs for progressive enhancement
- Cache common phrases and corrections
- Optimize prompt length to reduce tokens

### Challenge 4: Model Size & Memory
**Problem**: Local LLMs require significant RAM (2-4GB)

**Solutions**:
- Use quantized models (4-bit, 8-bit) to reduce size by 75%
- Implement lazy loading - load model only when needed
- Unload models after idle period
- Offer multiple model sizes (tiny, small, medium)
- Cloud fallback for low-memory devices

### Challenge 5: Battery Consumption
**Problem**: Continuous accessibility service drains battery

**Solutions**:
- Use Doze mode compatibility (exemption request)
- Lazy initialization - don't load heavy models until first use
- Batch LLM processing when possible
- Use efficient audio encoding
- Implement usage patterns to predict need (load before likely use)

### Challenge 6: Privacy & Trust
**Problem**: Users concerned about accessibility service accessing all text

**Solutions**:
- Clear privacy policy and permissions explanation
- Open-source core components
- Audit logs showing what data was accessed (optional)
- Fully offline mode with local models
- Auto-disable in sensitive apps (banking, password managers)

---

## 9. Success Metrics

### 9.1 Technical Metrics
- **Transcription accuracy**: >95% word accuracy (measured against ground truth)
- **LLM enhancement quality**: >90% user satisfaction in A/B tests
- **Latency**: <3 seconds from speech end to text insertion
- **Battery impact**: <5% drain per hour of active use
- **Crash rate**: <0.1% of sessions
- **Memory usage**: <500MB peak during processing

### 9.2 User Experience Metrics
- **Activation success rate**: >98% (gesture recognized correctly)
- **Completion rate**: >90% of started dictations result in text insertion
- **User retention**: >60% weekly active users at 30 days
- **Feature adoption**: >50% of users try LLM enhancement
- **Custom dictionary usage**: >30% of users add custom words

### 9.3 Business Metrics (if applicable)
- **Daily active users**: Target based on market research
- **Average dictations per user per day**: >5 indicates high engagement
- **Premium conversion** (if freemium): >5% of free users
- **User reviews**: >4.5 stars on Play Store

---

## 10. Risk Assessment

### High Risk
1. **Google Play policy violations**: Accessibility services scrutinized heavily
   - *Mitigation*: Clear use case, detailed privacy policy, no data collection beyond stated purpose

2. **Accessibility service approval delay**: Can take weeks/months
   - *Mitigation*: Submit early, have alternative distribution (APK, F-Droid)

3. **LLM costs**: Cloud APIs can be expensive at scale
   - *Mitigation*: Implement usage caps, aggressive caching, local-first approach

### Medium Risk
1. **Device fragmentation**: Android ecosystem varies widely
   - *Mitigation*: Focus on popular devices first, extensive testing matrix

2. **Transcription accuracy**: Speech recognition still imperfect
   - *Mitigation*: LLM post-processing helps, custom dictionary, user corrections

3. **User adoption**: Accessibility services require setup effort
   - *Mitigation*: Excellent onboarding, clear value proposition, tutorials

### Low Risk
1. **Competition**: Existing dictation apps (Gboard, etc.)
   - *Mitigation*: LLM enhancement is differentiator, better UX

2. **Technical feasibility**: All components proven in other apps
   - *Mitigation*: Prototype early, validate each component

---

## 11. Future Enhancements

### Post-MVP Features
1. **Voice commands**: "new paragraph", "delete last sentence", "send"
2. **Multi-language support**: Automatic language detection
3. **Conversation mode**: Back-and-forth dictation for messaging
4. **Smart suggestions**: Predictive text based on context
5. **Desktop sync**: Continue dictation across devices
6. **Whisper mode**: Enhanced processing for quiet speech
7. **Speaker identification**: Multi-speaker transcription
8. **Punctuation commands**: Natural "comma", "period" recognition
9. **Code dictation**: Programming language-aware transcription
10. **Integration APIs**: Allow other apps to use the service

### Advanced LLM Features
1. **Style adaptation**: Match tone to app context (formal email vs. casual chat)
2. **Expansion**: "Coffee at 3" → "Would you like to meet for coffee at 3pm?"
3. **Translation**: Speak in one language, insert in another
4. **Summarization**: Speak long thoughts, insert concise version
5. **Command execution**: "Remind me..." creates actual reminder

---

## 12. Go-to-Market Strategy

### 12.1 Launch Plan
1. **Alpha testing** (Internal, 2 weeks): Core team testing
2. **Closed beta** (50 users, 4 weeks): Invite-only, heavy feedback
3. **Open beta** (Play Store, 8 weeks): Public testing, iterate rapidly
4. **v1.0 launch**: Full feature set, marketing push

### 12.2 Pricing Model Options

**Option A: Freemium**
- Free tier: 100 enhanced dictations/month, cloud-based
- Pro: $4.99/month - Unlimited, local models, custom dictionary
- Enterprise: $14.99/month - API access, team sharing

**Option B: One-time Purchase**
- $19.99 one-time - All features, lifetime updates
- Focus on privacy-conscious users

**Option C: Open Source + Donations**
- Fully free and open-source
- Optional donations or Patreon support
- Fastest user adoption, builds trust

**Recommended**: Option A (Freemium) for sustainability with Option C for community variant

### 12.3 Marketing Channels
1. **Product Hunt launch**: Target tech early adopters
2. **Reddit**: r/Android, r/productivity, r/accessibility
3. **YouTube demos**: Showcase LLM enhancement quality
4. **Developer blogs**: Write technical deep-dives
5. **Accessibility communities**: Highlight universal access benefits
6. **Comparison content**: "vs. Gboard", "vs. Google Voice Typing"

---

## 13. Recommended Next Steps

### Immediate Actions (Before Development Starts)
1. **Validate market demand**: Survey potential users, analyze competitor reviews
2. **Prototype gesture detection**: Quick test app to verify long-press feasibility
3. **Test LLM prompts**: Experiment with different prompts offline to optimize quality
4. **Choose initial LLM backend**: Start with one (suggest OpenAI) for MVP
5. **Set up development environment**: Install Android Studio, configure project
6. **Create project repository**: Initialize with README, license, contribution guidelines

### Week 1 Sprint Planning
1. Project setup and dependency configuration
2. Implement basic accessibility service skeleton
3. Create text field detection logic
4. Build simple overlay UI for testing
5. Test on 3 different apps to validate approach

---

## 14. Conclusion

This plan outlines a comprehensive approach to building an Android speech-to-text application with LLM enhancement, inspired by Wispr Flow's success on macOS. The key differentiators are:

1. **System-wide integration**: Works everywhere, not just in one app
2. **LLM enhancement**: Goes beyond transcription to improve clarity
3. **Privacy-first options**: Local processing for sensitive users
4. **Personalization**: Learning user vocabulary over time

The project is technically feasible with current Android APIs and AI models. The main challenges are user experience (gesture detection), performance optimization (local LLMs on mobile), and user trust (accessibility service permissions).

**Estimated timeline**: 14 weeks for full MVP
**Team size**: 1-2 Android developers, 1 ML engineer (part-time)
**Budget considerations**: Cloud LLM costs (estimate $0.01-0.05 per dictation)

The plan is structured to deliver value incrementally, with Phase 2 delivering a usable (though basic) dictation app, and subsequent phases adding the intelligent enhancement that differentiates this from existing solutions.

---

## Appendix A: Wispr Flow Feature Comparison

| Feature | Wispr Flow (macOS) | Our App (Android) | Notes |
|---------|-------------------|-------------------|-------|
| Press-and-hold activation | ✅ Function key | ✅ Long-press | Android uses touch gesture |
| System-wide compatibility | ✅ All apps | ✅ All apps | Via accessibility service |
| Filler word removal | ✅ | ✅ | LLM-based |
| Grammar correction | ✅ | ✅ | LLM-based |
| Whisper mode | ✅ | 🔄 Future | Requires enhanced audio processing |
| Auto-dictionary | ✅ | ✅ | Custom learning module |
| 100+ languages | ✅ | 🔄 Phase 2 | Depends on STT engine |
| Voice shortcuts/snippets | ✅ | 🔄 Future | Post-MVP feature |
| Background operation | ✅ | ✅ | Foreground service |
| Offline mode | ❌ Cloud only | ✅ Local models | Key differentiator |
| Privacy-first | ⚠️ Cloud processed | ✅ Configurable | Local option available |

✅ = Included in MVP, 🔄 = Planned for future, ❌ = Not included

---

## Appendix B: Key Android APIs Reference

### Accessibility Service
- `android.accessibilityservice.AccessibilityService`
- `android.view.accessibility.AccessibilityEvent`
- `android.view.accessibility.AccessibilityNodeInfo`

### Audio Capture
- `android.media.AudioRecord`
- `android.media.MediaRecorder`

### Overlay Windows
- `android.view.WindowManager`
- `android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY`

### Speech Recognition
- `android.speech.SpeechRecognizer`
- `android.speech.RecognitionListener`

### Permissions
- `android.permission.RECORD_AUDIO`
- `android.permission.BIND_ACCESSIBILITY_SERVICE`
- `android.permission.SYSTEM_ALERT_WINDOW`
- `android.permission.FOREGROUND_SERVICE`

---

**Document Version**: 1.0
**Last Updated**: 2025-11-13
**Status**: Planning Phase - Awaiting Approval to Proceed
