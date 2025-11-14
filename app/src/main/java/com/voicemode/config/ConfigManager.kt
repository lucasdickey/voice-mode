package com.voicemode.config

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages secure configuration for the app
 */
class ConfigManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "voice_mode_config",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_API_ENDPOINT = "bedrock_api_endpoint"
        private const val KEY_API_KEY = "bedrock_api_key"
    }

    fun setApiEndpoint(endpoint: String) {
        sharedPreferences.edit().putString(KEY_API_ENDPOINT, endpoint).apply()
    }

    fun getApiEndpoint(): String {
        return sharedPreferences.getString(KEY_API_ENDPOINT, "") ?: ""
    }

    fun setApiKey(key: String) {
        sharedPreferences.edit().putString(KEY_API_KEY, key).apply()
    }

    fun getApiKey(): String {
        return sharedPreferences.getString(KEY_API_KEY, "") ?: ""
    }

    fun hasApiCredentials(): Boolean {
        return getApiEndpoint().isNotEmpty() && getApiKey().isNotEmpty()
    }

    fun clearCredentials() {
        sharedPreferences.edit().clear().apply()
    }
}
