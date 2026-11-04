package com.rncoding.testvineshield.core.data.security

import com.rncoding.testvineshield.core.domain.security.SecureStorage
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.CoreFoundation.*
import platform.Security.*

/** Keychain generic-password items; preserves the existing service/account/UTF-8 format.
 * Every Create/Copy result is released, including failure paths. No secret is logged.
 */
@OptIn(ExperimentalForeignApi::class)
class IosSecureStorage(
    private val service: String = "com.rncoding.testvineshield.vineyard.secure-storage"
) : SecureStorage {
    private val lock = Mutex()

    override suspend fun get(key: String): String? = withContext(Dispatchers.Default) {
        lock.withLock {
            withQuery(key) { query ->
                CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
                CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
                memScoped {
                    val output = alloc<CFTypeRefVar>()
                    output.value = null
                    val status = SecItemCopyMatching(query, output.ptr)
                    try {
                        when (status) {
                            errSecItemNotFound -> null
                            errSecSuccess -> {
                                val raw = checkNotNull(output.value) { "Keychain returned no data." }
                                check(CFGetTypeID(raw) == CFDataGetTypeID()) { "Invalid Keychain data type." }
                                val data: CFDataRef = raw.reinterpret()
                                val count = CFDataGetLength(data)
                                check(count in 0..Int.MAX_VALUE.toLong()) { "Invalid Keychain data length." }
                                val bytes = CFDataGetBytePtr(data)
                                ByteArray(count.toInt()) { checkNotNull(bytes)[it].toByte() }
                                    .decodeToString(throwOnInvalidSequence = true)
                            }
                            else -> error("Keychain read failed: $status")
                        }
                    } finally { output.value?.let { CFRelease(it) } }
                }
            }
        }
    }

    override suspend fun save(key: String, value: String) = withContext(Dispatchers.Default) {
        lock.withLock {
            val bytes = value.encodeToByteArray()
            val data = bytes.usePinned {
                CFDataCreate(null, if (bytes.isEmpty()) null else it.addressOf(0).reinterpret(), bytes.size.convert())
            } ?: error("Unable to allocate Keychain data.")
            try {
                withQuery(key) { query ->
                    withDictionary { attributes ->
                        CFDictionarySetValue(attributes, kSecValueData, data)
                        var status = SecItemUpdate(query, attributes)
                        if (status == errSecItemNotFound) {
                            CFDictionarySetValue(query, kSecValueData, data)
                            status = SecItemAdd(query, null)
                            if (status == errSecDuplicateItem) {
                                // A different storage instance/process inserted between update and add.
                                CFDictionaryRemoveValue(query, kSecValueData)
                                status = SecItemUpdate(query, attributes)
                            }
                        }
                        check(status == errSecSuccess) { "Keychain save failed: $status" }
                    }
                }
            } finally { CFRelease(data) }
        }
    }

    override suspend fun delete(key: String) = withContext(Dispatchers.Default) {
        lock.withLock { withQuery(key) { checkDelete(SecItemDelete(it)) } }
    }

    override suspend fun clear() = withContext(Dispatchers.Default) {
        // Scope clear to this application's service; never delete unrelated Keychain entries.
        lock.withLock { withQuery(null) { checkDelete(SecItemDelete(it)) } }
    }

    private fun checkDelete(status: Int) {
        check(status == errSecSuccess || status == errSecItemNotFound) { "Keychain delete failed: $status" }
    }

    private inline fun <T> withQuery(key: String?, block: (CFMutableDictionaryRef) -> T): T =
        withDictionary { query ->
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            withString(service) { CFDictionarySetValue(query, kSecAttrService, it) }
            if (key != null) withString(key) { CFDictionarySetValue(query, kSecAttrAccount, it) }
            block(query)
        }

    private inline fun <T> withDictionary(block: (CFMutableDictionaryRef) -> T): T {
        val dictionary = CFDictionaryCreateMutable(
            null, 0, kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr
        ) ?: error("Unable to allocate Keychain query.")
        try { return block(dictionary) } finally { CFRelease(dictionary) }
    }

    private inline fun <T> withString(value: String, block: (CFStringRef) -> T): T {
        // CString conversion cannot represent embedded NULs; keys/service are identifiers.
        require('\u0000' !in value) { "Keychain identifiers cannot contain NUL." }
        val string = CFStringCreateWithCString(null, value, kCFStringEncodingUTF8)
            ?: error("Unable to allocate Keychain identifier.")
        try { return block(string) } finally { CFRelease(string) }
    }
}
