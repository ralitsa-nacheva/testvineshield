package com.rncoding.testvineshield.core.domain.auth

import kotlinx.coroutines.flow.StateFlow

class ObserveAuthStateUseCase(
    private val authSessionCoordinator: AuthSessionCoordinator
) {

    operator fun invoke(): StateFlow<AuthState> {
        return authSessionCoordinator.authState
    }
}