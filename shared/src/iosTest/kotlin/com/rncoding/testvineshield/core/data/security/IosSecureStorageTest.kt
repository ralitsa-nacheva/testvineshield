package com.rncoding.testvineshield.core.data.security

import kotlinx.coroutines.runBlocking
import platform.Foundation.NSUUID
import kotlin.test.*

class IosSecureStorageTest {
    @Test fun roundTripOverwriteEmptyUnicodeAndIdempotentDelete() = runBlocking {
        val storage = IosSecureStorage("testvineshield.tests.${NSUUID().UUIDString}")
        try {
            assertNull(storage.get("session"))
            storage.save("session", "лозе 🍇\u0000value")
            assertEquals("лозе 🍇\u0000value", storage.get("session"))
            storage.save("session", "")
            assertEquals("", storage.get("session"))
            storage.delete("session"); storage.delete("session")
            assertNull(storage.get("session"))
        } finally { storage.clear() }
    }

    @Test fun clearIsLimitedToItsService() = runBlocking {
        val first = IosSecureStorage("testvineshield.tests.${NSUUID().UUIDString}")
        val second = IosSecureStorage("testvineshield.tests.${NSUUID().UUIDString}")
        try {
            first.save("same", "first"); second.save("same", "second")
            first.clear()
            assertNull(first.get("same"))
            assertEquals("second", second.get("same"))
        } finally { first.clear(); second.clear() }
    }
}
