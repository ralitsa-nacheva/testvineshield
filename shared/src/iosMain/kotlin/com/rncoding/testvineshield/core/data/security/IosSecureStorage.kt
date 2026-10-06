package com.rncoding.testvineshield.core.data.security

import com.rncoding.testvineshield.core.data.security.toCFDictionary
import com.rncoding.testvineshield.core.domain.security.SecureStorage
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.SecItemUpdate
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFBooleanFalse
import platform.CoreFoundation.CFDictionaryRef

@OptIn(ExperimentalForeignApi::class)
class IosSecureStorage : SecureStorage {

    override suspend fun get(
        key: String
    ): String? {

        val query =
            baseQuery(key).toMutableMap().apply {
                this[kSecReturnData] =
                    kCFBooleanTrue
                this[kSecMatchLimit] =
                    kSecMatchLimitOne
            }

        val result =
            CFTypeRefVar()

        val status =
            SecItemCopyMatching(
                query.toCFDictionary(),
                result.ptr
            )

        return when (status) {

            0 -> {
                val data =
                    result.value as? NSData
                        ?: error(
                            "Keychain returned invalid data."
                        )

                data.toByteArray()
                    .decodeToString()
            }

            errSecItemNotFound ->
                null

            else ->
                error(
                    "Keychain read failed. " +
                            "status=$status"
                )
        }
    }

    override suspend fun save(
        key: String,
        value: String
    ) {

        val data =
            value
                .encodeToByteArray()
                .toNSData()

        val attributes =
            baseQuery(key).toMutableMap().apply {
                this[kSecValueData] = data
            }

        val addStatus =
            SecItemAdd(
                attributes.toCFDictionary(),
                null
            )

        if (
            addStatus == errSecDuplicateItem
        ) {

            val updateAttributes =
                mapOf(
                    kSecValueData to data
                )

            val updateStatus =
                SecItemUpdate(
                    baseQuery(key)
                        .toCFDictionary(),
                    updateAttributes
                        .toCFDictionary()
                )

            check(updateStatus == 0) {
                "Keychain update failed. " +
                        "status=$updateStatus"
            }

        } else {

            check(addStatus == 0) {
                "Keychain insert failed. " +
                        "status=$addStatus"
            }
        }
    }

    override suspend fun delete(
        key: String
    ) {

        val status =
            SecItemDelete(
                baseQuery(key)
                    .toCFDictionary()
            )

        check(
            status == 0 ||
                    status == errSecItemNotFound
        ) {
            "Keychain delete failed. " +
                    "status=$status"
        }
    }

    override suspend fun clear() {

        val query =
            mapOf(
                kSecClass to
                        kSecClassGenericPassword,

                kSecAttrService to
                        SERVICE
            )

        val status =
            SecItemDelete(
                query.toCFDictionary()
            )

        check(
            status == 0 ||
                    status == errSecItemNotFound
        ) {
            "Keychain clear failed. " +
                    "status=$status"
        }
    }

    private fun baseQuery(
        key: String
    ): Map<Any?, Any?> =
        mapOf(
            kSecClass to
                    kSecClassGenericPassword,

            kSecAttrService to
                    SERVICE,

            kSecAttrAccount to
                    key
        )

    private companion object {

        const val SERVICE =
            "com.rncoding.testvineshield.vineyard.secure-storage"
    }
}