package com.rncoding.testvineshield.core.presentation.vineyard_list

sealed interface VineyardListEvent {

    data class ShowSnackbar(
        val message: String
    ) : VineyardListEvent

    data object VineyardDeleted :
        VineyardListEvent
}