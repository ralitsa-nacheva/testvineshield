package com.rncoding.testvineshield.core.data.session


import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
actual class SecureStorage(private val context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        "secure_session",
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    actual fun save(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    actual fun get(key: String): String? = prefs.getString(key, null)
    actual fun clear(key: String) {
        prefs.edit().remove(key).apply()
    }
}
