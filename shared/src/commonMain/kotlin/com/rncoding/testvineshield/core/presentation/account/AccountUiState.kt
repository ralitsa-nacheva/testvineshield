package com.rncoding.testvineshield.core.presentation.account

data class AccountUiState(
    val email: String = "",
    val currentPassword: String = "",
    val newEmail: String = "",
    val newPassword: String = "",
    val confirmNewPassword: String = "",
    val isUpdating: Boolean = false,
    val isDeleting: Boolean = false,
    val isLoggingOut: Boolean = false,
    val errorMessage: String? = null
)