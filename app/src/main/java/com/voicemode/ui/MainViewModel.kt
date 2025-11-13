package com.voicemode.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class MainUiState(
    val hasMicrophonePermission: Boolean = false,
    val hasOverlayPermission: Boolean = false,
    val isAccessibilityEnabled: Boolean = false
) {
    val isFullySetup: Boolean
        get() = hasMicrophonePermission && hasOverlayPermission && isAccessibilityEnabled
}

@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        startPermissionMonitoring()
    }

    private fun startPermissionMonitoring() {
        viewModelScope.launch {
            while (true) {
                checkPermissions()
                delay(1000) // Check every second
            }
        }
    }

    private fun checkPermissions() {
        val context = getApplication<Application>()

        val hasMicrophone = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val hasOverlay = Settings.canDrawOverlays(context)

        val hasAccessibility = isAccessibilityServiceEnabled(context)

        _state.update { current ->
            current.copy(
                hasMicrophonePermission = hasMicrophone,
                hasOverlayPermission = hasOverlay,
                isAccessibilityEnabled = hasAccessibility
            )
        }

        Timber.d(
            "Permissions - Mic: $hasMicrophone, Overlay: $hasOverlay, Accessibility: $hasAccessibility"
        )
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val serviceName = "${context.packageName}/com.voicemode.accessibility.VoiceModeAccessibilityService"

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.contains(serviceName)
    }
}
