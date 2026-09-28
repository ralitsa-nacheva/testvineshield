package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.security.SessionManager



class LogoutUserUseCase(
    private val sessionManager: SessionManager
) {

    suspend operator fun invoke(): Result<Unit, AppError> {
        return sessionManager.clearSession()
    }
}