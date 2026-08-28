package com.rncoding.testvineshield.core.domain.security

interface SecureStorage {

    suspend fun get(key: String): String?

    suspend fun save(
        key: String,
        value: String
    )

    suspend fun delete(
        key: String
    )

    suspend fun clear()
}