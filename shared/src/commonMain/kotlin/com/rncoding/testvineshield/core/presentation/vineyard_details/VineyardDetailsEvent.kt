package com.rncoding.testvineshield.core.presentation.vineyard_details

import com.rncoding.testvineshield.core.domain.error.AppError

sealed interface VineyardDetailsEvent {

    data class ShowSnackbar(
        val error: AppError
    ) : VineyardDetailsEvent

    data object VineyardDeleted :
        VineyardDetailsEvent
}