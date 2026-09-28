package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.auth.AuthState
import com.rncoding.testvineshield.core.domain.auth.SessionState
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.SecuritySettingsRepository
import com.rncoding.testvineshield.core.domain.repository.UserRepository
import com.rncoding.testvineshield.core.domain.security.SecuritySettings
import com.rncoding.testvineshield.core.domain.security.SessionManager

/**
 * Restores the application authentication state from the
 * persisted local session.
 *
 * This use case does not perform login.
 *
 * It answers:
 *
 * "When the application starts, what authentication state
 * should the application enter?"
 */
class RestoreSessionUseCase(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val securitySettingsRepository: SecuritySettingsRepository
) {

    suspend operator fun invoke(): Result<AuthState, AppError> {

        return when (val stateResult = sessionManager.getSessionState()) {

            is Result.Error -> {
                Result.Error(stateResult.error)
            }

            is Result.Success -> {

                when (stateResult.data) {

                    SessionState.LoggedOut -> {
                        Result.Success(
                            AuthState.Unauthenticated(
                                reason =
                                    AuthState.Unauthenticated.Reason.NotLoggedIn
                            )
                        )
                    }

                    SessionState.Expired -> {
                        Result.Success(
                            AuthState.Unauthenticated(
                                reason =
                                    AuthState.Unauthenticated.Reason.SessionExpired
                            )
                        )
                    }

                    SessionState.TimedOut -> {
                        Result.Success(
                            AuthState.Locked(
                                reason =
                                    AuthState.Locked.LockReason.InactivityTimeout
                            )
                        )
                    }

                    SessionState.Active -> {
                        restoreActiveSession()
                    }
                }
            }
        }
    }

    private suspend fun restoreActiveSession():
            Result<AuthState, AppError> {

        return when (val sessionResult = sessionManager.getSession()) {

            is Result.Error -> {
                Result.Error(sessionResult.error)
            }

            is Result.Success -> {

                val session = sessionResult.data
                    ?: return Result.Success(
                        AuthState.Unauthenticated(
                            reason =
                                AuthState.Unauthenticated.Reason.NotLoggedIn
                        )
                    )

                when (
                    val userResult =
                        userRepository.getUserById(session.userId)
                ) {

                    is Result.Error -> {
                        Result.Error(userResult.error)
                    }

                    is Result.Success -> {

                        val user = userResult.data

                        if (user == null) {

                            clearInvalidSession()

                        } else {

                            restoreUser(user)
                        }
                    }
                }
            }
        }
    }

    private suspend fun clearInvalidSession():
            Result<AuthState, AppError> {

        return when (val clearResult = sessionManager.clearSession()) {

            is Result.Error -> {
                Result.Error(clearResult.error)
            }

            is Result.Success -> {
                Result.Success(
                    AuthState.Unauthenticated(
                        reason = AuthState.Unauthenticated.Reason.NotLoggedIn
                    )
                )
            }
        }
    }

    private suspend fun restoreUser(
        user: UserDomainModel
    ): Result<AuthState, AppError> {

        return when (
            val settingsResult =
                securitySettingsRepository.getSettings(user.userId)
        ) {

            is Result.Error -> {
                Result.Error(settingsResult.error)
            }

            is Result.Success -> {

                val settings = settingsResult.data

                if (requiresUnlock(settings)) {

                    Result.Success(
                        AuthState.Locked(
                            reason =
                                AuthState.Locked.LockReason.AppLaunchRequiresUnlock
                        )
                    )

                } else {

                    Result.Success(
                        AuthState.Authenticated(user)
                    )
                }
            }
        }
    }

    private fun requiresUnlock(
        settings: SecuritySettings
    ): Boolean {
        return settings.biometricEnabled ||
                settings.appPinEnabled
    }
}