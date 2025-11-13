package com.voicemode.accessibility

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import timber.log.Timber

/**
 * Manages the overlay UI that appears when recording.
 *
 * This shows:
 * - Recording indicator
 * - Visual feedback
 * - Cancel button
 */
class OverlayManager(private val context: Context) {

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null
    private var isShowing = false

    /**
     * Show the recording overlay
     */
    @SuppressLint("InflateParams")
    fun showRecordingOverlay() {
        if (isShowing) {
            Timber.w("Overlay already showing")
            return
        }

        try {
            // Create a simple overlay view programmatically
            // In Phase 2, we'll create a proper XML layout
            val overlay = createSimpleOverlayView()

            val params = createWindowLayoutParams()

            windowManager.addView(overlay, params)
            overlayView = overlay
            isShowing = true

            Timber.i("Recording overlay shown")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show overlay")
        }
    }

    /**
     * Hide the overlay
     */
    fun hideOverlay() {
        if (!isShowing) {
            return
        }

        try {
            overlayView?.let { view ->
                windowManager.removeView(view)
                overlayView = null
            }
            isShowing = false

            Timber.i("Overlay hidden")
        } catch (e: Exception) {
            Timber.e(e, "Failed to hide overlay")
        }
    }

    /**
     * Create the window layout parameters for the overlay
     */
    private fun createWindowLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 200 // 200 pixels from bottom
        }
    }

    /**
     * Create a simple overlay view programmatically
     * TODO: Replace with proper Compose UI in next iteration
     */
    @SuppressLint("SetTextI18n")
    private fun createSimpleOverlayView(): View {
        val overlay = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )

            // Set background
            setBackgroundColor(android.graphics.Color.parseColor("#CC000000"))
            setPadding(48, 32, 48, 32)

            // Add a simple text view for now
            val textView = android.widget.TextView(context).apply {
                text = "🎤 Recording..."
                textSize = 18f
                setTextColor(android.graphics.Color.WHITE)
            }

            addView(textView)
        }

        return overlay
    }

    /**
     * Update overlay to show processing state
     */
    fun showProcessing() {
        // TODO: Update overlay UI to show "Processing..." state
        Timber.d("Showing processing state")
    }

    /**
     * Update overlay to show error
     */
    fun showError(message: String) {
        // TODO: Update overlay UI to show error
        Timber.e("Showing error: $message")
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        hideOverlay()
    }
}
