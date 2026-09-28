package com.rncoding.testvineshield.core.domain.security

import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result

interface LocalAuthenticator {
    suspend fun isAvailable():
            Result<Boolean, AppError>

    suspend fun authenticate(
        reason: String
    ): Result<Unit, AppError>
}