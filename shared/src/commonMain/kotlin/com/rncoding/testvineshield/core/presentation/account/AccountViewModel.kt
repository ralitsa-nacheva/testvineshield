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
                errorMessage = null
            )
        }
    }

    fun onCurrentPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                currentPassword = value,
                errorMessage = null
            )
        }
    }

    fun onNewPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                newPassword = value,
                errorMessage = null
            )
        }
    }

    fun onConfirmNewPasswordChanged(value: String) {
        _uiState.update {
            it.copy(
                confirmNewPassword = value,
                errorMessage = null
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
                    errorMessage = "New passwords do not match."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isUpdating = true,
                    errorMessage = null
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
                            errorMessage = null
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isUpdating = false,
                            errorMessage = result.error.userMessage
                        )
                    }
                }
            }
        }
    }

    fun logout() {
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
                    isLoggingOut = true,
                    errorMessage = null
                )
            }

            when (val result = authSessionCoordinator.logout()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoggingOut = false
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoggingOut = false,
                            errorMessage = result.error.userMessage
                        )
                    }
                }
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
                    errorMessage = null
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
                            errorMessage = result.error.userMessage
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(errorMessage = null)
        }
    }
}