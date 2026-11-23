package com.rncoding.testvineshield.core.presentation.vineyard_editor

import com.rncoding.testvineshield.core.domain.error.AppError

sealed interface VineyardEditorEvent {

    data class ShowSnackbar(
        val error: AppError
    ) : VineyardEditorEvent

    data class Saved(
        val vineyardId: Long
    ) : VineyardEditorEvent
}