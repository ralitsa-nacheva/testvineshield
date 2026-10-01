package com.rncoding.testvineshield.core.presentation.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.rncoding.testvineshield.core.domain.auth.AuthState
import com.rncoding.testvineshield.core.presentation.navigation.AuthenticatedApp
import com.rncoding.testvineshield.core.presentation.user_auth.AuthViewModel
import com.rncoding.testvineshield.core.presentation.user_auth.LockScreen
import com.rncoding.testvineshield.core.presentation.user_auth.LoginRegisterScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppRoot(
    modifier: Modifier = Modifier
) {
    val authViewModel = koinViewModel<AuthViewModel>()

    val authState by authViewModel.authState.collectAsState()

    when (val state = authState) {

        AuthState.Loading -> {
            LoadingScreen()
        }

        is AuthState.Unauthenticated -> {
            LoginRegisterScreen(
                viewModel = authViewModel,
                reason = state.reason
            )
        }

        is AuthState.Locked -> {
            LockScreen(
                viewModel = authViewModel,
                reason = state.reason
            )
        }

        is AuthState.Authenticated ->
            AuthenticatedApp(
                userId = state.user.userId
            )

        is AuthState.Error -> {
            AuthStartupErrorScreen(
                error = state.error,
                onRetry = authViewModel::retryRestoreSession
            )
        }
    }
}