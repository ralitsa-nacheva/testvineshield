package com.rncoding.testvineshield.core.domain.datamodels

data class SessionDomainModel(
    val userId: Long,
    val createdAt: Long,
    val lastActiveAt: Long,
    val expiresAt: Long,
    val requiresUnlock: Boolean = false
)
