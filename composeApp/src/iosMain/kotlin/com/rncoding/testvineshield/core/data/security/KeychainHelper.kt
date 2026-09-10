package com.rncoding.testvineshield.core.data.security

import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.*
import platform.darwin.OSStatus
import kotlinx.cinterop.*
object KeychainHelper {
    private const val SERVICE = "com.yourapp.secure"
    // 🔐 Save or update
    fun save(key: String, value: String): Result<Unit> = runCatching {
        val data = value.encodeToByteArray().toNSData()
        val query = mutableMapOf<Any?, Any>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE,
            kSecAttrAccount to key
        )
        val attributes = mapOf<Any?, Any>(
            kSecValueData to data,
            kSecAttrAccessible to kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        )
        val status = SecItemAdd(
            (query + attributes).toCFDictionary(),
            null
        )
        when (status) {
            errSecSuccess -> Unit
            errSecDuplicateItem -> {
                val updateStatus = SecItemUpdate(
                    query.toCFDictionary(),
                    attributes.toCFDictionary()
                )
                if (updateStatus != errSecSuccess) {
                    throw KeychainException(updateStatus)
                }
            }
            else -> throw KeychainException(status)
        }
    }
    // 🔍 Get value
    fun get(key: String): Result<String?> = runCatching {
        val query = mapOf<Any?, Any>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE,
            kSecAttrAccount to key,
            kSecReturnData to kCFBooleanTrue!!,
            kSecMatchLimit to kSecMatchLimitOne
        )
        memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(
                query.toCFDictionary(),
                result.ptr
            )
            when (status) {
                errSecSuccess -> {
                    val data = result.value as NSData
                    data.toByteArray().decodeToString()
                }
                errSecItemNotFound -> null
                else -> throw KeychainException(status)
            }
        }
    }
    // ❌ Delete
    fun delete(key: String): Result<Unit> = runCatching {
        val query = mapOf<Any?, Any>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE,
            kSecAttrAccount to key
        )
        val status = SecItemDelete(query.toCFDictionary())
        if (status != errSecSuccess && status != errSecItemNotFound) {
            throw KeychainException(status)
        }
    }
    // 🔥 Clear ALL keys (optional)
    fun clearAll(): Result<Unit> = runCatching {
        val query = mapOf<Any?, Any>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to SERVICE
        )
        val status = SecItemDelete(query.toCFDictionary())
        if (status != errSecSuccess && status != errSecItemNotFound) {
            throw KeychainException(status)
        }
    }
}
