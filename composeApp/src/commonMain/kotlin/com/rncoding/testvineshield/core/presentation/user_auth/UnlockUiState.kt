package com.rncoding.testvineshield.core.presentation.user_auth

import com.rncoding.testvineshield.core.domain.error.AppError

sealed interface UnlockUiState {

    data object Idle : UnlockUiState

    data object Authenticating : UnlockUiState

    data object Unlocked : UnlockUiState

    data class Error(
        val error: AppError
    ) : UnlockUiState
}