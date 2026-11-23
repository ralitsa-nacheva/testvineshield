package com.rncoding.testvineshield.core.presentation.vineyard_details

import com.rncoding.testvineshield.core.presentation.vineyards.VineyardLoadIssue



data class VineyardDetailsUiState(
    val isLoading: Boolean = true,
    val details: VineyardDetailsUiModel? = null,
    val loadIssue: VineyardLoadIssue? = null,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false
)