package com.rncoding.testvineshield.core.domain.security

interface PasswordHasher {

    fun generateSalt(): String

    fun hash(
        password: String,
        salt: String
    ): String

    fun verify(
        password: String,
        salt: String,
        hash: String
    ): Boolean
}