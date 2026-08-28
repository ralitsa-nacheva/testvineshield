package com.rncoding.testvineshield.core.data.session

import platform.Security.*
actual class SecureStorage {
    actual fun save(key: String, value: String) {
        KeychainHelper.save(key, value)
    }
    actual fun get(key: String): String? {
        return KeychainHelper.get(key)
    }
    actual fun clear(key: String) {
        KeychainHelper.delete(key)
    }
}
