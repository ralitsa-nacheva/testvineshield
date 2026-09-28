package com.rncoding.testvineshield.core.presentation.user_auth

sealed interface AuthUiEvent {

    data object RegistrationSuccess : AuthUiEvent
}