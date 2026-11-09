package com.rncoding.testvineshield.core.presentation.root

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
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
    val authViewModel = koinViewModel<AuthViewModel>()

    val authState by authViewModel.authState.collectAsState()

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, authViewModel) {
        val observer = LifecycleEventObserver { _, _ ->
            authViewModel.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        }
        lifecycle.addObserver(observer)
        authViewModel.setForeground(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        onDispose { lifecycle.removeObserver(observer); authViewModel.setForeground(false) }
    }
    CompositionLocalProvider(LocalUserActivity provides authViewModel::recordInteraction) {
        Box(Modifier.fillMaxSize().userActivity(authViewModel::recordInteraction)) {
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
    }
}
