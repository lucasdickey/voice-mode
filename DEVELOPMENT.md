# Development Guide

## Phase 1 Implementation Status ✅

Phase 1 (Foundation & Accessibility Service) is now complete! Here's what has been implemented:

### Completed Components

#### 1. Project Structure
- ✅ Android project with Gradle Kotlin DSL
- ✅ Hilt dependency injection setup
- ✅ Jetpack Compose UI framework
- ✅ Timber logging
- ✅ Material Design 3 theming

#### 2. Main Application
- ✅ `VoiceModeApplication` - Application class with Hilt
- ✅ `MainActivity` - Setup wizard with permission checks
- ✅ `MainViewModel` - Permission monitoring and state management
- ✅ Beautiful Material 3 UI showing setup status

#### 3. Accessibility Service
- ✅ `VoiceModeAccessibilityService` - Core service
- ✅ `TextFieldMonitor` - Tracks focused text fields
- ✅ `GestureDetectorModule` - Gesture detection scaffolding
- ✅ `TextInserter` - Insert text using ACTION_SET_TEXT
- ✅ `OverlayManager` - Recording overlay UI

#### 4. Features
- ✅ System-wide text field detection
- ✅ Password field detection (auto-disable)
- ✅ Sensitive field detection (banking, 2FA, etc.)
- ✅ Text insertion with clipboard fallback
- ✅ Basic overlay UI
- ✅ Permission handling workflow

## Building the Project

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK 34
- Android device or emulator with Android 7.0+ (API 24+)

### Setup Steps

1. **Clone and open project**
   ```bash
   cd /home/user/voice-mode
   # Open in Android Studio: File → Open → Select voice-mode directory
   ```

2. **Sync Gradle**
   - Android Studio will prompt to sync
   - Or manually: Build → Sync Project with Gradle Files
   - Wait for dependencies to download

3. **Build the app**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device**
   ```bash
   ./gradlew installDebug
   ```

   Or use Android Studio:
   - Run → Run 'app'
   - Select your device/emulator

### First Run Setup

1. **Launch Voice Mode app**
   - You'll see the setup screen with three permission cards

2. **Grant Microphone Permission**
   - Tap "Grant" on Microphone card
   - Allow when prompted

3. **Grant Overlay Permission**
   - Tap "Grant" on Display Overlay card
   - Enable in Settings

4. **Enable Accessibility Service** (Most Important!)
   - Tap "Grant" on Accessibility Service card
   - Opens Settings → Accessibility
   - Find "Voice Mode Dictation"
   - Toggle ON
   - Confirm in dialog

5. **Return to app**
   - All three checkmarks should be green
   - Status should show "All Set!"

## Testing the Accessibility Service

### Basic Text Field Detection Test

1. **Open any app with a text field** (Messages, Chrome, Gmail, etc.)

2. **Tap in a text field**
   - Check Logcat for: `Text field focused - Package: ...`

3. **Check the logs**
   ```bash
   adb logcat | grep VoiceMode
   ```

   You should see:
   ```
   VoiceMode Accessibility Service connected
   Text field focused - Package: com.google.android.gms, Class: android.widget.EditText
   ```

### Testing Text Insertion

Currently, the service doesn't auto-activate dictation (that comes in Phase 2 with audio capture). To test the text insertion functionality:

**Option 1: Add a test button in MainActivity**
```kotlin
// Add this in MainActivity's Compose UI:
Button(onClick = {
    // Trigger test insertion
    val service = ... // Get service reference
    service.insertText("Hello from Voice Mode!")
}) {
    Text("Test Text Insertion")
}
```

**Option 2: Trigger via debugger**
- Set breakpoint in `VoiceModeAccessibilityService`
- When text field is focused, manually call `insertText("test")`

### Password Field Detection Test

1. Open an app with a password field
2. Tap in the password field
3. Check logs for: `Password field detected - dictation disabled`

### Overlay Test

The overlay will appear in Phase 2 when audio recording starts. For now, you can test it by:

1. Calling `overlayManager?.showRecordingOverlay()` from the service
2. You should see a dark overlay with "🎤 Recording..." text
3. Call `overlayManager?.hideOverlay()` to dismiss

## Project Structure

```
app/src/main/
├── java/com/voicemode/
│   ├── VoiceModeApplication.kt          # App entry point
│   ├── accessibility/
│   │   ├── VoiceModeAccessibilityService.kt  # Main service
│   │   ├── TextFieldMonitor.kt               # Text field tracking
│   │   ├── GestureDetectorModule.kt          # Gesture detection
│   │   ├── TextInserter.kt                   # Text insertion
│   │   └── OverlayManager.kt                 # Overlay UI
│   └── ui/
│       ├── MainActivity.kt                    # Main screen
│       ├── MainViewModel.kt                   # ViewModel
│       └── theme/                             # Compose theme
├── res/
│   ├── xml/
│   │   └── accessibility_service_config.xml   # Service config
│   ├── values/
│   │   ├── strings.xml
│   │   ├── themes.xml
│   │   └── colors.xml
│   └── mipmap-*/                              # Launcher icons
└── AndroidManifest.xml
```

## Debugging Tips

### Enable Verbose Logging

All logging is done through Timber, which is automatically enabled in debug builds.

View logs:
```bash
# All Voice Mode logs
adb logcat -s VoiceMode:*

# With timestamp
adb logcat -v time -s VoiceMode:*

# Save to file
adb logcat -v time -s VoiceMode:* > voicemode.log
```

### Check if Service is Running

```bash
# List all accessibility services
adb shell dumpsys accessibility

# Look for:
# Service[label=Voice Mode Dictation, ...]: enabled=true
```

### Common Issues

#### 1. Service won't start
- **Problem**: Accessibility service not appearing in Settings
- **Solution**:
  - Check AndroidManifest.xml has correct service declaration
  - Verify app is installed (not just built)
  - Try uninstalling and reinstalling

#### 2. Text insertion fails
- **Problem**: ACTION_SET_TEXT not working in some apps
- **Solution**:
  - Check logs for fallback to clipboard
  - Some apps (WebViews) need special handling
  - Will be improved in Phase 2

#### 3. Overlay not showing
- **Problem**: Overlay permission not granted
- **Solution**:
  - Check Settings → Apps → Voice Mode → Display over other apps
  - Must be enabled

#### 4. Build errors
- **Problem**: Gradle sync failures
- **Solution**:
  - File → Invalidate Caches / Restart
  - Delete `.gradle` and `build` directories
  - Sync again

## Next Steps: Phase 2

Phase 2 will add:
- **Audio capture** using AudioRecord
- **Long-press gesture detection** (improved)
- **Cloud transcription** using Google Speech Recognition API
- **Test dictation flow** end-to-end
- **Improved overlay UI** with better visual feedback

See [PLAN.md](PLAN.md) for full Phase 2 details.

## Testing

Run unit tests:
```bash
./gradlew test
```

Run instrumented tests:
```bash
./gradlew connectedAndroidTest
```

## Code Style

- Follow [Kotlin style guide](https://kotlinlang.org/docs/coding-conventions.html)
- Use Timber for logging (not Log.d)
- Document public APIs with KDoc
- Prefer Kotlin coroutines over callbacks

## Resources

- [Android Accessibility Guide](https://developer.android.com/guide/topics/ui/accessibility/service)
- [Jetpack Compose Docs](https://developer.android.com/jetpack/compose)
- [Hilt Documentation](https://dagger.dev/hilt/)
- [Material 3 Guidelines](https://m3.material.io/)

---

**Phase 1 Status**: ✅ Complete and ready for testing!

**Current Version**: 1.0.0-alpha01
