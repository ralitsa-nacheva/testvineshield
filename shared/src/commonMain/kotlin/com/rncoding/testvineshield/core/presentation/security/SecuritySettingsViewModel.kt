package com.rncoding.testvineshield.core.presentation.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.repository.SecuritySettingsRepository
import com.rncoding.testvineshield.core.domain.security.BiometricAuthenticator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.rncoding.testvineshield.core.domain.error.Result

class SecuritySettingsViewModel(
    private val userId: Long,
    private val repository: SecuritySettingsRepository,
    private val biometricAuthenticator: BiometricAuthenticator
) : ViewModel() {

    private val _state =
        MutableStateFlow(
            SecuritySettingsUiState()
        )

    val state =
        _state.asStateFlow()

    fun load() {
        viewModelScope.launch {

            _state.update {
                it.copy(isLoading = true)
            }

            val biometricAvailable =
                biometricAuthenticator.isAvailable()

            when (
                val result =
                    repository.getSettings(userId)
            ) {

                is Result.Success -> {

                    _state.value =
                        SecuritySettingsUiState(
                            biometricEnabled =
                                result.data.biometricEnabled,
                            pinEnabled =
                                result.data.appPinEnabled,
                            biometricAvailable =
                                biometricAvailable
                        )
                }

                is Result.Error -> {

                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = result.error
                        )
                    }
                }
            }
        }
    }

    fun setBiometricEnabled(
        enabled: Boolean
    ) {
        viewModelScope.launch {

            if (enabled) {

                if (
                    !biometricAuthenticator
                        .isAvailable()
                ) {
                    return@launch
                }

                when (
                    val authentication =
                        biometricAuthenticator
                            .authenticate()
                ) {

                    is Result.Error -> {
                        _state.update {
                            it.copy(
                                error =
                                    authentication.error
                            )
                        }

                        return@launch
                    }

                    is Result.Success -> Unit
                }
            }

            when (
                val result =
                    repository.setBiometricEnabled(
                        userId = userId,
                        enabled = enabled
                    )
            ) {

                is Result.Success -> {
                    _state.update {
                        it.copy(
                            biometricEnabled = enabled,
                            error = null
                        )
                    }
                }

                is Result.Error -> {
                    _state.update {
                        it.copy(
                            error = result.error
                        )
                    }
                }
            }
        }
    }

    fun setPinEnabled(
        enabled: Boolean
    ) {
        viewModelScope.launch {

            when (
                val result =
                    repository.setPinEnabled(
                        userId = userId,
                        enabled = enabled
                    )
            ) {

                is Result.Success -> {
                    _state.update {
                        it.copy(
                            pinEnabled = enabled,
                            error = null
                        )
                    }
                }

                is Result.Error -> {
                    _state.update {
                        it.copy(
                            error = result.error
                        )
                    }
                }
            }
        }
    }
}