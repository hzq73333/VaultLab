package com.vaultlab.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Keeps user secrets (API keys) encrypted on the device.
 * The key material lives in the Android Keystore, so it cannot be read off the file.
 */
class SecureStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "vaultlab_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun getVirusTotalKey(): String? = prefs.getString(KEY_VIRUSTOTAL, null)

    fun setVirusTotalKey(key: String?) {
        prefs.edit().apply {
            if (key.isNullOrBlank()) remove(KEY_VIRUSTOTAL) else putString(KEY_VIRUSTOTAL, key.trim())
        }.apply()
    }

    private companion object {
        const val KEY_VIRUSTOTAL = "virustotal_api_key"
    }
}
