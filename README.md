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

**Current Phase**: Planning ✅

This project is currently in the planning stage. See [PLAN.md](PLAN.md) for the complete implementation plan and [ARCHITECTURE.md](ARCHITECTURE.md) for technical architecture diagrams.

## Documentation

- [**PLAN.md**](PLAN.md) - Comprehensive implementation plan with phases, features, and timeline
- [**ARCHITECTURE.md**](ARCHITECTURE.md) - Technical architecture diagrams and system design
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

### Setup (Coming Soon)

```bash
# Clone the repository
git clone https://github.com/yourusername/voice-mode.git

# Open in Android Studio
# File → Open → Select voice-mode directory

# Sync Gradle dependencies
# Build → Sync Project with Gradle Files

# Run on device
# Run → Run 'app'
```

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
- [ ] Accessibility service implementation
- [ ] Audio capture & cloud transcription
- [ ] Cloud LLM enhancement
- [ ] Basic UI and settings

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

**Note**: This project is currently in planning phase. Star and watch this repository to be notified when development begins!

## Screenshots

*Coming soon - mockups and actual app screenshots will be added as development progresses.*

---

Built with ❤️ for the Android community
