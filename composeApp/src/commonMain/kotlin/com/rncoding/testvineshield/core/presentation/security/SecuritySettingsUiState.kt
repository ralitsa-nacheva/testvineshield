package com.rncoding.testvineshield.core.presentation.security

import com.rncoding.testvineshield.core.domain.error.AppError

data class SecuritySettingsUiState(
    val biometricEnabled: Boolean = false,
    val pinEnabled: Boolean = false,
    val biometricAvailable: Boolean = false,
    val isLoading: Boolean = false,
    val error: AppError? = null
)
