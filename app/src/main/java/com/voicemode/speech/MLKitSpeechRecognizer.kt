package com.voicemode.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Fallback speech recognizer using Android's built-in SpeechRecognizer
 * Used when cloud-based transcription (AWS Bedrock) is unavailable
 */
class MLKitSpeechRecognizer(private val context: Context) {
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

    companion object {
        private const val TAG = "MLKitSpeechRecognizer"
    }

    /**
     * Start listening for speech input
     * Returns transcribed text when speech ends
     */
    suspend fun startListening(): String = suspendCancellableCoroutine { continuation ->
        try {
            val recognitionListener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Ready for speech")
                }

                override fun onBeginningOfSpeech() {
                    Log.d(TAG, "Speech began")
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Handle RMS changes (volume level)
                }

                override fun onBufferReceived(buffer: ByteArray?) {
                    // Handle buffer updates
                }

                override fun onEndOfSpeech() {
                    Log.d(TAG, "Speech ended")
                }

                override fun onError(error: Int) {
                    Log.e(TAG, "Recognition error: $error")
                    continuation.resumeWithException(
                        Exception("Speech recognition failed with error code: $error")
                    )
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val result = if (!matches.isNullOrEmpty()) matches[0] else "No speech detected"
                    Log.d(TAG, "Recognition results: $result")
                    continuation.resume(result)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    Log.d(TAG, "Partial result: $partial")
                }

                override fun onEvent(eventType: Int, params: Bundle?) {
                    Log.d(TAG, "Event: $eventType")
                }
            }

            // Create intent for speech recognition
            val intent = Intent("android.speech.action.RECOGNIZE_SPEECH").apply {
                putExtra("android.speech.extra.LANGUAGE_MODEL", "free_form")
                putExtra("android.speech.extra.LANGUAGE", "en-US")
            }

            // Start listening
            speechRecognizer.setRecognitionListener(recognitionListener)
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition", e)
            continuation.resumeWithException(e)
        }
    }

    /**
     * Stop listening and return transcribed text
     */
    fun stopListening() {
        try {
            speechRecognizer.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition", e)
        }
    }

    /**
     * Cancel the current listening session
     */
    fun cancel() {
        try {
            speechRecognizer.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling speech recognition", e)
        }
    }

    /**
     * Destroy the recognizer instance
     */
    fun shutdown() {
        try {
            speechRecognizer.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognizer", e)
        }
    }
}
