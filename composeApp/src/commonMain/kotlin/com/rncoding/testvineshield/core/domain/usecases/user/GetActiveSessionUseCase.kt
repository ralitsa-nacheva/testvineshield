package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.datamodels.SessionDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.repository.SessionRepository
import com.rncoding.testvineshield.core.domain.error.Result

class GetActiveSessionUseCase(
    private val sessionRepo: SessionRepository
) {
    suspend operator fun invoke(): Result<SessionDomainModel?, AppError> {
        return sessionRepo.getSession()
    }
}