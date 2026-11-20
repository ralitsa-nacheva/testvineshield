package com.rncoding.testvineshield.core.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.auth.AuthSessionCoordinator
import com.rncoding.testvineshield.core.domain.auth.AuthState
import com.rncoding.testvineshield.core.domain.auth.ObserveAuthStateUseCase
import com.rncoding.testvineshield.core.domain.error.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import com.rncoding.testvineshield.core.domain.error.UnexpectedError

class AccountViewModel(
    private val authSessionCoordinator: AuthSessionCoordinator,
    observeAuthStateUseCase: ObserveAuthStateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    val authState: StateFlow<AuthState> =
        observeAuthStateUseCase()

    init {
        observeAuthenticatedUser()
    }

    private fun observeAuthenticatedUser() {
        viewModelScope.launch {
            authState.collect { state ->
                if (state is AuthState.Authenticated) {
                    _uiState.update {
                        it.copy(
                            email = state.user.userEmail,
                            newEmail = state.user.userEmail
                        )
                    }
                }
            }
        }
    }

    fun onNewEmailChanged(value: String) {
        _uiState.update {
            it.copy(
                newEmail = value,
                operationIssue = null
            )
        }
    }

    fun onCurrentPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                currentPassword = value,
                operationIssue = null
            )
        }
    }

    fun onNewPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                newPassword = value,
                operationIssue = null
            )
        }
    }

    fun onConfirmNewPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                confirmNewPassword = value,
                operationIssue = null
            )
        }
    }

    fun updateAccount() {
        val state = _uiState.value

        if (state.isUpdating ||
            state.isDeleting ||
            state.isLoggingOut
        ) {
            return
        }

        /*
         * If the user entered a new password, the confirmation
         * must match before calling the domain layer.
         */
        if (state.newPassword.isNotBlank() &&
            state.newPassword != state.confirmNewPassword
        ) {
            _uiState.update {
                it.copy(
                    operationIssue = AccountIssue.PasswordMismatch
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUpdating = true,
                    operationIssue = null
                )
            }

            val newPassword =
                state.newPassword.takeIf { it.isNotBlank() }

            when (
                val result = authSessionCoordinator.updateAccount(
                    currentPassword = state.currentPassword,
                    newEmail = state.newEmail,
                    newPassword = newPassword
                )
            ) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            email = result.data.userEmail,
                            newEmail = result.data.userEmail,
                            currentPassword = "",
                            newPassword = "",
                            confirmNewPassword = "",
                            isUpdating = false,
                            operationIssue = null
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            operationIssue = AccountIssue.Failure(result.error)
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        val state = _uiState.value
        if (state.isUpdating || state.isDeleting || state.isLoggingOut) return

        // Claim the action before launching: two taps in the same event loop
        // must not enqueue two session-clearing operations.
        _uiState.update {
            it.copy(isLoggingOut = true, operationIssue = null,
                currentPassword = "", newPassword = "", confirmNewPassword = "")
        }
        viewModelScope.launch {
            try {
                when (val result = authSessionCoordinator.logout()) {
                    is Result.Success -> _uiState.value = AccountUiState()
                    is Result.Error -> _uiState.update { it.copy(operationIssue = AccountIssue.Failure(result.error)) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _uiState.update { it.copy(operationIssue = AccountIssue.Failure(UnexpectedError())) }
            } finally {
                _uiState.update { it.copy(isLoggingOut = false) }
            }
        }
    }

    fun deleteAccount() {
        val state = _uiState.value

        if (state.isUpdating ||
            state.isDeleting ||
            state.isLoggingOut
        ) {
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDeleting = true,
                    operationIssue = null
                )
            }

            when (
                val result = authSessionCoordinator.deleteAccount(
                    currentPassword = state.currentPassword
                )
            ) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            currentPassword = ""
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            operationIssue = AccountIssue.Failure(result.error)
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(operationIssue = null)
        }
    }
}