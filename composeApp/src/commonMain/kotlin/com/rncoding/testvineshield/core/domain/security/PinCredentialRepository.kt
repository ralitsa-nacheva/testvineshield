package com.rncoding.testvineshield.core.domain.security

import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result

interface PinCredentialRepository {

    suspend fun hasPin(userId: Long): Result<Boolean, AppError>

    suspend fun setPin(
        userId: Long,
        pin: String
    ): Result<Unit, AppError>

    suspend fun verifyPin(
        userId: Long,
        pin: String
    ): Result<Boolean, AppError>

    suspend fun clearPin(
        userId: Long
    ): Result<Unit, AppError>
}