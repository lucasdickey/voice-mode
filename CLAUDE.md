# CLAUDE.md - Voice Mode AI Assistant Guidelines

## Overview

This document serves as a guide for AI assistants (Claude) working on the Voice Mode project. It provides context on the current development status, project goals, and guidelines for making contributions.

## Current Development Status

**Overall Completion**: ~65-70% of full plan
**Current Phase**: Phase 3 (LLM Post-Processing) - Infrastructure Ready
**Last Updated**: 2025-11-18

### Phase Progress Summary

| Phase | Name | Status | Completion |
|-------|------|--------|-----------|
| 1 | Foundation & Accessibility Service | ✅ Nearly Complete | 95% |
| 2 | Audio & Transcription | ✅ Complete | 100% |
| 3 | LLM Post-Processing | ✅ Infrastructure Ready | 95% |
| 4 | Local Models & Optimization | ⏳ Not Started | 0% |
| 5 | Custom Dictionary & Learning | ⏳ Not Started | 0% |
| 6 | Polish & Production | ⏳ Foundation Only | 10% |

## What's Been Built

### ✅ Completed Components

**Android Application** (Kotlin, ~720 lines)
- Accessibility service with FAB overlay for system-wide activation
- Audio recording module using MediaRecorder (M4A format, 16kHz)
- Cloud transcription integration with OpenAI Whisper API
- Fallback offline speech recognition using Android native SpeechRecognizer
- MVVM architecture with Kotlin Flow for state management
- Secure credential storage using EncryptedSharedPreferences
- Material Design 3 UI with Jetpack Compose

**Backend Server** (Node.js/Express, ~200 lines)
- RESTful API with `/api/transcribe` and `/api/process-text` endpoints
- API key authentication middleware
- Health check endpoint
- Error handling middleware

**AWS Infrastructure** (Complete, CloudFormation)
- Lambda function (Node.js 18.x, 1GB memory, 300s timeout)
- API Gateway with HTTPS and custom authorizer
- DynamoDB table for transcription history with TTL
- S3 bucket for audio file storage with encryption
- CloudWatch for logging and monitoring
- Full IAM policy configuration for least-privilege access

**CI/CD Pipeline** (GitHub Actions)
- Automated deployment on main branch
- Multi-environment support (dev/staging/prod)
- OIDC authentication for AWS

**Documentation** (16 comprehensive guides)
- Architecture, setup, deployment, testing guides
- AWS configuration and troubleshooting
- Quick start and implementation summaries

## Critical TODOs (Blocking)

These items must be completed for the feature to work:

### 1. Text Injection to Active Field
**File**: `app/src/main/java/com/voicemode/VoiceModeAccessibilityService.kt:67`
**Status**: Marked as TODO
**Impact**: BLOCKING - App won't insert text without this
**Description**: Implement text injection to the currently focused text field using `AccessibilityNodeInfo.ACTION_SET_TEXT`

### 2. Complete LLM Integration
**File**: `backend/src/services/bedrockService.js`
**Status**: Infrastructure ready, implementation pending
**Impact**: BLOCKING - LLM enhancement won't work
**Description**: Integrate Claude API to process raw transcriptions and improve grammar, proper nouns, etc.

## Project Structure

