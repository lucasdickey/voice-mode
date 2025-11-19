package com.voicemode.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voicemode.audio.AudioRecorder
import com.voicemode.aws.BedrockService
import com.voicemode.speech.MLKitSpeechRecognizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VoiceInputViewModel(
    private val context: Context,
    private val bedrockService: BedrockService
) : ViewModel() {
    private val audioRecorder = AudioRecorder(context)
    private val mlKitRecognizer = MLKitSpeechRecognizer(context)

    private val _uiState = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    val uiState: StateFlow<VoiceInputState> = _uiState.asStateFlow()

    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText.asStateFlow()

    companion object {
        private const val TAG = "VoiceInputViewModel"
    }

    fun startRecording() {
        if (audioRecorder.startRecording()) {
            _uiState.value = VoiceInputState.Recording
            Log.d(TAG, "Recording started")
        } else {
            _uiState.value = VoiceInputState.Error("Failed to start recording")
        }
    }

    fun stopRecording() {
        val audioFile = audioRecorder.stopRecording()
        if (audioFile != null) {
            _uiState.value = VoiceInputState.Processing
            transcribeAudio(audioFile)
        } else {
            _uiState.value = VoiceInputState.Error("Failed to stop recording")
        }
    }

    fun cancelRecording() {
        audioRecorder.cancelRecording()
        _uiState.value = VoiceInputState.Idle
    }

    private fun transcribeAudio(audioFile: java.io.File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Try cloud-based transcription first
                val result = bedrockService.transcribeAudio(audioFile)
                when (result) {
                    is BedrockService.TranscriptionResult.Success -> {
                        Log.d(TAG, "Cloud transcription successful: ${result.text}")
                        // Try to enhance the transcription with LLM
                        enhanceTranscription(result.text)
                    }
                    is BedrockService.TranscriptionResult.Error -> {
                        Log.w(TAG, "Cloud transcription failed, falling back to ML Kit: ${result.message}")
                        // Fallback to ML Kit speech recognition
                        fallbackToMLKit()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cloud transcription exception, falling back to ML Kit", e)
                fallbackToMLKit()
            }
        }
    }

    private fun enhanceTranscription(rawText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "Enhancing transcription with LLM")
                val enhancementResult = bedrockService.enhanceText(rawText)
                when (enhancementResult) {
                    is BedrockService.EnhancementResult.Success -> {
                        _transcribedText.value = enhancementResult.text
                        _uiState.value = VoiceInputState.Success(enhancementResult.text)
                        Log.d(TAG, "LLM enhancement successful: ${enhancementResult.text}")
                    }
                    is BedrockService.EnhancementResult.Error -> {
                        Log.w(TAG, "LLM enhancement failed, using raw transcription: ${enhancementResult.message}")
                        // Fall back to raw transcription if enhancement fails
                        _transcribedText.value = rawText
                        _uiState.value = VoiceInputState.Success(rawText)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "LLM enhancement exception, using raw transcription", e)
                // Fall back to raw transcription if enhancement fails
                _transcribedText.value = rawText
                _uiState.value = VoiceInputState.Success(rawText)
            }
        }
    }

    private fun fallbackToMLKit() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting ML Kit speech recognition fallback")
                _uiState.value = VoiceInputState.Processing

                // Use ML Kit for on-device speech recognition
                val result = mlKitRecognizer.startListening()

                _transcribedText.value = result
                _uiState.value = VoiceInputState.Success(result)
                Log.d(TAG, "ML Kit transcription successful: $result")
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Speech recognition failed"
                _uiState.value = VoiceInputState.Error(errorMsg)
                Log.e(TAG, "ML Kit transcription error", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.cancelRecording()
        mlKitRecognizer.shutdown()
    }
}

sealed class VoiceInputState {
    object Idle : VoiceInputState()
    object Recording : VoiceInputState()
    object Processing : VoiceInputState()
    data class Success(val text: String) : VoiceInputState()
    data class Error(val message: String) : VoiceInputState()
}
