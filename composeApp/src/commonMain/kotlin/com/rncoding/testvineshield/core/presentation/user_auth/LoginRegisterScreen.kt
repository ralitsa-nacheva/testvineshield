package com.rncoding.testvineshield.core.presentation.user_auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.rncoding.testvineshield.core.domain.auth.AuthState

@Composable
fun LoginRegisterScreen(
    viewModel: AuthViewModel,
    reason: AuthState.Unauthenticated.Reason
) {

    var showRegister by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {

        viewModel.events.collect { event ->

            when (event) {

                AuthUiEvent.RegistrationSuccess -> {
                    showRegister = false
                }
            }
        }
    }

    if (showRegister) {

        RegisterScreen(
            viewModel = viewModel,
            onNavigateToLogin = {
                showRegister = false
            }
        )

    } else {

        LoginScreen(
            viewModel = viewModel,
            reason = reason,
            onNavigateToRegister = {
                showRegister = true
            }
        )
    }
}