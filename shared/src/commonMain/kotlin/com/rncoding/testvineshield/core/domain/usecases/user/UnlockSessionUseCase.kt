package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.auth.AuthState
import com.rncoding.testvineshield.core.domain.auth.SessionState
import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.UserRepository
import com.rncoding.testvineshield.core.domain.security.LocalAuthenticator
import com.rncoding.testvineshield.core.domain.security.SessionManager

/**
 * Unlocks an existing authenticated session using the
 * platform's local authentication mechanism.
 *
 * Examples:
 *
 * Android:
 * - BiometricPrompt
 * - device credential
 *
 * iOS:
 * - LocalAuthentication / LAContext
 */
class UnlockSessionUseCase(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val localAuthenticator: LocalAuthenticator
) {

    suspend operator fun invoke(): Result<AuthState, AppError> {

        // 1. Get the persisted session.
        val session = when (val result = sessionManager.getSession()) {
            is Result.Error -> return Result.Error(result.error)

            is Result.Success -> {
                result.data
                    ?: return Result.Error(AuthError.NoActiveSession)
            }
        }

        // 2. Authenticate the user locally.
        when (
            val authenticationResult =
                localAuthenticator.authenticate(
                    reason = "Unlock your vineyard account"
                )
        ) {
            is Result.Error -> {
                return Result.Error(authenticationResult.error)
            }

            is Result.Success -> {
                // Authentication succeeded.
            }
        }

        // 3. Make sure the session has not expired.
        val sessionState = when (val result = sessionManager.getSessionState()) {
            is Result.Error -> return Result.Error(result.error)
            is Result.Success -> result.data
        }

        when (sessionState) {
            SessionState.Expired -> {
                return Result.Error(AuthError.SessionExpired)
            }

            SessionState.LoggedOut -> {
                return Result.Error(AuthError.NoActiveSession)
            }

            SessionState.TimedOut,
            SessionState.Active -> {
                // Continue.
            }
        }

        // 4. Refresh the session's lastActiveAt.
        val refreshedSession = when (val result = sessionManager.refreshSession()) {
            is Result.Error -> return Result.Error(result.error)
            is Result.Success -> result.data
        }

        // 5. Load the user associated with the session.
        val user = when (
            val result = userRepository.getUserById(refreshedSession.userId)
        ) {
            is Result.Error -> return Result.Error(result.error)

            is Result.Success -> {
                result.data
                    ?: return Result.Error(AuthError.UserNotFound)
            }
        }

        // 6. Authentication succeeded and the session is valid.
        return Result.Success(
            AuthState.Authenticated(user)
        )
    }
}