```
voice-mode/
├── app/                           # Android application (Kotlin)
│   ├── src/main/java/com/voicemode/
│   │   ├── VoiceModeAccessibilityService.kt   # Main service (169 lines)
│   │   ├── VoiceInputViewModel.kt             # State management (115 lines)
│   │   ├── audio/AudioRecorder.kt             # Audio capture (105 lines)
│   │   ├── aws/BedrockService.kt              # HTTP API client (80 lines)
│   │   ├── speech/MLKitSpeechRecognizer.kt    # Fallback recognizer (123 lines)
│   │   └── config/ConfigManager.kt            # Secure storage (53 lines)
│   └── build.gradle
│
├── backend/                       # Node.js/Express backend
│   ├── src/index.js               # Server setup
│   ├── src/routes/transcription.js # API endpoints
│   ├── src/services/bedrockService.js # Whisper/Claude integration
│   └── package.json
│
├── aws-infrastructure/
│   ├── cloudformation.yaml        # IaC template
│   └── deploy.sh                  # Deployment script
│
├── .github/workflows/
│   └── deploy.yml                 # CI/CD pipeline
│
└── Documentation/
    ├── PLAN.md                    # Full development plan (14 weeks)
    ├── CLAUDE.md                  # This file
    ├── AGENT.md                   # Identical to CLAUDE.md
    ├── START_HERE.md              # Navigation guide
    ├── ARCHITECTURE.md            # System design
    └── (+ 11 other guides)
```

## Technology Stack

**Android**: Kotlin, Jetpack Compose, AndroidX, OkHttp, GSON
**Backend**: Node.js, Express, AWS SDK
**Infrastructure**: AWS Lambda, API Gateway, DynamoDB, S3, CloudWatch
**IaC**: CloudFormation YAML
**CI/CD**: GitHub Actions
**Transcription**: OpenAI Whisper API
**Security**: EncryptedSharedPreferences, TLS 1.3, API key authentication

## Development Guidelines

### Code Style & Best Practices

1. **Kotlin**: Follow Android official style guide
   - Use Kotlin idioms and coroutines
   - Prefer immutable data structures
   - Use sealed classes for state management

2. **Android Architecture**: MVVM with Clean Architecture
   - Separate concerns: UI, ViewModel, Services
   - Use StateFlow for state management
   - Dependency injection where appropriate

3. **Error Handling**: Always handle failures gracefully
   - Log errors with context
   - Provide fallback mechanisms where possible
   - Show user-friendly error messages

4. **Testing**: Write tests for new features
   - Unit tests for business logic
   - Integration tests for API calls
   - Mock external dependencies

### Commit Message Format

Use clear, descriptive commit messages:
```
feat: add feature name
fix: resolve issue description
docs: update documentation
refactor: improve code quality
test: add/update tests
```

### Creating PRs

When creating pull requests:
1. Reference the phase and deliverable
2. Describe what was implemented and why
3. Include testing approach
4. Note any breaking changes

## Next Steps

### Immediate (This Sprint)
1. **Complete text injection** to active text field
2. **Finish LLM integration** with Claude API
3. **End-to-end testing** of full transcription pipeline
4. **Deploy AWS infrastructure** to dev environment

### Short Term (Next 2-3 Sprints)
1. Refine error handling and user feedback
2. Optimize transcription latency
3. Add usage analytics and history
4. Set up testing environment

### Medium Term (Phases 4-6)
1. Implement local models (Whisper.cpp, Phi-3-mini)
2. Build custom dictionary system with learning
3. Polish UI/UX and production deployment
4. Performance optimization across devices

## Key Metrics & Goals

**Technical Metrics**:
- Transcription accuracy: >95% word accuracy
- LLM enhancement quality: >90% user satisfaction
- Latency: <3 seconds from speech end to text insertion
- Crash rate: <0.1% of sessions

**User Experience**:
- Activation success rate: >98%
- Completion rate: >90% of started dictations
- User retention: >60% weekly active at 30 days

## Important Notes

- **AWS Costs**: ~$104/month at 10K transcriptions/month
- **APK Size**: 8.9 MB (debug build)
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 34)
- **Privacy**: All audio cleared after processing, local storage encrypted

## Contact & Communication

For questions or clarifications:
1. Check relevant documentation in the `/Documentation` folder
2. Review commit history for context on implementation decisions
3. Check GitHub issues for known problems
4. Refer to PLAN.md for feature specifications

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2025-11-18 | Initial document created based on codebase review |

---

**Document Purpose**: Provide AI assistants with context, guidelines, and current status to facilitate productive contributions to the Voice Mode project.
