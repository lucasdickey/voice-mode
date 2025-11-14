package com.voicemode.aws

import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Base64

class BedrockService(
    private val apiEndpoint: String,
    private val apiKey: String
) {
    private val client = OkHttpClient()
    private val gson = Gson()

    companion object {
        private const val TAG = "BedrockService"
    }

    /**
     * Send audio file to backend API which will call Bedrock Whisper
     */
    suspend fun transcribeAudio(audioFile: File): TranscriptionResult {
        return try {
            // Read the audio file and encode to base64
            val audioBytes = audioFile.readBytes()
            val encodedAudio = Base64.getEncoder().encodeToString(audioBytes)

            // Create request payload
            val payload = TranscriptionRequest(
                audio = encodedAudio,
                filename = audioFile.name
            )

            val requestBody = gson.toJson(payload)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$apiEndpoint/transcribe")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val result = gson.fromJson(responseBody, TranscriptionResponse::class.java)
                TranscriptionResult.Success(result.transcription, result.confidence)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e(TAG, "Transcription failed: $errorBody")
                TranscriptionResult.Error("HTTP ${response.code}: $errorBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Transcription error", e)
            TranscriptionResult.Error(e.message ?: "Unknown error")
        }
    }

    data class TranscriptionRequest(
        val audio: String,
        val filename: String
    )

    data class TranscriptionResponse(
        val transcription: String,
        val confidence: Float
    )

    sealed class TranscriptionResult {
        data class Success(val text: String, val confidence: Float) : TranscriptionResult()
        data class Error(val message: String) : TranscriptionResult()
    }
}
