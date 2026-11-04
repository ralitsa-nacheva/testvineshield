package com.rncoding.testvineshield.core.data.security

import kotlin.test.*

class IosPasswordHasherTest {
    private val hasher = IosPasswordHasher()

    @Test fun matchesIndependentPbkdf2Sha256UnicodeVector() {
        // Python hashlib.pbkdf2_hmac, 600000 iterations, 32-byte output.
        assertEquals("1wUxBHtTb/a0+KnpWKvLgCPjP23Q+3RfpfO7SfpeJ/A=", hasher.hash("лозе🍇", "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="))
    }

    @Test fun rejectsWrongPasswordAndMalformedSalt() {
        val salt = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8="
        val hash = "1wUxBHtTb/a0+KnpWKvLgCPjP23Q+3RfpfO7SfpeJ/A="
        assertFalse(hasher.verify("wrong", salt, hash))
        assertFalse(hasher.verify("лозе🍇", "invalid", hash))
    }

    @Test fun saltsAreRandomAndHaveExpectedEncodedLength() {
        val first = hasher.generateSalt()
        assertEquals(44, first.length)
        assertNotEquals(first, hasher.generateSalt())
    }
}
