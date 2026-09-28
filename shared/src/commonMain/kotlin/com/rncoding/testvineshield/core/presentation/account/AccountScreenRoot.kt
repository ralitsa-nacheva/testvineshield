package com.rncoding.testvineshield.core.presentation.account

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreenRoot(
    onBack: () -> Unit,
    onSecuritySettings: () -> Unit,
    viewModel: AccountViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AccountScreen(
        uiState = uiState,
        onBack = onBack,
        onSecuritySettings = onSecuritySettings,
        onNewEmailChanged = viewModel::onNewEmailChanged,
        onCurrentPasswordChanged = viewModel::onCurrentPasswordChanged,
        onNewPasswordChanged = viewModel::onNewPasswordChanged,
        onConfirmNewPasswordChanged = viewModel::onConfirmNewPasswordChanged,
        onUpdateAccount = viewModel::updateAccount,
        onLogout = viewModel::logout,
        onDeleteAccount = viewModel::deleteAccount,
        onClearError = viewModel::clearError
    )
}