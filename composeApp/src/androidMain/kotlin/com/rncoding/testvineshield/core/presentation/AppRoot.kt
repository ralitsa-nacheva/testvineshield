package com.rncoding.testvineshield.core.presentation

import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.domain.auth.AuthState
import com.rncoding.testvineshield.core.presentation.auth.AuthScreen
import com.rncoding.testvineshield.core.presentation.dashboard.DashboardScreen
import com.rncoding.testvineshield.core.presentation.error.ErrorScreen
import com.rncoding.testvineshield.core.presentation.loading.LoadingScreen

/**
 * Root navigation/state dispatcher for the application.
 *
 * AppRoot does not perform authentication itself.
 *
 * It simply renders the UI appropriate for AuthState.
 */
@Composable
fun AppRoot(
    viewModel: AppViewModel
) {

    when (val state = viewModel.authState) {

        AuthState.Loading -> {

            LoadingScreen()
        }

        is AuthState.Unauthenticated -> {

            AuthScreen()
        }

        is AuthState.Authenticated -> {

            DashboardScreen(
                userId = state.user.userId
            )
        }

        is AuthState.Locked -> {

            /*
             * The Locked screen should launch local
             * authentication and call UnlockSessionUseCase
             * through the appropriate ViewModel.
             */
            LockedScreen(
                reason = state.reason
            )
        }

        is AuthState.Error -> {

            ErrorScreen(
                error = state.error
            )
        }
    }
}