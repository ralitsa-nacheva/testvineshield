package com.rncoding.testvineshield.core.presentation.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

    LaunchedEffect(viewModel) {
        viewModel.load()
    }

    SecuritySettingsScreen(
        state = state,
        onBack = onBack,
        onBiometricChanged = viewModel::setBiometricEnabled,
        onPinChanged = viewModel::setPinEnabled,
        onClearError = viewModel::clearError
    )
}