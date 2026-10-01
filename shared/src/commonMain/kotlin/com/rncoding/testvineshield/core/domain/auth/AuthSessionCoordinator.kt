package com.rncoding.testvineshield.core.domain.auth

import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.usecases.user.LoginUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.LogoutUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.RegisterUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.RestoreSessionUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.UnlockSessionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.user.DeleteUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.UpdateUserUseCase

class AuthSessionCoordinator(

    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val unlockSessionUseCase: UnlockSessionUseCase,
    private val loginUserUseCase: LoginUserUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val logoutUserUseCase: LogoutUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
) {

    private val _authState =
        MutableStateFlow<AuthState>(
            AuthState.Loading
        )

    val authState: StateFlow<AuthState> =
        _authState.asStateFlow()

    suspend fun restoreSession() {
        _authState.value = AuthState.Loading

        when (val result = restoreSessionUseCase()) {

            is Result.Success -> {
                _authState.value = result.data
            }

            is Result.Error -> {
                _authState.value =
                    AuthState.Error(result.error)
            }
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<UserDomainModel, AppError> {

        val result =
            loginUserUseCase(
                email = email,
                password = password
            )

        when (result) {

            is Result.Success -> {
                _authState.value =
                    AuthState.Authenticated(result.data)
            }

            is Result.Error -> {
                /*
                 * Login errors are returned to the caller.
                 *
                 * We deliberately do NOT change the global
                 * AuthState to Error here.
                 *
                 * The user is still unauthenticated.
                 */
            }
        }

        return result
    }

    suspend fun register(
        email: String,
        password: String
    ): Result<UserDomainModel, AppError> {

        return registerUserUseCase(
            email = email,
            password = password
        )
    }

    suspend fun unlock(): Result<AuthState, AppError> {

        val result =
            unlockSessionUseCase()

        when (result) {

            is Result.Success -> {
                _authState.value = result.data
            }

            is Result.Error -> {
                /*
                 * Unlock failure does not mean that the
                 * authentication state itself is broken.
                 *
                 * The user remains locked.
                 *
                 * The caller receives the error and can
                 * display it in the UI.
                 */
            }
        }

        return result
    }

    suspend fun logout(): Result<Unit, AppError> {

        val result =
            logoutUserUseCase()

        when (result) {

            is Result.Success -> {
                _authState.value =
                    AuthState.Unauthenticated(
                        reason =
                            AuthState.Unauthenticated.Reason.LoggedOut
                    )
            }

            is Result.Error -> {
                /*
                 * The session could not be cleared.
                 *
                 * Keep the current authentication state
                 * unchanged and return the error to the caller.
                 */
            }
        }

        return result
    }

    suspend fun updateAccount(
        currentPassword: String,
        newEmail: String,
        newPassword: String?
    ): Result<UserDomainModel, AppError> {

        val currentState = _authState.value

        val authenticatedState = currentState as? AuthState.Authenticated
            ?: return Result.Error(AuthError.NoActiveSession)

        val result = updateUserUseCase(
            userId = authenticatedState.user.userId,
            currentPassword = currentPassword,
            newEmail = newEmail,
            newPassword = newPassword
        )

        when (result) {
            is Result.Success -> {
                _authState.value = AuthState.Authenticated(result.data)
            }

            is Result.Error -> {
                // Keep the current authenticated state.
            }
        }

        return result
    }

    suspend fun deleteAccount(
        currentPassword: String
    ): Result<Unit, AppError> {

        val currentState = _authState.value

        val authenticatedState = currentState as? AuthState.Authenticated
            ?: return Result.Error(AuthError.NoActiveSession)

        val result = deleteUserUseCase(
            userId = authenticatedState.user.userId,
            currentPassword = currentPassword
        )

        when (result) {
            is Result.Success -> {
                _authState.value = AuthState.Unauthenticated(
                    reason = AuthState.Unauthenticated.Reason.LoggedOut
                )
            }

            is Result.Error -> {
                // Keep the current authenticated state.
            }
        }

        return result
    }
}