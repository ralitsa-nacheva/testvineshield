package com.rncoding.testvineshield.core.presentation.security

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

    SecuritySettingsScreen(
        state = state,
        onBack = onBack,
        onBiometricChanged = viewModel::setBiometricEnabled,
        onDeviceCredentialChanged = viewModel::setDeviceCredentialEnabled,
        onLockChanged = viewModel::setLockEnabled,
        onTimeoutChanged = viewModel::setTimeout,
        onReload = viewModel::load,
        onClearError = viewModel::clearError
    )
}