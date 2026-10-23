package com.rncoding.testvineshield.core.domain.auth

import com.rncoding.testvineshield.core.domain.datamodels.UserDomainModel
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.usecases.user.LoginUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.LogoutUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.RegisterUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.RestoreSessionUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.UnlockSessionUseCase
import com.rncoding.testvineshield.core.domain.scheduler.WeatherHistoryBackgroundWorkCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.user.DeleteUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.UpdateUserUseCase
import com.rncoding.testvineshield.core.domain.scheduler.WeatherHistoryRiskScheduler

class AuthSessionCoordinator(

    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val unlockSessionUseCase: UnlockSessionUseCase,
    private val loginUserUseCase: LoginUserUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val logoutUserUseCase: LogoutUserUseCase,
    private val updateUserUseCase: UpdateUserUseCase,
    private val deleteUserUseCase: DeleteUserUseCase,
    private val backgroundWorkCoordinator: WeatherHistoryBackgroundWorkCoordinator,
    private val weatherHistoryRiskScheduler: WeatherHistoryRiskScheduler
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

                val state = result.data

                if (state is AuthState.Authenticated) {
                    scheduleBackgroundWork(state.user.userId)
                }
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

                scheduleBackgroundWork(result.data.userId)
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

                val state = result.data

                if (state is AuthState.Authenticated) {
                    scheduleBackgroundWork(state.user.userId)
                }
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

        // Capture the authenticated user before clearing the session.
        val userId = (
                _authState.value as? AuthState.Authenticated
                )?.user?.userId

        val result = logoutUserUseCase()

        when (result) {

            is Result.Success -> {

                _authState.value =
                    AuthState.Unauthenticated(
                        reason =
                            AuthState.Unauthenticated.Reason.LoggedOut
                    )

                // Cancellation is best-effort.
                // A successfully cleared session must remain logged out.
                if (userId != null) {
                    try {
                        weatherHistoryRiskScheduler.cancelAllForUser(
                            userId = userId
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Logout already succeeded.
                        // Diagnostic logging/recovery will be added later.
                    }
                }
            }

            is Result.Error -> {
                // The session could not be cleared.
                // Preserve the current authentication state.
                // Do not cancel scheduled work.
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

        val authenticatedState =
            currentState as? AuthState.Authenticated
                ?: return Result.Error(AuthError.NoActiveSession)

        // Capture the user ID before deleting the account.
        val userId = authenticatedState.user.userId

        val result = deleteUserUseCase(
            userId = userId,
            currentPassword = currentPassword
        )

        when (result) {

            is Result.Success -> {

                _authState.value =
                    AuthState.Unauthenticated(
                        reason =
                            AuthState.Unauthenticated.Reason.LoggedOut
                    )

                // Account deletion has already succeeded.
                // WorkManager cancellation must not change that result.
                try {
                    weatherHistoryRiskScheduler.cancelAllForUser(
                        userId = userId
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Background cancellation is best-effort.
                    // Add diagnostic logging/recovery later.
                }
            }

            is Result.Error -> {
                // Account deletion failed.
                // Preserve the current authenticated state.
                // Do not cancel scheduled work.
            }
        }

        return result
    }


    private suspend fun scheduleBackgroundWork(userId: Long) {
        try {
            backgroundWorkCoordinator.scheduleForUser(userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Background scheduling must not turn a successful
            // authentication operation into a login failure.
            //
            // We will introduce proper diagnostic logging
            // and recovery in a later checkpoint.
        }
    }
}