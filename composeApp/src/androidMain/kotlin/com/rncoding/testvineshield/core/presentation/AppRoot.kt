package com.rncoding.testvineshield.core.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.rncoding.testvineshield.core.domain.error.AuthState
import com.rncoding.testvineshield.core.presentation.user_auth.AuthViewModel

@Composable
fun AppRoot(
    authViewModel: AuthViewModel
) {
    val authState by
    authViewModel.authState
        .collectAsState()

    when (val state = authState) {

        AuthState.Idle -> {
            android.window.SplashScreen()
        }

        AuthState.Loading -> {
            LoadingScreen()
        }

        is AuthState.Unauthenticated -> {
            LoginScreen(
                onLogin = authViewModel::login,
                onRegister = authViewModel::register
            )
        }

        is AuthState.Locked -> {
            UnlockScreen(
                reason = state.reason,
                onBiometric = {
                    authViewModel
                        .unlockWithBiometric()
                },
                onPin = { pin ->
                    authViewModel
                        .unlockWithPin(pin)
                }
            )
        }

        is AuthState.Authenticated -> {
            VineyardApp(
                user = state.user,
                onLogout = authViewModel::logout
            )
        }

        is AuthState.Error -> {
            AuthErrorScreen(
                error = state.error,
                onRetry = {
                    authViewModel.initialize()
                }
            )
        }
    }
}