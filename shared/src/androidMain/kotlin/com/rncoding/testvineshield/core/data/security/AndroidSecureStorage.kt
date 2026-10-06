package com.rncoding.testvineshield.core.data.security

import android.content.Context
import android.util.Base64
import com.rncoding.testvineshield.core.domain.security.SecureStorage
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AndroidSecureStorage(
    context: Context
) : SecureStorage {

    private val applicationContext =
        context.applicationContext

    private val preferences =
        applicationContext.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

    private val keyStore =
        KeyStore.getInstance(
            ANDROID_KEYSTORE
        ).apply {
            load(null)
        }

    override suspend fun get(
        key: String
    ): String? {

        val storedValue =
            preferences.getString(
                key,
                null
            ) ?: return null

        val parts =
            storedValue.split(
                SEPARATOR
            )

        require(parts.size == 2) {
            "Invalid secure storage entry."
        }

        val iv =
            Base64.decode(
                parts[0],
                Base64.NO_WRAP
            )

        val ciphertext =
            Base64.decode(
                parts[1],
                Base64.NO_WRAP
            )

        val cipher =
            Cipher.getInstance(
                TRANSFORMATION
            )

        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(
                GCM_TAG_LENGTH_BITS,
                iv
            )
        )

        val plaintext =
            cipher.doFinal(ciphertext)

        return plaintext.toString(
            StandardCharsets.UTF_8
        )
    }

    override suspend fun save(
        key: String,
        value: String
    ) {

        val cipher =
            Cipher.getInstance(
                TRANSFORMATION
            )

        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateKey()
        )

        val iv =
            cipher.iv

        val ciphertext =
            cipher.doFinal(
                value.toByteArray(
                    StandardCharsets.UTF_8
                )
            )

        val storedValue =
            buildString {
                append(
                    Base64.encodeToString(
                        iv,
                        Base64.NO_WRAP
                    )
                )
                append(SEPARATOR)
                append(
                    Base64.encodeToString(
                        ciphertext,
                        Base64.NO_WRAP
                    )
                )
            }

        preferences.edit()
            .putString(
                key,
                storedValue
            )
            .apply()
    }

    override suspend fun delete(
        key: String
    ) {
        preferences.edit()
            .remove(key)
            .apply()
    }

    override suspend fun clear() {
        preferences.edit()
            .clear()
            .apply()
    }

    private fun getOrCreateKey(): SecretKey {

        val existingKey =
            keyStore.getKey(
                KEY_ALIAS,
                null
            ) as? SecretKey

        if (existingKey != null) {
            return existingKey
        }

        val keyGenerator =
            KeyGenerator.getInstance(
                KEY_ALGORITHM,
                ANDROID_KEYSTORE
            )

        keyGenerator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                        android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(
                    android.security.keystore.KeyProperties.BLOCK_MODE_GCM
                )
                .setEncryptionPaddings(
                    android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setKeySize(KEY_SIZE_BITS)
                .build()
        )

        return keyGenerator.generateKey()
    }

    private companion object {

        const val PREFS_NAME =
            "vineyard_secure_storage"

        const val KEY_ALIAS =
            "vineyard_secure_storage_key"

        const val ANDROID_KEYSTORE =
            "AndroidKeyStore"

        const val KEY_ALGORITHM =
            "AES"

        const val TRANSFORMATION =
            "AES/GCM/NoPadding"

        const val KEY_SIZE_BITS =
            256

        const val GCM_TAG_LENGTH_BITS =
            128

        const val SEPARATOR =
            ":"
    }
}