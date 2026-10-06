package com.rncoding.testvineshield.core.data.security

import com.rncoding.testvineshield.core.domain.security.PasswordHashParameters
import com.rncoding.testvineshield.core.domain.security.PasswordHasher
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
class AndroidPasswordHasher : PasswordHasher {

    private val secureRandom =
        SecureRandom()

    override fun generateSalt(): String {

        val salt =
            ByteArray(
                PasswordHashParameters.SALT_BYTES
            )

        secureRandom.nextBytes(salt)

        return Base64.Default.encode(salt)
    }

    override fun hash(
        password: String,
        salt: String
    ): String {

        val saltBytes =
            Base64.Default.decode(salt)

        require(
            saltBytes.size ==
                    PasswordHashParameters.SALT_BYTES
        ) {
            "Invalid password salt."
        }

        val spec =
            PBEKeySpec(
                password.toCharArray(),
                saltBytes,
                PasswordHashParameters.ITERATIONS,
                PasswordHashParameters.HASH_BYTES * 8
            )

        return try {

            val factory =
                SecretKeyFactory.getInstance(
                    "PBKDF2WithHmacSHA256"
                )

            val derivedKey =
                factory
                    .generateSecret(spec)
                    .encoded

            require(
                derivedKey.size ==
                        PasswordHashParameters.HASH_BYTES
            ) {
                "Unexpected derived key length."
            }

            Base64.Default.encode(
                derivedKey
            )

        } finally {
            spec.clearPassword()
        }
    }

    override fun verify(
        password: String,
        salt: String,
        hash: String
    ): Boolean {

        return try {

            val expectedHash =
                Base64.Default.decode(hash)

            if (
                expectedHash.size !=
                PasswordHashParameters.HASH_BYTES
            ) {
                return false
            }

            val saltBytes =
                Base64.Default.decode(salt)

            if (
                saltBytes.size !=
                PasswordHashParameters.SALT_BYTES
            ) {
                return false
            }

            val spec =
                PBEKeySpec(
                    password.toCharArray(),
                    saltBytes,
                    PasswordHashParameters.ITERATIONS,
                    PasswordHashParameters.HASH_BYTES * 8
                )

            try {

                val factory =
                    SecretKeyFactory.getInstance(
                        "PBKDF2WithHmacSHA256"
                    )

                val actualHash =
                    factory
                        .generateSecret(spec)
                        .encoded

                constantTimeEquals(
                    actualHash,
                    expectedHash
                )

            } finally {
                spec.clearPassword()
            }

        } catch (
            _: IllegalArgumentException
        ) {
            false

        } catch (
            _: RuntimeException
        ) {
            false
        }
    }

    private fun constantTimeEquals(
        first: ByteArray,
        second: ByteArray
    ): Boolean {

        if (first.size != second.size) {
            return false
        }

        var difference = 0

        for (index in first.indices) {
            difference =
                difference or
                        (
                                first[index].toInt()
                                        xor
                                        second[index].toInt()
                                )
        }

        return difference == 0
    }
}