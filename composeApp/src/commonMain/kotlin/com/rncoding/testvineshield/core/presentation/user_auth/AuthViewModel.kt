package com.rncoding.testvineshield.core.presentation.user_auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.error.AuthError
import com.rncoding.testvineshield.core.domain.error.AuthState
import com.rncoding.testvineshield.core.domain.repository.SecuritySettingsRepository
import com.rncoding.testvineshield.core.domain.repository.SessionRepository
import com.rncoding.testvineshield.core.domain.repository.UserRepository
import com.rncoding.testvineshield.core.domain.security.BiometricAuthenticator
import com.rncoding.testvineshield.core.domain.security.PinCredentialRepository
import com.rncoding.testvineshield.core.domain.usecases.user.LoginUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.RegisterUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.user.CreateSessionUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.LogoutUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.RestoreSessionUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.UnlockSessionUseCase
import kotlinx.coroutines.launch

class AuthViewModel(
    private val loginUseCase: LoginUserUseCase,
    private val registerUseCase: RegisterUserUseCase,
    private val restoreSessionUseCase: RestoreSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val unlockSessionUseCase: UnlockSessionUseCase
) : ViewModel() {

    private val _authState =
        MutableStateFlow<AuthState>(
            AuthState.Loading
        )

    val authState: StateFlow<AuthState> =
        _authState.asStateFlow()

    private val _uiState =
        MutableStateFlow(
            AuthUiState()
        )

    val uiState: StateFlow<AuthUiState> =
        _uiState.asStateFlow()

    init {
        restoreSession()
    }

    // ----------------------------------------------------
    // Form input
    // ----------------------------------------------------

    fun onEmailChanged(value: String) {

        _uiState.value =
            _uiState.value.copy(
                email = value,
                emailError = null,
                error = null
            )
    }

    fun onPasswordChanged(value: String) {

        _uiState.value =
            _uiState.value.copy(
                password = value,
                passwordError = null,
                error = null
            )
    }

    // ----------------------------------------------------
    // Restore existing session
    // --------------------------------------------------------------------------------------------------------

    private fun restoreSession() {

        viewModelScope.launch {

            _authState.value =
                AuthState.Loading

            when (
                val result =
                    restoreSessionUseCase()
            ) {

                is Result.Success -> {
                    _authState.value =
                        result.data
                }

                is Result.Error -> {
                    _authState.value =
                        AuthState.Error(
                            result.error
                        )
                }
            }
        }
    }
    // ----------------------------------------------------
    // Login
    // ----------------------------------------------------

    fun login() {

        val current =
            _uiState.value

        if (current.isSubmitting) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                current.copy(
                    isSubmitting = true,
                    error = null
                )

            when (
                val result =
                    loginUseCase(
                        email = current.email,
                        password = current.password
                    )
            ) {

                is Result.Success -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false
                        )

                    _authState.value =
                        AuthState.Authenticated(
                            user = result.data
                        )
                }

                is Result.Error -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false,
                            error = result.error
                        )

                    /*
                     * Login failure does not necessarily mean
                     * that the global authentication state has
                     * failed.
                     *
                     * If the user was already authenticated,
                     * don't destroy that state because of a
                     * failed login form attempt.
                     */
                    if (
                        _authState.value
                                is AuthState.Unauthenticated
                    ) {
                        _authState.value =
                            AuthState.Error(
                                error = result.error
                            )
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // Registration
    // ----------------------------------------------------

    fun register() {

        val current =
            _uiState.value

        if (current.isSubmitting) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                current.copy(
                    isSubmitting = true,
                    error = null
                )

            when (
                val result =
                    registerUseCase(
                        email = current.email,
                        password = current.password
                    )
            ) {

                is Result.Success -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false
                        )

                    _authState.value =
                        AuthState.Authenticated(
                            user = result.data
                        )
                }

                is Result.Error -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false,
                            error = result.error
                        )
                }
            }
        }
    }

    // ----------------------------------------------------
    // Logout
    // ----------------------------------------------------

    fun logout() {

        viewModelScope.launch {

            when (
                val result =
                    logoutUseCase()
            ) {

                is Result.Success -> {

                    _authState.value =
                        AuthState.Unauthenticated(
                            reason =
                                AuthState.Unauthenticated
                                    .Reason.LoggedOut
                        )
                }

                is Result.Error -> {

                    _authState.value =
                        AuthState.Error(
                            error = result.error
                        )
                }
            }
        }
    }

    // ----------------------------------------------------
    // Lock
    // ----------------------------------------------------

    fun lockForInactivity() {

        if (
            _authState.value
                    is AuthState.Authenticated
        ) {
            _authState.value =
                AuthState.Locked(
                    reason =
                        AuthState.Locked.LockReason
                            .InactivityTimeout
                )
        }
    }

    fun lockOnAppLaunch() {

        if (
            _authState.value
                    is AuthState.Authenticated
        ) {
            _authState.value =
                AuthState.Locked(
                    reason =
                        AuthState.Locked.LockReason
                            .AppLaunchRequiresUnlock
                )
        }
    }

    // ----------------------------------------------------
    // Unlock
    // ----------------------------------------------------

    fun unlock() {

        viewModelScope.launch {

            when (
                val result =
                    unlockSessionUseCase()
            ) {

                is Result.Success -> {

                    _authState.value =
                        AuthState.Authenticated(
                            user = result.data
                        )
                }

                is Result.Error -> {

                    _authState.value =
                        AuthState.Error(
                            error = result.error
                        )
                }
            }
        }
    }
}