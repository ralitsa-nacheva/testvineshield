package com.rncoding.testvineshield.core.presentation.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.SecurityError
import com.rncoding.testvineshield.core.domain.error.UnexpectedError
import com.rncoding.testvineshield.core.domain.repository.SecuritySettingsRepository
import com.rncoding.testvineshield.core.domain.security.LocalAuthenticator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SecuritySettingsViewModel(
    private val userId: Long,
    private val repository: SecuritySettingsRepository,
    private val localAuthenticator: LocalAuthenticator
) : ViewModel() {
    init { require(userId > 0L) { "A valid user ID is required." } }

    private val _state = MutableStateFlow(SecuritySettingsUiState())
    val state = _state.asStateFlow()

    fun load() {
        if (_state.value.isLoading || _state.value.isUpdating) return
        // Set the guard before launching so rapid calls cannot start duplicate work.
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val settingsResult = repository.getSettings(userId)
                currentCoroutineContext().ensureActive()
                when (settingsResult) {
                    is Result.Error -> showError(settingsResult.error.userMessage)
                    is Result.Success -> {
                        // Keep the saved preferences even if the platform availability check throws.
                        _state.update {
                            it.copy(
                                biometricEnabled = settingsResult.data.biometricEnabled,
                                pinEnabled = settingsResult.data.appPinEnabled
                            )
                        }
                        val availability = localAuthenticator.isAvailable()
                        currentCoroutineContext().ensureActive()
                        val available = availability is Result.Success && availability.data
                        val error = when (availability) {
                            is Result.Error -> availability.error.userMessage
                            is Result.Success -> null
                        }
                        _state.update {
                            it.copy(
                                biometricEnabled = settingsResult.data.biometricEnabled,
                                pinEnabled = settingsResult.data.appPinEnabled,
                                biometricAvailable = available,
                                errorMessage = error
                            )
                        }
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                showError(UnexpectedError().userMessage)
            } finally {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        if (_state.value.isLoading || _state.value.isUpdating) return
        if (_state.value.biometricEnabled == enabled) return
        updateSettings {
            if (enabled) {
                when (val availability = localAuthenticator.isAvailable()) {
                    is Result.Error -> {
                        showError(availability.error.userMessage)
                        return@updateSettings
                    }
                    is Result.Success -> {
                        _state.update { it.copy(biometricAvailable = availability.data) }
                        if (!availability.data) {
                            showError(SecurityError.BiometricNotAvailable.userMessage)
                            return@updateSettings
                        }
                    }
                }
                currentCoroutineContext().ensureActive()
                when (val authentication = localAuthenticator.authenticate(
                    reason = "Enable biometric unlock for your vineyard account"
                )) {
                    is Result.Error -> {
                        showError(authentication.error.userMessage)
                        return@updateSettings
                    }
                    is Result.Success -> Unit
                }
            }
            // Never persist an enabled setting after a cancelled prompt/coroutine.
            currentCoroutineContext().ensureActive()
            when (val result = repository.setBiometricEnabled(userId, enabled)) {
                is Result.Error -> showError(result.error.userMessage)
                is Result.Success -> _state.update { it.copy(biometricEnabled = enabled) }
            }
        }
    }

    fun setPinEnabled(enabled: Boolean) {
        if (_state.value.isLoading || _state.value.isUpdating) return
        // There is no app-PIN setup/verification flow in this baseline.
        // Existing preferences may be disabled, but must not be newly enabled.
        if (enabled) {
            showError("App PIN setup is not available yet.")
            return
        }
        if (_state.value.pinEnabled == enabled) return
        updateSettings {
            when (val result = repository.setPinEnabled(userId, enabled)) {
                is Result.Error -> showError(result.error.userMessage)
                is Result.Success -> _state.update { it.copy(pinEnabled = enabled) }
            }
        }
    }

    private fun updateSettings(operation: suspend () -> Unit) {
        _state.update { it.copy(isUpdating = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                operation()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                currentCoroutineContext().ensureActive()
                showError(UnexpectedError().userMessage)
            } finally {
                _state.update { it.copy(isUpdating = false) }
            }
        }
    }

    private fun showError(message: String) {
        _state.update { it.copy(errorMessage = message) }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }
}
