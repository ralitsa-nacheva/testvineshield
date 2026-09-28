package com.rncoding.testvineshield.core.domain.security

import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result

interface BiometricAuthenticator {

    fun isAvailable(): Boolean

    suspend fun authenticate(): Result<Unit, AppError>
}