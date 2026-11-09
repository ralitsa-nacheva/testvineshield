package com.rncoding.testvineshield.core.domain.security

import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result

enum class LocalUnlockMethod { Biometric, DeviceCredential }

interface LocalAuthenticator {
    suspend fun isAvailable(method: LocalUnlockMethod): Result<Boolean, AppError> =
        if (method == LocalUnlockMethod.Biometric) isAvailable() else Result.Success(false)

    suspend fun authenticate(reason: String, method: LocalUnlockMethod): Result<Unit, AppError> =
        if (method == LocalUnlockMethod.Biometric) authenticate(reason)
        else Result.Error(com.rncoding.testvineshield.core.domain.error.SecurityError.DeviceCredentialNotAvailable)

    suspend fun isAvailable():
            Result<Boolean, AppError>

    suspend fun authenticate(
        reason: String
    ): Result<Unit, AppError>
}