package com.rncoding.testvineshield.core.presentation.security

import com.rncoding.testvineshield.core.presentation.user_auth.localizedLocalAuthenticationPrompt
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SecuritySettingsScreenRoot(
    userId: Long,
    onBack: () -> Unit
) {
    val viewModel: SecuritySettingsViewModel = koinViewModel(
        parameters = {
            parametersOf(userId)
        }
    )

    val state by viewModel.state.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.load()
    }

    val prompt = localizedLocalAuthenticationPrompt(stringResource(Res.string.local_auth_setup_reason))

    SecuritySettingsScreen(
        state = state,
        onBack = onBack,
        onBiometricChanged = { viewModel.setBiometricEnabled(it, prompt) },
        onDeviceCredentialChanged = { viewModel.setDeviceCredentialEnabled(it, prompt) },
        onLockChanged = viewModel::setLockEnabled,
        onTimeoutChanged = viewModel::setTimeout,
        onReload = viewModel::load,
        onClearError = viewModel::clearError
    )
}