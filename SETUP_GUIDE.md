# Voice Mode - Complete Setup Guide

This guide covers everything needed to get Voice Mode running on your Android device and backend server.

## Table of Contents
1. [Backend Server Setup](#backend-server-setup)
2. [Android App Configuration](#android-app-configuration)
3. [AWS Bedrock Setup](#aws-bedrock-setup)
4. [Building and Testing APK](#building-and-testing-apk)
5. [Troubleshooting](#troubleshooting)

---

## Backend Server Setup

### Prerequisites
- Node.js 18+ (https://nodejs.org/)
- npm (comes with Node.js)
- AWS Account

### Step 1: Install Backend Dependencies

```bash
cd backend
npm install
```

### Step 2: Configure Environment Variables

```bash
cp .env.example .env
```

Edit `.env` with your values:

```env
# Server
PORT=3000
NODE_ENV=development

# AWS Configuration
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=your-aws-access-key
AWS_SECRET_ACCESS_KEY=your-aws-secret-key

# API Security
API_KEY=your-secure-random-api-key

# CORS
CORS_ORIGIN=*
```

**To get AWS credentials:**
1. Go to AWS Console → IAM → Users
2. Create new user or use existing
3. Create access key
4. Attach policy: `AmazonBedrockFullAccess` or `AmazonTranscribeFullAccess`

### Step 3: Start Backend Server

```bash
npm run dev
```

Expected output:
```
Voice Mode Backend running on port 3000
Environment: development
```

### Step 4: Test Backend

```bash
curl http://localhost:3000/health
```

Should return:
```json
{
  "status": "healthy",
  "timestamp": "2024-11-13T12:00:00.000Z",
  "uptime": 5.123
}
```

---

## Android App Configuration

### Step 1: Update Backend Endpoint

Edit `app/src/main/java/com/voicemode/VoiceModeAccessibilityService.kt`:

```kotlin
private fun getBedrockApiEndpoint(): String {
    return "http://your-backend-api.com:3000"  // Change this
}

private fun getBedrockApiKey(): String {
    return "your-api-key"  // Must match backend API_KEY
}
```

### Step 2: Secure Configuration (Recommended)

Instead of hardcoding credentials, use encrypted storage:

```kotlin
// In your MainActivity or onboarding screen:
val configManager = ConfigManager(context)
configManager.setApiEndpoint("http://your-backend-api.com:3000")
configManager.setApiKey("your-api-key")

// Later, use:
val endpoint = configManager.getApiEndpoint()
val apiKey = configManager.getApiKey()
```

### Step 3: Update build.gradle (if needed)

Ensure these dependencies are included:

```gradle
// AWS SDK
implementation 'software.amazon.awssdk:bedrockruntime:2.20.123'

// ML Kit Speech Recognition (fallback)
implementation 'com.google.android.gms:play-services-mlkit-speech-recognition:16.3.0'

// HTTP Client
implementation 'com.squareup.okhttp3:okhttp:4.11.0'
```

---

## AWS Bedrock Setup

### Option A: AWS Transcribe (Recommended)

1. **Enable AWS Transcribe:**
   - AWS Console → Transcribe
   - Ensure region is set to `us-east-1` (or your chosen region)

2. **Create IAM Policy:**
   ```json
   {
     "Version": "2012-10-17",
     "Statement": [
       {
         "Effect": "Allow",
         "Action": [
           "transcribe:StartTranscriptionJob",
           "transcribe:GetTranscriptionJob"
         ],
         "Resource": "*"
       }
     ]
   }
   ```

3. **Update Backend:**
   - Edit `backend/src/services/bedrockService.js`
   - Replace placeholder with AWS Transcribe API calls

### Option B: OpenAI Whisper API (Simplest)

1. **Get API Key:** https://platform.openai.com/api-keys
2. **Install package:** `npm install openai`
3. **Update bedrockService.js:**

```javascript
const OpenAI = require('openai');
const client = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });

async function transcribeAudio(audioBuffer, filename) {
  const file = new (require('stream').Readable)();
  file.push(audioBuffer);
  file.push(null);

  const transcription = await client.audio.transcriptions.create({
    file: file,
    model: 'whisper-1'
  });

  return {
    transcription: transcription.text,
    confidence: 0.95,
    timestamp: new Date().toISOString()
  };
}
```

### Option C: AssemblyAI (High Accuracy)

1. **Get API Key:** https://www.assemblyai.com/
2. **Install package:** `npm install assemblyai`
3. **Update bedrockService.js:**

```javascript
const { AssemblyAI } = require('assemblyai');
const client = new AssemblyAI({ apiKey: process.env.ASSEMBLYAI_API_KEY });

async function transcribeAudio(audioBuffer, filename) {
  const transcript = await client.transcripts.transcribe({
    audio_data: audioBuffer
  });

  return {
    transcription: transcript.text,
    confidence: 0.95,
    timestamp: new Date().toISOString()
  };
}
```

---

## Building and Testing APK

### Prerequisites
- Android Studio (https://developer.android.com/studio)
- Android SDK 34+
- Emulator or physical Android device (API 24+)

### Step 1: Build APK

```bash
# In project root
./gradlew assembleDebug
```

Output will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Step 2: Install on Device/Emulator

```bash
# Via Android Studio: Run → Run 'app'
# Or via ADB:
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Step 3: Configure Permissions

1. Open app → "Enable Accessibility Service" button
2. Go to Settings → Accessibility → Voice Mode
3. Enable the service

### Step 4: Test Speech Recognition

1. Open any text input field in another app
2. Tap the microphone floating action button
3. Speak something
4. Button will show ⏳ while processing
5. Transcribed text should appear in the input field

---

## Troubleshooting

### Backend Issues

#### "Missing credentials in configuration"
```bash
# Set AWS credentials:
export AWS_ACCESS_KEY_ID=your-key
export AWS_SECRET_ACCESS_KEY=your-secret
npm run dev
```

Or use AWS CLI:
```bash
aws configure
```

#### Port 3000 already in use
```bash
# Find and kill process on port 3000:
lsof -i :3000
kill -9 <PID>

# Or use different port:
PORT=3001 npm run dev
```

#### CORS errors
Ensure backend `.env` has:
```env
CORS_ORIGIN=http://your-app-ip:port
```

### Android App Issues

#### "Failed to start recording"
- Check RECORD_AUDIO permission is granted
- Settings → Apps → Voice Mode → Permissions → Microphone
- Ensure Android version is 6.0+ for runtime permissions

#### "Accessibility Service failed"
- Restart the phone
- Re-enable accessibility service in Settings

#### "API connection timeout"
- Verify backend server is running
- Check backend URL is correct in app code
- Ensure device can reach backend (check network)
- Use `adb logcat` to see detailed errors

#### "ML Kit fallback not working"
- Ensure `play-services-mlkit-speech-recognition` is installed
- Check Google Play Services is up to date on device
- Device requires English language pack downloaded

### Testing Commands

```bash
# Test backend health
curl http://localhost:3000/health

# Test transcription endpoint
curl -X POST http://localhost:3000/api/transcribe \
  -H "Authorization: Bearer your-api-key" \
  -H "Content-Type: application/json" \
  -d '{"audio": "base64-audio-data", "filename": "test.m4a"}'

# View Android app logs
adb logcat | grep VoiceMode
```

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                     Android Device                           │
│  ┌───────────────────────────────────────────────────────┐  │
│  │         Voice Mode Accessibility Service               │  │
│  │  ┌──────────────────────────────────────────────────┐  │  │
│  │  │ AudioRecorder (device mic) → Save as M4A         │  │  │
│  │  │ VoiceInputViewModel (orchestrate recognition)    │  │  │
│  │  │ BedrockService (HTTP client to backend)         │  │  │
│  │  │ MLKitSpeechRecognizer (fallback, on-device)     │  │  │
│  │  └──────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────┘  │
└──────────────────────────────┬───────────────────────────────┘
                               │ HTTP POST (M4A audio as base64)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                   Backend Server (Node.js)                  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Express Server                                         │  │
│  │  ┌──────────────────────────────────────────────────┐  │  │
│  │  │ POST /api/transcribe (receives audio)            │  │  │
│  │  │   → Decode base64 → Call speech service         │  │  │
│  │  │   → Return transcribed text                      │  │  │
│  │  │ POST /api/process-text (optional)                │  │  │
│  │  │   → Process with Claude via Bedrock             │  │  │
│  │  └──────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────┘  │
│         │                    │                    │          │
│         ▼                    ▼                    ▼          │
│  ┌────────────────┐  ┌────────────────┐  ┌─────────────┐   │
│  │ AWS Transcribe │  │ OpenAI Whisper │  │ AssemblyAI  │   │
│  │   (Option A)   │  │  (Option B)    │  │ (Option C)  │   │
│  └────────────────┘  └────────────────┘  └─────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

---

## Next Steps

1. **Set up monitoring:** Use Firebase Crashlytics
2. **Add user authentication:** Firebase Auth or Auth0
3. **Persist history:** Add database (Firebase Firestore or PostgreSQL)
4. **Improve UI:** Add visual feedback during recording
5. **Deploy backend:** AWS Lambda, Heroku, or DigitalOcean
6. **Test thoroughly:** Record various accents and languages

---

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Check Android logcat: `adb logcat | grep VoiceMode`
3. Check backend logs: Look at terminal output
4. Enable debug mode in `.env`: `NODE_ENV=development`
