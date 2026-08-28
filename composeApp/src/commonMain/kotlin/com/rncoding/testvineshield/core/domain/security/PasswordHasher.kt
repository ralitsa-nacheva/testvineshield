package com.rncoding.testvineshield.core.domain.security

interface PasswordHasher {
    fun hash(password: String, salt: String): String
    fun generateSalt(): String
    fun verify(password: String, salt: String, hash: String): Boolean
}