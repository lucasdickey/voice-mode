# Quick Start: Local End-to-End Testing

This guide walks you through testing the complete Voice Mode pipeline locally **without** needing AWS infrastructure.

**Time required**: ~30 minutes
**Cost**: $0
**Prerequisites**: Android Studio, Node.js 18+, a device/emulator with microphone

---

## Step 1: Prepare the Backend (5 minutes)

### 1.1 Install Dependencies

```bash
cd backend
npm install
```

Expected output: ~20 packages installed

### 1.2 Create Local Environment File

```bash
cat > .env << 'EOF'
PORT=3000
BEDROCK_REGION=us-east-1
BEDROCK_MODEL_ID=anthropic.claude-3-5-sonnet-20241022
NODE_ENV=development
API_KEY=test-api-key-12345
EOF
```

### 1.3 Start the Backend Server

```bash
npm start
```

Expected output:
```
Server running on http://localhost:3000
Health check available at GET /health
```

✅ **Backend is running!** Leave this terminal open.

---

## Step 2: Configure the Android App (10 minutes)

### 2.1 Update API Endpoint & Key

**Option A: Using Android Emulator (Recommended)**

The emulator has special access to localhost:
- Host `localhost:3000` → `10.0.2.2:3000` (in emulator)

**Option B: Using Physical Device**

You need the IP of your computer:
```bash
# macOS
ifconfig | grep "inet " | grep -v 127.0.0.1

# Linux
hostname -I

# Windows (in PowerShell)
ipconfig | findstr "IPv4"
```

### 2.2 Update ConfigManager

Edit: `app/src/main/java/com/voicemode/config/ConfigManager.kt`

Find the getters for API endpoint and key, and update them:

```kotlin
private fun getBedrockApiEndpoint(): String {
    // For emulator:
    return "http://10.0.2.2:3000"

    // For physical device, use your computer's IP:
    // return "http://192.168.1.100:3000"  // Replace with your IP
}

private fun getBedrockApiKey(): String {
    return "test-api-key-12345"  // Must match .env API_KEY
}
```

**OR use EncryptedSharedPreferences properly** (better practice):

```kotlin
// In ConfigManager.kt - load from secure storage
fun getApiEndpoint(): String {
    val sharedPref = context.getSharedPreferences("voicemode_config", Context.MODE_PRIVATE)
    return sharedPref.getString("api_endpoint", "http://10.0.2.2:3000") ?: "http://10.0.2.2:3000"
}

fun getApiKey(): String {
    val sharedPref = context.getSharedPreferences("voicemode_config", Context.MODE_PRIVATE)
    return sharedPref.getString("api_key", "test-api-key-12345") ?: "test-api-key-12345"
}
```

### 2.3 Build the APK

```bash
cd app
./gradlew clean assembleDebug
```

Expected output:
```
BUILD SUCCESSFUL in 45s
APK: app/build/outputs/apk/debug/app-debug.apk
```

---

## Step 3: Install & Configure App (10 minutes)

### 3.1 Install on Emulator or Device

**For Android Emulator:**
```bash
# Start emulator first (or use Android Studio)
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**For Physical Device:**
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Expected output:
```
Success
```

### 3.2 Enable Accessibility Service

1. Open the app on your device
2. Go to **Settings** → **Accessibility**
3. Find **Voice Mode** in the accessibility services list
4. Toggle **ON**
5. Confirm the prompt

You should see the floating microphone button (🎤) appear in text fields.

### 3.3 Grant Microphone Permission

1. Open any text field (e.g., Notes app)
2. Tap the 🎤 FAB button
3. Android will prompt for microphone permission
4. Grant **Allow**

---

## Step 4: Test the Flow (5 minutes)

### 4.1 Open a Test App

Use any app with a text field:
- Android Notes
- Messages
- Gmail
- Browser address bar
- Your own test activity

### 4.2 Test Recording

1. Focus on a text field (FAB 🎤 should appear)
2. **Tap the FAB** - it should change to ⏹️ (stop icon)
3. **Speak clearly**: "Hello world, this is a test"
4. **Wait 2 seconds** for silence detection
5. **FAB should show ⏳** (processing)

### 4.3 Verify Backend Processing

Check the backend terminal:

```
Raw transcription: "hello world this is a test"
Processing with Claude...
Enhanced: "Hello, world. This is a test."
```

### 4.4 Verify Text Injection

After 2-3 seconds, you should see:
- FAB returns to 🎤
- **Text appears in the field**: "Hello, world. This is a test."

✅ **Success!**

---

## Step 5: Troubleshooting

### Problem: "Connection refused" or app crashes

**Check 1**: Is backend running?
```bash
curl http://localhost:3000/health
# Should return: {"status":"ok"}
```

**Check 2**: Is API endpoint correct?
- Emulator: `http://10.0.2.2:3000`
- Physical device: Use actual IP (not localhost)

**Check 3**: Is API key correct?
- Backend `.env`: `API_KEY=test-api-key-12345`
- App ConfigManager: `"test-api-key-12345"`

### Problem: FAB doesn't appear

