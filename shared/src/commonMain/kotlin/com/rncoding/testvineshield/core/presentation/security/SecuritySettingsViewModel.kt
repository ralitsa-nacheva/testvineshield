package com.rncoding.testvineshield.core.presentation.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.repository.SecuritySettingsRepository
import com.rncoding.testvineshield.core.domain.security.SecuritySettings
import com.rncoding.testvineshield.core.domain.security.BiometricAuthenticator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SecuritySettingsViewModel(
    private val userId: Long,
    private val repository: SecuritySettingsRepository,
    private val biometricAuthenticator: BiometricAuthenticator
) : ViewModel() {

    private val _state =
        MutableStateFlow(
            SecuritySettingsUiState()
        )

    val state: StateFlow<SecuritySettingsUiState> =
        _state.asStateFlow()

    fun load() {
        if (_state.value.isLoading) {
            return
        }

        viewModelScope.launch {

            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null
                )
            }

            val biometricAvailable =
                biometricAuthenticator.isAvailable()

            when (
                val result =
                    repository.getSettings(userId)
            ) {

                is Result.Success<*> -> {

                    val settings =
                        result.data as SecuritySettings

                    _state.value =
                        SecuritySettingsUiState(
                            biometricEnabled =
                                settings.biometricEnabled,

                            pinEnabled =
                                settings.appPinEnabled,

                            biometricAvailable =
                                biometricAvailable,

                            isLoading = false,

                            isUpdating = false,

                            errorMessage = null
                        )
                }

                is Result.Error<*> -> {

                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage =
                                result.error.userMessage
                        )
                    }
                }

                else -> {

                    _state.update {
                        it.copy(
                            isLoading = false,
                            errorMessage =
                                "Unable to load security settings."
                        )
                    }
                }
            }
        }
    }

    fun setBiometricEnabled(
        enabled: Boolean
    ) {

        if (_state.value.isUpdating) {
            return
        }

        viewModelScope.launch {

            _state.update {
                it.copy(
                    isUpdating = true,
                    errorMessage = null
                )
            }

            /*
             * Enabling biometric authentication requires
             * the device to support biometric authentication.
             */
            if (enabled) {

                val biometricAvailable =
                    biometricAuthenticator.isAvailable()

                if (!biometricAvailable) {

                    _state.update {
                        it.copy(
                            isUpdating = false,
                            errorMessage =
                                "Biometric authentication is not available on this device."
                        )
                    }

                    return@launch
                }

                /*
                 * Require successful biometric authentication
                 * before enabling the setting.
                 */
                when (
                    val authentication =
                        biometricAuthenticator.authenticate()
                ) {

                    is Result.Success<*> -> {
                        // Continue with saving the setting.
                    }

                    is Result.Error<*> -> {

                        _state.update {
                            it.copy(
                                isUpdating = false,
                                errorMessage =
                                    authentication.error.userMessage
                            )
                        }

                        return@launch
                    }

                    else -> {

                        _state.update {
                            it.copy(
                                isUpdating = false,
                                errorMessage =
                                    "Biometric authentication could not be completed."
                            )
                        }

                        return@launch
                    }
                }
            }

            /*
             * Persist the new biometric setting.
             */
            when (
                val result =
                    repository.setBiometricEnabled(
                        userId = userId,
                        enabled = enabled
                    )
            ) {

                is Result.Success<*> -> {

                    _state.update {
                        it.copy(
                            biometricEnabled = enabled,
                            isUpdating = false,
                            errorMessage = null
                        )
                    }
                }

                is Result.Error<*> -> {

                    _state.update {
                        it.copy(
                            isUpdating = false,
                            errorMessage =
                                result.error.userMessage
                        )
                    }
                }

                else -> {

                    _state.update {
                        it.copy(
                            isUpdating = false,
                            errorMessage =
                                "Unable to update biometric settings."
                        )
                    }
                }
            }
        }
    }

    fun setPinEnabled(
        enabled: Boolean
    ) {

        if (_state.value.isUpdating) {
            return
        }

        viewModelScope.launch {

            _state.update {
                it.copy(
                    isUpdating = true,
                    errorMessage = null
                )
            }

            when (
                val result =
                    repository.setPinEnabled(
                        userId = userId,
                        enabled = enabled
                    )
            ) {

                is Result.Success<*> -> {

                    _state.update {
                        it.copy(
                            pinEnabled = enabled,
                            isUpdating = false,
                            errorMessage = null
                        )
                    }
                }

                is Result.Error<*> -> {

                    _state.update {
                        it.copy(
                            isUpdating = false,
                            errorMessage =
                                result.error.userMessage
                        )
                    }
                }

                else -> {

                    _state.update {
                        it.copy(
                            isUpdating = false,
                            errorMessage =
                                "Unable to update PIN settings."
                        )
                    }
                }
            }
        }
    }

    fun clearError() {

        _state.update {
            it.copy(
                errorMessage = null
            )
        }
    }
}