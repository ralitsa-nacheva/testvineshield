package com.rncoding.testvineshield.core.presentation.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.error.*
import com.rncoding.testvineshield.core.domain.repository.SecuritySettingsRepository
import com.rncoding.testvineshield.core.domain.security.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SecuritySettingsViewModel(
    private val userId: Long,
    private val repository: SecuritySettingsRepository,
    private val localAuthenticator: LocalAuthenticator
) : ViewModel() {
    init { require(userId > 0) }
    private val _state = MutableStateFlow(SecuritySettingsUiState())
    val state = _state.asStateFlow()

    fun load() {
        if (_state.value.isLoading || _state.value.isUpdating) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                when (val saved = repository.getSettings(userId)) {
                    is Result.Error -> error(saved.error.userMessage)
                    is Result.Success -> {
                        _state.update { it.copy(settings = saved.data.normalized(), loaded = true) }
                        val biometric = localAuthenticator.isAvailable(LocalUnlockMethod.Biometric)
                        val device = localAuthenticator.isAvailable(LocalUnlockMethod.DeviceCredential)
                        currentCoroutineContext().ensureActive()
                        _state.update { it.copy(
                            biometricAvailable = biometric is Result.Success && biometric.data,
                            deviceCredentialAvailable = device is Result.Success && device.data
                        ) }
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error(UnexpectedError().userMessage) }
            finally { _state.update { it.copy(isLoading = false) } }
        }
    }

    fun setLockEnabled(enabled: Boolean) = change { it.copy(inactivityLockEnabled = enabled).normalized() }
    fun setTimeout(minutes: Int) {
        if (minutes !in SecuritySettings.timeoutMinutes) return
        change { it.copy(inactivityTimeoutMillis = minutes * 60_000L) }
    }
    fun setBiometricEnabled(enabled: Boolean) = setMethod(LocalUnlockMethod.Biometric, enabled)
    fun setDeviceCredentialEnabled(enabled: Boolean) = setMethod(LocalUnlockMethod.DeviceCredential, enabled)

    private fun setMethod(method: LocalUnlockMethod, enabled: Boolean) {
        if (!_state.value.canEdit || !_state.value.settings.inactivityLockEnabled) return
        change(method.takeIf { enabled }) {
            when (method) {
                LocalUnlockMethod.Biometric -> it.copy(biometricEnabled = enabled)
                LocalUnlockMethod.DeviceCredential -> it.copy(deviceCredentialEnabled = enabled)
            }
        }
    }

    private fun change(
        prove: LocalUnlockMethod? = null,
        transform: (SecuritySettings) -> SecuritySettings
    ) {
        if (!_state.value.canEdit) return
        val next = transform(_state.value.settings).normalized()
        if (next == _state.value.settings) return
        _state.update { it.copy(isUpdating = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                if (prove != null) {
                    when (val availability = localAuthenticator.isAvailable(prove)) {
                        is Result.Error -> { error(availability.error.userMessage); return@launch }
                        is Result.Success -> if (!availability.data) {
                            error("This method is unavailable. Use your account password or configure device security.")
                            return@launch
                        }
                    }
                    when (val auth = localAuthenticator.authenticate("Enable optional vineyard unlock", prove)) {
                        is Result.Error -> { error(auth.error.userMessage); return@launch }
                        is Result.Success -> Unit
                    }
                }
                currentCoroutineContext().ensureActive()
                // Save the whole policy in one upsert: disabling also clears both optional methods.
                when (val saved = repository.saveSettings(userId, next)) {
                    is Result.Error -> error(saved.error.userMessage)
                    is Result.Success -> _state.update { it.copy(settings = next) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { error(UnexpectedError().userMessage) }
            finally { _state.update { it.copy(isUpdating = false) } }
        }
    }
    private fun error(message: String) { _state.update { it.copy(errorMessage = message) } }
    fun clearError() { _state.update { it.copy(errorMessage = null) } }
}
