package com.rncoding.testvineshield.core.presentation.vineyard_details



data class VineyardDetailsUiState(
    val isLoading: Boolean = true,
    val details: VineyardDetailsUiModel? = null,
    val errorMessage: String? = null,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false
)