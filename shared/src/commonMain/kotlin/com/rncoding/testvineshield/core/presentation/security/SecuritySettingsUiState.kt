package com.rncoding.testvineshield.core.presentation.security

data class SecuritySettingsUiState(
    val biometricEnabled: Boolean = false,
    val pinEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null
)