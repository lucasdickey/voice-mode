# Voice Mode - Android Speech-to-Text with LLM Enhancement

> System-wide voice dictation for Android with AI-powered transcription cleanup, inspired by Wispr Flow for macOS.

## Overview

Voice Mode is a native Android application that provides intelligent speech-to-text functionality across all apps. Simply long-press in any text field to activate dictation, and the app will transcribe your speech, clean it up with LLM post-processing, and insert perfectly formatted text.

### Key Features

- **System-wide activation**: Works in any app (Gmail, Messages, Chrome, Slack, etc.)
- **LLM enhancement**: Removes filler words, fixes grammar, capitalizes proper nouns
- **Privacy-first**: Optional fully offline mode with local models
- **Smart learning**: Custom dictionary that adapts to your vocabulary
- **Background operation**: Always ready, no need to open the app

## Project Status

**Current Phase**: Phase 1 Complete ✅ | Phase 2 Starting 🚀

**What's Working**:
- ✅ Complete Android project structure
- ✅ Accessibility Service detecting text fields system-wide
- ✅ Permission handling (microphone, overlay, accessibility)
- ✅ Text insertion into any app using ACTION_SET_TEXT
- ✅ Password and sensitive field detection
- ✅ Basic recording overlay UI
- ✅ Beautiful Material 3 setup wizard

**Next Up (Phase 2)**:
- Audio capture using AudioRecord
- Cloud transcription (Google Speech Recognition)
- End-to-end dictation flow
- Improved gesture detection

## Documentation

- [**PLAN.md**](PLAN.md) - Comprehensive implementation plan with phases, features, and timeline
- [**ARCHITECTURE.md**](ARCHITECTURE.md) - Technical architecture diagrams and system design
- [**DEVELOPMENT.md**](DEVELOPMENT.md) - Developer guide for building and testing
- [**QUICK_START.md**](QUICK_START.md) - Quick reference for developers
- **CONTRIBUTING.md** - Guidelines for contributing (coming soon)

## How It Works

1. **Long-press** in any text field to activate
2. **Speak** your message
3. **Release** when done
4. **AI processes** your speech - removes "um", "uh", fixes grammar
5. **Perfect text** appears in the field automatically

### Example

**You say**: "um so like I was thinking we should meet at uh java joes coffee shop tomorrow around three"

**App inserts**: "I was thinking we should meet at Java Joe's Coffee Shop tomorrow around 3pm."

## Technology Stack

- **Language**: Kotlin
- **Platform**: Android 7.0+ (API 24+)
- **Architecture**: MVVM with Clean Architecture
- **Key Technologies**:
  - Accessibility Services (system-level access)
  - Whisper.cpp (local speech recognition)
  - ONNX Runtime / TensorFlow Lite (local LLM)
  - Jetpack Compose (modern UI)
  - Room (encrypted database)

## Development Timeline

**Estimated**: 14 weeks from start to MVP

| Phase | Duration | Description |
|-------|----------|-------------|
| Phase 1 | 2 weeks | Foundation & Accessibility Service |
| Phase 2 | 2 weeks | Audio Capture & Basic Transcription |
| Phase 3 | 2 weeks | LLM Post-Processing |
| Phase 4 | 3 weeks | Local Models & Optimization |
| Phase 5 | 2 weeks | Custom Dictionary & Learning |
| Phase 6 | 3 weeks | Polish & Production Release |

See [PLAN.md](PLAN.md) for detailed phase breakdown.

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- Android device/emulator running Android 7.0+ (API 24+)
- 4GB+ RAM recommended for local model testing
- Microphone access

### Setup

```bash
# Open in Android Studio
# File → Open → Select voice-mode directory

# Sync Gradle dependencies (automatic on first open)
# Build → Sync Project with Gradle Files

# Build and run on device
./gradlew assembleDebug
./gradlew installDebug

# Or use Android Studio: Run → Run 'app'
```

See [DEVELOPMENT.md](DEVELOPMENT.md) for detailed setup instructions and testing guide.

## Permissions Required

- **Accessibility Service** - System-level text field access
- **Microphone** - Voice recording
- **Internet** (optional) - Cloud LLM services
- **Overlay Windows** - Display dictation UI

## Privacy & Security

- Audio is **never stored** permanently
- All processing can be done **100% offline**
- Custom dictionary stored **locally only** with encryption
- Automatic disable in **password fields** and sensitive apps
- Full control over cloud vs. local processing

## Inspiration

This project is inspired by [Wispr Flow](https://wisprflow.ai/) for macOS, bringing similar functionality to Android with enhanced privacy options and local processing capabilities.

## Contributing

We welcome contributions! Please see CONTRIBUTING.md for guidelines (coming soon).

## License

[To be determined - likely MIT or Apache 2.0]

## Roadmap

### MVP (v1.0) - Target: Q2 2025
- [x] Planning complete
- [x] **Phase 1: Accessibility service implementation** ✅
- [ ] **Phase 2: Audio capture & cloud transcription** (In Progress)
- [ ] Phase 3: Cloud LLM enhancement
- [ ] Phase 4: Local models & optimization
- [ ] Phase 5: Custom dictionary & learning
- [ ] Phase 6: Production polish & release

### Post-MVP (v1.1+)
- [ ] Local model support (offline mode)
- [ ] Custom dictionary with learning
- [ ] Voice commands (punctuation, formatting)
- [ ] Multi-language support
- [ ] Desktop sync

### Future Enhancements
- [ ] Whisper mode (quiet speech detection)
- [ ] Code dictation mode
- [ ] Style adaptation (formal vs. casual)
- [ ] Real-time translation

## Contact & Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/voice-mode/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/voice-mode/discussions)
- **Email**: [your-email@example.com]

## Acknowledgments

- [Wispr Flow](https://wisprflow.ai/) - Inspiration for the project
- [Whisper.cpp](https://github.com/ggerganov/whisper.cpp) - Local speech recognition
- [OpenAI Whisper](https://github.com/openai/whisper) - STT model
- Android Accessibility community

---

**Status**: Phase 1 complete! Phase 2 development starting. See [DEVELOPMENT.md](DEVELOPMENT.md) to start building and testing.

## Screenshots

*Screenshots coming soon - Phase 1 establishes the foundation. UI will be enhanced in subsequent phases.*

---

Built with ❤️ for the Android community
