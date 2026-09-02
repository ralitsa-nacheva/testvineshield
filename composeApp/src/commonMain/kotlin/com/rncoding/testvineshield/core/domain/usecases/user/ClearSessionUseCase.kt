package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.repository.SessionRepository
import com.rncoding.testvineshield.core.domain.error.Result

class ClearSessionUseCase(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(): Result<Unit, AppError> {
        return sessionRepository.clearSession()
    }
}
