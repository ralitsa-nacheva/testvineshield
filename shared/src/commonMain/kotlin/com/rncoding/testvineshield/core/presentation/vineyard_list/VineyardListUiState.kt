package com.rncoding.testvineshield.core.presentation.vineyard_list

data class VineyardListUiState(
    val vineyards: List<VineyardListItemUiModel> = emptyList(),
    val isLoading: Boolean = true,
    val isReordering: Boolean = false,
    val vineyardPendingDeletion: Long? = null,
    val errorMessage: String? = null
)