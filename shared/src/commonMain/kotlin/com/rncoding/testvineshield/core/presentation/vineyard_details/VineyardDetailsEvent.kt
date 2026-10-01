package com.rncoding.testvineshield.core.presentation.vineyard_details

sealed interface VineyardDetailsEvent {

    data class ShowSnackbar(
        val message: String
    ) : VineyardDetailsEvent

    data object VineyardDeleted :
        VineyardDetailsEvent
}