package com.rncoding.testvineshield.core.presentation.vineyard_list

import com.rncoding.testvineshield.core.domain.error.AppError

sealed interface VineyardListEvent {

    data class ShowSnackbar(
        val error: AppError
    ) : VineyardListEvent

    data object VineyardDeleted :
        VineyardListEvent
}