package com.rncoding.testvineshield.core.presentation.vineyard_details

import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel

data class VineyardDetailsUiState(
    val isLoading: Boolean = true,
    val vineyard: VineyardDomainModel? = null,
    val errorMessage: String? = null,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false
)