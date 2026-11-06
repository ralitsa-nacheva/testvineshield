package com.rncoding.testvineshield.core.presentation.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
    traceStartup("ROOT resolving startup ViewModel")
    val startupViewModel = koinViewModel<AppStartupViewModel>()
    val startupState by startupViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(startupState) {
        val stage = when (startupState) {
            AppStartupUiState.Loading -> "STARTUP state Loading"
            AppStartupUiState.Ready -> "STARTUP state Ready"
            AppStartupUiState.Failed -> "STARTUP state Failed"
        }
        traceStartup(stage)
    }
    SideEffect { traceStartup("ROOT composition committed") }

    Box(modifier = modifier.fillMaxSize()) {
        when (startupState) {
            AppStartupUiState.Loading -> LoadingScreen("Preparing your local records…")
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
    traceStartup("SESSION resolving auth ViewModel")
    val authViewModel = koinViewModel<AuthViewModel>()

    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        val stage = when (authState) {
            AuthState.Loading -> "SESSION state Loading"
            is AuthState.Unauthenticated -> "SESSION state Unauthenticated"
            is AuthState.Locked -> "SESSION state Locked"
            is AuthState.Authenticated -> "SESSION state Authenticated"
            is AuthState.Error -> "SESSION state Error"
        }
        traceStartup(stage)
    }
    when (val state = authState) {

        AuthState.Loading -> {
            LoadingScreen("Restoring your session…")
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