**Check 1**: Is Accessibility Service enabled?
```bash
adb shell settings get secure enabled_accessibility_services
# Should show com.voicemode/.VoiceModeAccessibilityService
```

**Check 2**: Is the text field editable?
- Try in Android Notes (guaranteed to work)
- Some web fields may not be accessible

### Problem: Recording starts but doesn't transcribe

**Check 1**: Look at backend logs for errors
```
# Terminal where backend is running
Error transcribing audio: ...
```

**Check 2**: Check Android logcat for errors
```bash
adb logcat | grep "VoiceMode"
```

**Check 3**: Is the audio file being created?
```bash
ls -la /tmp/audio_*.m4a
```

### Problem: Transcription works but LLM enhancement fails

This is OK! The app should fall back to raw transcription:
- Raw: `"hello world this is a test"`
- Enhanced (failed): Falls back to raw text
- Text still gets injected

**To fix**: Check backend logs for Claude/Bedrock errors

---

## Understanding the Data Flow

```
┌─────────────────────────────────────────────────────┐
│ Android App (on emulator/device)                    │
├─────────────────────────────────────────────────────┤
│ 1. User taps FAB button                             │
│ 2. Record audio → save to /tmp/audio_*.m4a          │
│ 3. Read audio file & Base64 encode                  │
│ 4. POST /api/transcribe {audio, filename}           │
│    └─ Authorization: Bearer test-api-key-12345      │
└────────────────────┬────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────┐
│ Backend (Node.js on localhost:3000)                 │
├─────────────────────────────────────────────────────┤
│ 5. Verify API key ✓                                 │
│ 6. Decode Base64 audio                              │
│ 7. Save to /tmp/audio_*.m4a                         │
│ 8. Call bedrockService.transcribeAudio()            │
│ 9. Return { transcription, confidence }             │
│ 10. POST /api/process-text {text}                   │
│    └─ Calls Claude via Bedrock (if AWS creds set)   │
│       OR uses mock response                         │
│ 11. Return { processed, original, timestamp }       │
└────────────────────┬────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────┐
│ Android App (Continued)                             │
├─────────────────────────────────────────────────────┤
│ 12. Receive enhanced text in response               │
│ 13. Update UI state to Success                      │
│ 14. injectTextToActiveField(text)                   │
│     └─ ACTION_SET_TEXT or ACTION_PASTE              │
│ 15. Text appears in text field ✓                    │
└─────────────────────────────────────────────────────┘
```

---

## Testing Different Scenarios

### Test 1: Basic Transcription (No Enhancement)

Temporarily disable enhancement in `VoiceInputViewModel.kt`:

```kotlin
private fun transcribeAudio(audioFile: java.io.File) {
    // ... existing code ...
    when (result) {
        is BedrockService.TranscriptionResult.Success -> {
            Log.d(TAG, "Cloud transcription successful: ${result.text}")
            // SKIP enhancement for this test
            _transcribedText.value = result.text
            _uiState.value = VoiceInputState.Success(result.text)
        }
        // ... rest of code ...
    }
}
```

**Expected**: Raw transcription appears (with grammar errors, filler words, etc.)

### Test 2: Enhancement Only (No Transcription)

Test the `/api/process-text` endpoint directly:

```bash
curl -X POST http://localhost:3000/api/process-text \
  -H "Authorization: Bearer test-api-key-12345" \
  -H "Content-Type: application/json" \
  -d '{"text":"um hello world like this is a test you know"}'
```

Expected response:
```json
{
  "success": true,
  "original": "um hello world like this is a test you know",
  "processed": "Hello, world. This is a test.",
  "timestamp": "2025-11-18T...",
  "model": "anthropic.claude-3-5-sonnet-20241022"
}
```

### Test 3: Fallback to ML Kit (No Cloud)

Temporarily break the backend connection:

```bash
# Stop the backend (Ctrl+C)
# Try recording again
# App should fallback to Android native speech recognition
```

Expected: Text still appears, but uses Android's built-in recognizer

---

## Next Steps After Testing

### ✅ Tests Pass?
1. Commit working code
2. Move to AWS deployment (Option #2)
3. Deploy to production

### ❌ Tests Fail?
1. Check logs: `adb logcat | grep -i "voicemode\|bedrock"`
2. Check backend terminal for errors
3. Verify API key matches between app and backend
4. Verify endpoint URL is correct for your setup

---

## Stopping the Backend

When done testing:

```bash
# In the terminal running backend
Ctrl+C

# Verify it stopped
curl http://localhost:3000/health
# Should fail with "Connection refused"
```

---

## Summary

| Step | Command | Time |
|------|---------|------|
| 1. Backend setup | `npm install && npm start` | 5 min |
| 2. App config | Edit ConfigManager.kt | 5 min |
| 3. Build APK | `./gradlew clean assembleDebug` | 5 min |
| 4. Install | `adb install` | 2 min |
| 5. Enable service | Settings → Accessibility | 3 min |
| 6. Test | Tap FAB → Speak → Verify | 5 min |
| **Total** | | **25 min** |

---

**Good luck! Let me know if you hit any blockers.** 🚀
