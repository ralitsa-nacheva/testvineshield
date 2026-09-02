package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.auth.AuthState
import com.rncoding.testvineshield.core.domain.repository.SessionRepository
import com.rncoding.testvineshield.core.domain.repository.UserRepository
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.security.SessionManager


class RestoreSessionUseCase(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository
) {

    suspend operator fun invoke():
            Result<AuthState, AppError> {

        return when (
            val sessionResult =
                sessionManager.refreshSession()
        ) {

            is Result.Error ->
                Result.Error(
                    sessionResult.error
                )

            is Result.Success -> {

                val session =
                    sessionResult.data

                if (session == null) {

                    return Result.Success(
                        AuthState.Unauthenticated(
                            reason =
                                AuthState.Unauthenticated
                                    .Reason.NotLoggedIn
                        )
                    )
                }

                if (session.requiresUnlock) {

                    return Result.Success(
                        AuthState.Locked(
                            reason =
                                AuthState.Locked.LockReason
                                    .AppLaunchRequiresUnlock
                        )
                    )
                }

                when (
                    val userResult =
                        userRepository.getUserById(
                            session.userId
                        )
                ) {

                    is Result.Success ->
                        Result.Success(
                            AuthState.Authenticated(
                                user = userResult.data
                            )
                        )

                    is Result.Error ->
                        Result.Error(
                            userResult.error
                        )
                }
            }
        }
    }
}