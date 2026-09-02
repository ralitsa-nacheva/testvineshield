package com.rncoding.testvineshield.core.presentation

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.auth.AuthState

@Composable
fun AppRoot(viewModel: AppViewModel) {
    when (val state = viewModel.authState) {
        is AuthState.Loading -> {
            LoadingScreen()
        }
        is AuthState.Unauthenticated -> {
            AuthScreen()
        }
        is AuthState.Authenticated -> {
            DashboardScreen(userId = state.userId)
        }
        is AuthState.Error -> {
            ErrorScreen(state.message)
        }
    }
}