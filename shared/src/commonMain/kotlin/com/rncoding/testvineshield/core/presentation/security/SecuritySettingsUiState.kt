package com.rncoding.testvineshield.core.presentation.security

import com.rncoding.testvineshield.core.domain.security.SecuritySettings

data class SecuritySettingsUiState(
    val settings: SecuritySettings = SecuritySettings(),
    val biometricAvailable: Boolean = false,
    val deviceCredentialAvailable: Boolean = false,
    val loaded: Boolean = false,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null
) { val canEdit: Boolean get() = loaded && !isLoading && !isUpdating }
