package com.rncoding.testvineshield.core.presentation.user_auth


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.auth.AuthSessionCoordinator
import com.rncoding.testvineshield.core.domain.auth.AuthState
import com.rncoding.testvineshield.core.domain.auth.ObserveAuthStateUseCase
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.usecases.user.LoginUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.RegisterUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.usecases.user.LogoutUserUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.RestoreSessionUseCase
import com.rncoding.testvineshield.core.domain.usecases.user.UnlockSessionUseCase
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authSessionCoordinator: AuthSessionCoordinator,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase
) : ViewModel() {

    // ----------------------------------------------------
    // Global authentication state
    // ----------------------------------------------------

    val authState: StateFlow<AuthState> =
        observeAuthStateUseCase()

    // ----------------------------------------------------
    // Authentication screen UI state
    // ----------------------------------------------------

    private val _uiState =
        MutableStateFlow(AuthUiState())

    val uiState: StateFlow<AuthUiState> =
        _uiState.asStateFlow()

    // ----------------------------------------------------
    // Initialization
    // ----------------------------------------------------

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
    // ----------------------------------------------------

    private fun restoreSession() {

        viewModelScope.launch {
            authSessionCoordinator.restoreSession()
        }
    }

    // ----------------------------------------------------
    // Login
    // ----------------------------------------------------

    fun login() {

        val current = _uiState.value

        if (current.isSubmitting) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                current.copy(
                    isSubmitting = true,
                    error = null,
                    emailError = null,
                    passwordError = null
                )

            when (
                val result =
                    authSessionCoordinator.login(
                        email = current.email,
                        password = current.password
                    )
            ) {

                is Result.Success -> {

                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false,
                            error = null,
                            password = ""
                        )
                }

                is Result.Error -> {
                    handleOperationError(result.error)
                }
            }
        }
    }

    // ----------------------------------------------------
    // Registration
    // ----------------------------------------------------

    fun register() {

        val current = _uiState.value

        if (current.isSubmitting) {
            return
        }

        viewModelScope.launch {

            _uiState.value =
                current.copy(
                    isSubmitting = true,
                    error = null,
                    emailError = null,
                    passwordError = null
                )

            when (
                val result =
                    authSessionCoordinator.register(
                        email = current.email,
                        password = current.password
                    )
            ) {

                is Result.Success -> {

                    /*
                     * Registration does not create a session.
                     *
                     * The user must subsequently log in.
                     */

                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false,
                            error = null,
                            password = ""
                        )
                }

                is Result.Error -> {
                    handleOperationError(result.error)
                }
            }
        }
    }

    // ----------------------------------------------------
    // Unlock
    // ----------------------------------------------------

    fun unlock() {

        val current = _uiState.value

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
                    authSessionCoordinator.unlock()
            ) {

                is Result.Success -> {

                    /*
                     * result.data is already AuthState.
                     *
                     * The coordinator has updated its
                     * StateFlow, so the ViewModel does not
                     * construct AuthState.Authenticated here.
                     */

                    _uiState.value =
                        _uiState.value.copy(
                            isSubmitting = false,
                            error = null
                        )
                }

                is Result.Error -> {
                    handleOperationError(result.error)
                }
            }
        }
    }

    // ----------------------------------------------------
    // Logout
    // ----------------------------------------------------

    fun logout() {

        val current = _uiState.value

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
                    authSessionCoordinator.logout()
            ) {

                is Result.Success -> {

                    _uiState.value =
                        AuthUiState()
                }

                is Result.Error -> {
                    handleOperationError(result.error)
                }
            }
        }
    }

    // ----------------------------------------------------
    // Operation error handling
    // ----------------------------------------------------

    private fun handleOperationError(
        error: AppError
    ) {

        _uiState.value =
            _uiState.value.copy(
                isSubmitting = false,
                error = error
            )
    }
}