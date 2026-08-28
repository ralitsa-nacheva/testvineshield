package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.datamodels.SessionDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.repository.SessionRepository
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.security.SessionPolicy

class CreateSessionUseCase(
    private val sessionRepository: SessionRepository,
    private val clock: () -> Long
) {

    suspend operator fun invoke(
        userId: Long
    ): Result<Unit, AppError> {

        val now = clock()

        val session = SessionDomainModel(
            userId = userId,
            createdAt = now,
            lastActiveAt = now,
            expiresAt =
                now + SessionPolicy.MAX_SESSION_DURATION
        )

        return sessionRepository.saveSession(session) // should there be fun saveSession?
    }
}