package com.rncoding.testvineshield.core.domain.usecases.user

import com.rncoding.testvineshield.core.domain.auth.AuthState
import com.rncoding.testvineshield.core.domain.auth.SessionState
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.repository.*
import com.rncoding.testvineshield.core.domain.security.*

sealed interface UnlockRequest {
    data class Password(val value: String) : UnlockRequest {
        override fun toString(): String = "Password([redacted])"
    }
    data class Platform(val method: LocalUnlockMethod) : UnlockRequest
}

/** Verify the existing account, then refresh activity; never create a new session here. */
class UnlockSessionUseCase(
    private val sessionManager: SessionManager,
    private val userRepository: UserRepository,
    private val localAuthenticator: LocalAuthenticator,
    private val securitySettingsRepository: SecuritySettingsRepository
) {
    suspend operator fun invoke(request: UnlockRequest): Result<AuthState, AppError> {
        val session = when (val stored = sessionManager.getSession()) {
            is Result.Error -> return Result.Error(stored.error)
            is Result.Success -> stored.data ?: return Result.Error(AuthError.NoActiveSession)
        }
        when (val state = sessionManager.getSessionState()) {
            is Result.Error -> return Result.Error(state.error)
            is Result.Success -> when (state.data) {
                SessionState.Expired -> return Result.Error(AuthError.SessionExpired)
                SessionState.LoggedOut -> return Result.Error(AuthError.NoActiveSession)
                else -> Unit
            }
        }
        val user = when (val loaded = userRepository.getUserById(session.userId)) {
            is Result.Error -> return Result.Error(loaded.error)
            is Result.Success -> loaded.data ?: return Result.Error(AuthError.UserNotFound)
        }
        val authentication = when (request) {
            is UnlockRequest.Password -> {
                if (request.value.isEmpty()) return Result.Error(AuthError.InvalidCredentials)
                // Repository login verifies the saved hash; this does not create/extend a session.
                when (val verified = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { userRepository.login(user.userEmail, request.value) }) {
                    is Result.Error -> Result.Error(verified.error)
                    is Result.Success -> if (verified.data.userId == session.userId) Result.Success(Unit)
                        else Result.Error(AuthError.InvalidCredentials)
                }
            }
            is UnlockRequest.Platform -> {
                val settings = when (val saved = securitySettingsRepository.getSettings(session.userId)) {
                    is Result.Error -> return Result.Error(saved.error)
                    is Result.Success -> saved.data.normalized()
                }
                val enabled = when (request.method) {
                    LocalUnlockMethod.Biometric -> settings.biometricEnabled
                    LocalUnlockMethod.DeviceCredential -> settings.deviceCredentialEnabled
                }
                if (!enabled) return Result.Error(SecurityError.UserFallback)
                when (val available = localAuthenticator.isAvailable(request.method)) {
                    is Result.Error -> return Result.Error(available.error)
                    is Result.Success -> if (!available.data) return Result.Error(SecurityError.UserFallback)
                }
                localAuthenticator.authenticate("Unlock your vineyard account", request.method)
            }
        }
        if (authentication is Result.Error) return Result.Error(authentication.error)
        // Reject a completion after logout/account change instead of refreshing another user's session.
        when (val current = sessionManager.getSession()) {
            is Result.Error -> return Result.Error(current.error)
            is Result.Success -> if (current.data != session) return Result.Error(AuthError.NoActiveSession)
        }
        return when (val refreshed = sessionManager.refreshSession(expectedSession = session)) {
            is Result.Error -> Result.Error(refreshed.error)
            is Result.Success -> Result.Success(AuthState.Authenticated(user))
        }
    }
}
