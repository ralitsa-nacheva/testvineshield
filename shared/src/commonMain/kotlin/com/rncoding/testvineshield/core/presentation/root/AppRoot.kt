package com.rncoding.testvineshield.core.presentation.root

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rncoding.testvineshield.core.domain.error.AppInitializationError
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
    val startupViewModel = koinViewModel<AppStartupViewModel>()
    val startupState by startupViewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier) {
        when (startupState) {
            AppStartupUiState.Loading -> LoadingScreen()
            AppStartupUiState.Failed -> AuthStartupErrorScreen(
                error = AppInitializationError,
                onRetry = startupViewModel::retry
            )
            AppStartupUiState.Ready -> SessionRoot()
        }
    }
}

@Composable
private fun SessionRoot() {
    // AuthViewModel restores the session in init. Create it only after readiness.
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