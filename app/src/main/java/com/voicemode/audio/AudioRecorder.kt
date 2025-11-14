package com.voicemode.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AudioRecorder(private val context: Context) {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var isRecording = false

    companion object {
        private const val TAG = "AudioRecorder"
        private const val AUDIO_FORMAT = "WAV"
    }

    fun startRecording(): Boolean {
        return try {
            // Create output file
            outputFile = createAudioFile()

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(128000)
                setOutputFile(outputFile?.absolutePath)

                prepare()
                start()
            }

            isRecording = true
            Log.d(TAG, "Recording started: ${outputFile?.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            isRecording = false
            false
        }
    }

    fun stopRecording(): File? {
        return try {
            if (isRecording && mediaRecorder != null) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                Log.d(TAG, "Recording stopped")
                outputFile
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop recording", e)
            null
        }
    }

    fun cancelRecording() {
        try {
            if (isRecording && mediaRecorder != null) {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                outputFile?.delete()
                isRecording = false
                Log.d(TAG, "Recording cancelled")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel recording", e)
        }
    }

    fun isCurrentlyRecording(): Boolean = isRecording

    private fun createAudioFile(): File {
        val audioDir = File(context.cacheDir, "voice_recordings")
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(audioDir, "recording_$timestamp.m4a")
    }
}
