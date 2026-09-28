package com.rncoding.testvineshield.core.domain.security

object PasswordHashParameters {

    const val ALGORITHM = "PBKDF2-HMAC-SHA256"

    const val ITERATIONS = 600_000

    const val SALT_BYTES = 32

    const val HASH_BYTES = 32
}