# Quick Start - Testing Voice Mode APK

This guide gets you testing the Voice Mode app in 5 minutes.

## Step 1: Start the Backend Server (2 minutes)

```bash
cd backend
npm install
cp .env.example .env
```

Edit `.env` and set your preferred speech-to-text service:

### Option A: OpenAI Whisper (Easiest ⭐)
```env
PORT=3000
NODE_ENV=development
API_KEY=test-api-key-12345
```

Then update `backend/src/services/bedrockService.js` to use OpenAI:
```javascript
const OpenAI = require('openai');
const client = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });

async function transcribeAudio(audioBuffer, filename) {
  const transcription = await client.audio.transcriptions.create({
    file: new File([audioBuffer], filename),
    model: 'whisper-1'
  });
  return {
    transcription: transcription.text,
    confidence: 0.95,
    timestamp: new Date().toISOString()
  };
}
```

### Option B: Android Native (No Setup)
Uses device's built-in speech recognizer (works offline, fallback already configured).

### Option C: AWS Transcribe
See `SETUP_GUIDE.md` for detailed AWS setup.

Start the server:
```bash
npm run dev
```

Expected output:
```
Voice Mode Backend running on port 3000
Environment: development
```

Test it:
```bash
curl http://localhost:3000/health
```

## Step 2: Prepare the APK (30 seconds)

The APK is already built at:
```
app/build/outputs/apk/debug/app-debug.apk
```

If you modified the source, rebuild:
```bash
./gradlew assembleDebug
```

## Step 3: Install on Device/Emulator (1 minute)

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or open Android Studio and run the app directly:
```
Device menu → Run → app
```

## Step 4: Configure API Endpoint (1 minute)

Edit `app/src/main/java/com/voicemode/VoiceModeAccessibilityService.kt`:

```kotlin
private fun getBedrockApiEndpoint(): String {
    // Change this to your backend URL
    return "http://192.168.1.XXX:3000"  // Your computer's IP
}

private fun getBedrockApiKey(): String {
    // Change this to your backend API_KEY from .env
    return "test-api-key-12345"
}
```

Find your computer's IP:
```bash
# macOS/Linux
ipconfig getifaddr en0

# Windows
ipconfig
```

## Step 5: Test the App (1 minute)

1. Open an app with text input (Notes, WhatsApp, Email, etc.)
2. Focus on a text field
3. You should see a floating action button (FAB) with 🎤 icon
4. Tap the FAB
5. Say something ("Hello world", "Test message", etc.)
6. Wait 2-3 seconds (or tap again to stop recording)
7. See the transcribed text in the debug logs:
   ```bash
   adb logcat | grep "Transcribed\|Error\|Cloud\|ML"
   ```

## Troubleshooting

### "Connection refused"
- Backend server not running (start with `npm run dev`)
- Wrong IP address in app configuration
- Phone/emulator can't reach backend

### "API key mismatch"
- API_KEY in `.env` doesn't match what you put in app code
- Make sure they're identical

### "Failed to start recording"
- Microphone permission not granted
- Go to Settings → Apps → Voice Mode → Permissions → Microphone

### "No text appears"
- Check logs: `adb logcat | grep VoiceMode`
- Ensure backend health endpoint works: `curl http://localhost:3000/health`
- Check backend console for errors

### "Text injection not working"
- This is currently a TODO - app captures transcription but doesn't auto-insert into field yet
- Manual copy/paste works for testing

## What's Working

✅ Microphone recording
✅ Audio file creation
✅ HTTP communication with backend
✅ Transcription via cloud service
✅ Fallback to Android speech recognizer
✅ State management (idle/recording/processing)
✅ FAB state icons

## What's In Progress

🚧 Text injection into focused field
🚧 Persistent storage of transcriptions
🚧 Multi-language support

## What's Next

After confirming the above works:

1. Deploy backend to AWS Lambda or Heroku for remote testing
2. Implement text injection to actually fill text fields
3. Add transcription history database
4. Build release APK with signing
5. Publish to Google Play Store

## Quick Commands

```bash
# View all logs
adb logcat

# View only Voice Mode logs
adb logcat | grep VoiceMode

# View errors only
adb logcat | grep Error

# Clear logs
adb logcat -c

# View backend server logs
# (should be showing in terminal where you ran "npm run dev")
```

## Duration Summary

- Backend setup: 2 min
- APK installation: 1 min
- API configuration: 1 min
- First test: 1 min
- **Total: 5 minutes ⏱️**

## Next: Advanced Testing

See `SETUP_GUIDE.md` for:
- AWS Bedrock setup
- Heroku backend deployment
- Building release APK
- Firebase integration
- Production deployment

Good luck! 🚀
