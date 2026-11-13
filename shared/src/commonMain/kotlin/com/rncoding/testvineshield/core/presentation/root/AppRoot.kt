package com.rncoding.testvineshield.core.presentation.root

import com.rncoding.testvineshield.core.presentation.navigation.VineyardLinkInbox
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
    modifier: Modifier = Modifier,
    vineyardLinks: VineyardLinkInbox? = null
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
            AppStartupUiState.Ready -> SessionRoot(vineyardLinks)
        }
    }
}

@Composable
private fun SessionRoot(vineyardLinks: VineyardLinkInbox?) {
    // AuthViewModel restores the session in init. Create it only after readiness.
    val authViewModel = koinViewModel<AuthViewModel>()

    val authState by authViewModel.authState.collectAsState()
    // Keep navigation/form state behind a lock, keyed to its owner; logout discards it.
    val appStateHolder = rememberSaveableStateHolder()
    var appStateOwner by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(authState) {
        val state = authState
        if (state is AuthState.Unauthenticated) {
            appStateOwner?.let { appStateHolder.removeState(it) }
            appStateOwner = null
        } else if (state is AuthState.Authenticated) {
            if (appStateOwner != state.user.userId) appStateOwner?.let { appStateHolder.removeState(it) }
            appStateOwner = state.user.userId
        }
    }

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
            appStateHolder.SaveableStateProvider(state.user.userId) {
                AuthenticatedApp(
                    userId = state.user.userId,
                    vineyardLinks = vineyardLinks
                )
            }

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
