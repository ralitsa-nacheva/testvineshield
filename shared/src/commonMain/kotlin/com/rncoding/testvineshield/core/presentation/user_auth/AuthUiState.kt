package com.rncoding.testvineshield.core.presentation.user_auth


data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val errorMessage: String? = null
)