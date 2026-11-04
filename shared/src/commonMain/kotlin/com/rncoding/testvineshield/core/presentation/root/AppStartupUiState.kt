package com.rncoding.testvineshield.core.presentation.root

sealed interface AppStartupUiState {
    data object Loading : AppStartupUiState
    data object Ready : AppStartupUiState
    data object Failed : AppStartupUiState
}
