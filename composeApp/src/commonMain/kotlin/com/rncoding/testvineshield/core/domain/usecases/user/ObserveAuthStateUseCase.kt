package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.error.AuthState
import com.rncoding.testvineshield.core.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

class ObserveAuthStateUseCase(
    private val sessionRepository: SessionRepository
) {
   suspend operator fun invoke(): Flow<AuthState> {
        return sessionRepository.observeAuthState()
    }
}