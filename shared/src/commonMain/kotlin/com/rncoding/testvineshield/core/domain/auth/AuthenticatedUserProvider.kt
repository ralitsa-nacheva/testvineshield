package com.rncoding.testvineshield.core.domain.auth

import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

class AuthenticatedUserProvider(
    private val authSessionCoordinator: AuthSessionCoordinator
) {

    fun currentUserOrNull(): UserDomainModel? {
        return (authSessionCoordinator.authState.value as? AuthState.Authenticated)
            ?.user
    }

    fun observeUser(): Flow<UserDomainModel?> {
        return authSessionCoordinator.authState
            .map { state ->
                (state as? AuthState.Authenticated)?.user
            }
            .distinctUntilChanged()
    }
}