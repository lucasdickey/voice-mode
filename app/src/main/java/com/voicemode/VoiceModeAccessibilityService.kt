package com.voicemode

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import com.voicemode.aws.BedrockService
import com.voicemode.ui.theme.VoiceModeTheme
import com.voicemode.viewmodel.VoiceInputViewModel
import com.voicemode.viewmodel.VoiceInputState

class VoiceModeAccessibilityService : AccessibilityService(), ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private var fabView: ComposeView? = null

    private val store = ViewModelStore()
    private lateinit var lifecycleOwner: ServiceLifecycleOwner
    private lateinit var voiceInputViewModel: VoiceInputViewModel

    override val viewModelStore: ViewModelStore get() = store

    override fun onServiceConnected() {
        super.onServiceConnected()
        lifecycleOwner = ServiceLifecycleOwner()
        lifecycleOwner.create()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Initialize Bedrock service and ViewModel
        val bedrockService = BedrockService(
            apiEndpoint = getBedrockApiEndpoint(),
            apiKey = getBedrockApiKey()
        )
        voiceInputViewModel = VoiceInputViewModel(this, bedrockService)
    }

    private fun showFab() {
        if (fabView != null) return

        fabView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(this@VoiceModeAccessibilityService)

            setContent {
                VoiceModeTheme {
                    val uiState = voiceInputViewModel.uiState.collectAsState().value

                    // Handle state changes for transcription
                    LaunchedEffect(uiState) {
                        when (uiState) {
                            is VoiceInputState.Success -> {
                                Log.d("VoiceMode", "Transcribed: ${uiState.text}")
                                // TODO: Send transcribed text to active field
                            }
                            is VoiceInputState.Error -> {
                                Log.e("VoiceMode", "Error: ${uiState.message}")
                            }
                            else -> {}
                        }
                    }

                    FloatingActionButton(onClick = {
                        when (uiState) {
                            VoiceInputState.Idle -> voiceInputViewModel.startRecording()
                            is VoiceInputState.Success -> voiceInputViewModel.startRecording()
                            is VoiceInputState.Error -> voiceInputViewModel.startRecording()
                            VoiceInputState.Recording -> voiceInputViewModel.stopRecording()
                            VoiceInputState.Processing -> {} // Do nothing while processing
                        }
                    }) {
                        Text(when (uiState) {
                            VoiceInputState.Recording -> "⏹️"
                            VoiceInputState.Processing -> "⏳"
                            else -> "🎤"
                        })
                    }
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 16
            y = 16
        }

        windowManager.addView(fabView, params)
        if (!lifecycleOwner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
             lifecycleOwner.start()
        }
    }

    private fun hideFab() {
        fabView?.let {
            windowManager.removeView(it)
            fabView = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        when (event?.eventType) {
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                val source = event.source
                if (isEditable(source)) {
                    showFab()
                } else {
                    hideFab()
                }
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val currentFocus = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (!isEditable(currentFocus)) {
                    hideFab()
                }
            }
        }
    }

    private fun isEditable(node: AccessibilityNodeInfo?): Boolean {
        return node?.isEditable == true && node.isFocusable
    }

    override fun onInterrupt() {
        hideFab()
    }

    override fun onDestroy() {
        super.onDestroy()
        hideFab()
        lifecycleOwner.destroy()
    }

    private fun getBedrockApiEndpoint(): String {
        // TODO: Load from secure configuration
        // For now, this should be injected or loaded from a config file
        return "http://your-backend-api.com"
    }

    private fun getBedrockApiKey(): String {
        // TODO: Load from secure storage (e.g., EncryptedSharedPreferences)
        // For now, this should be injected or loaded from a config file
        return "your-api-key"
    }
}
