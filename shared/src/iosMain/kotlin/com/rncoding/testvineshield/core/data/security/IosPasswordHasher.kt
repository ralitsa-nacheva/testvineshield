package com.rncoding.testvineshield.core.data.security

import com.rncoding.testvineshield.core.domain.security.PasswordHashParameters
import com.rncoding.testvineshield.core.domain.security.PasswordHasher
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CommonCrypto.CCKeyDerivationPBKDF
import platform.CommonCrypto.kCCPBKDF2
import platform.CommonCrypto.kCCPRFHmacAlgSHA256
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(
    ExperimentalForeignApi::class,
    ExperimentalEncodingApi::class
)
class IosPasswordHasher : PasswordHasher {

    override fun generateSalt(): String {
        val salt =
            ByteArray(
                PasswordHashParameters.SALT_BYTES
            )

        salt.usePinned { pinned ->

            val status =
                SecRandomCopyBytes(
                    kSecRandomDefault,
                    salt.size.toULong(),
                    pinned.addressOf(0)
                )

            check(status == 0) {
                "Unable to generate cryptographically secure salt."
            }
        }

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

        val passwordBytes =
            password.encodeToByteArray()

        val derivedKey =
            ByteArray(
                PasswordHashParameters.HASH_BYTES
            )

        passwordBytes.usePinned { passwordPinned ->

            saltBytes.usePinned { saltPinned ->

                derivedKey.usePinned { keyPinned ->

                    val status =
                        CCKeyDerivationPBKDF(
                            kCCPBKDF2.convert(),

                            if (passwordBytes.isEmpty()) null else passwordPinned.addressOf(0),
                            passwordBytes.size.toULong(),

                            saltPinned.addressOf(0).reinterpret(),
                            saltBytes.size.toULong(),

                            kCCPRFHmacAlgSHA256.convert(),

                            PasswordHashParameters
                                .ITERATIONS
                                .toUInt(),

                            keyPinned.addressOf(0).reinterpret(),
                            derivedKey.size.toULong()
                        )

                    check(status == 0) {
                        "PBKDF2 password hashing failed. " +
                                "Status=$status"
                    }
                }
            }
        }

        return Base64.Default.encode(
            derivedKey
        )
    }

    override fun verify(
        password: String,
        salt: String,
        hash: String
    ): Boolean {

        return try {

            val calculatedHash =
                hash(
                    password = password,
                    salt = salt
                )

            constantTimeEquals(
                calculatedHash,
                hash
            )

        } catch (
            _: IllegalArgumentException
        ) {
            false

        } catch (
            _: IllegalStateException
        ) {
            false
        }
    }

    private fun constantTimeEquals(
        first: String,
        second: String
    ): Boolean {

        val firstBytes =
            first.encodeToByteArray()

        val secondBytes =
            second.encodeToByteArray()

        if (firstBytes.size != secondBytes.size) {
            return false
        }

        var difference = 0

        for (index in firstBytes.indices) {
            difference =
                difference or
                        (
                                firstBytes[index].toInt()
                                        xor
                                        secondBytes[index].toInt()
                                )
        }

        return difference == 0
    }
}