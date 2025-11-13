package com.voicemode.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import timber.log.Timber

/**
 * Main Accessibility Service for Voice Mode.
 *
 * This service:
 * - Monitors focused text fields across all apps
 * - Detects long-press gestures in text fields
 * - Inserts processed transcription text
 * - Provides system-wide dictation capability
 */
class VoiceModeAccessibilityService : AccessibilityService() {

    private var textFieldMonitor: TextFieldMonitor? = null
    private var gestureDetector: GestureDetectorModule? = null
    private var textInserter: TextInserter? = null
    private var overlayManager: OverlayManager? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Timber.d("VoiceMode Accessibility Service connected")

        // Initialize components
        textFieldMonitor = TextFieldMonitor()
        gestureDetector = GestureDetectorModule(this)
        textInserter = TextInserter(this)
        overlayManager = OverlayManager(this)

        Timber.i("All service components initialized successfully")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                handleViewFocused(event)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                handleTextChanged(event)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                handleWindowStateChanged(event)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleViewClicked(event)
            }
        }
    }

    private fun handleViewFocused(event: AccessibilityEvent) {
        val source = event.source ?: return

        if (source.isEditable) {
            textFieldMonitor?.onTextFieldFocused(source)
            Timber.d("Text field focused: ${source.className}")

            // Check if this is a password field - we should not activate dictation
            if (source.isPassword) {
                Timber.w("Password field detected - dictation disabled")
                return
            }

            // Check if this is a sensitive field
            if (isSensitiveField(source)) {
                Timber.w("Sensitive field detected - dictation disabled")
                return
            }
        }

        source.recycle()
    }

    private fun handleTextChanged(event: AccessibilityEvent) {
        // Monitor text changes for context awareness
        textFieldMonitor?.onTextChanged(event)
    }

    private fun handleWindowStateChanged(event: AccessibilityEvent) {
        // Track the current app/window for context
        Timber.d("Window state changed: ${event.packageName}")
    }

    private fun handleViewClicked(event: AccessibilityEvent) {
        val source = event.source ?: return

        if (source.isEditable) {
            // User clicked in a text field - could be activation trigger
            gestureDetector?.onTextFieldClicked(source)
        }

        source.recycle()
    }

    /**
     * Check if the text field is sensitive (banking apps, 2FA, etc.)
     */
    private fun isSensitiveField(node: AccessibilityNodeInfo): Boolean {
        // Check for common sensitive field hints
        val hints = listOf(
            "password",
            "pin",
            "cvv",
            "security code",
            "verification code",
            "2fa",
            "otp"
        )

        val hintText = node.hintText?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

        return hints.any { hint ->
            hintText.contains(hint) || text.contains(hint) || contentDesc.contains(hint)
        }
    }

    /**
     * Called when user activates dictation (e.g., long-press)
     */
    fun startDictation() {
        Timber.i("Starting dictation")

        val currentTextField = textFieldMonitor?.getCurrentTextField()
        if (currentTextField == null) {
            Timber.w("No text field focused, cannot start dictation")
            showToast("Please focus a text field first")
            return
        }

        // Show recording overlay
        overlayManager?.showRecordingOverlay()

        // Start audio recording (Phase 2)
        // TODO: Implement in Phase 2
        Timber.d("Audio recording would start here")
    }

    /**
     * Called when dictation is complete and we have processed text
     */
    fun insertText(text: String) {
        Timber.i("Inserting text: $text")

        val success = textInserter?.insertText(text) ?: false
        if (success) {
            Timber.d("Text inserted successfully")
            overlayManager?.hideOverlay()
        } else {
            Timber.e("Failed to insert text")
            showToast("Failed to insert text")
        }
    }

    private fun showToast(message: String) {
        // TODO: Show overlay message instead of toast for better UX
        Timber.d("Toast: $message")
    }

    override fun onInterrupt() {
        Timber.w("Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("Service destroyed")

        // Cleanup
        overlayManager?.cleanup()
        textFieldMonitor = null
        gestureDetector = null
        textInserter = null
        overlayManager = null
    }
}
