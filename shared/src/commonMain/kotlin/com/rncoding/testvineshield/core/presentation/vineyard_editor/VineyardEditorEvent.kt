package com.rncoding.testvineshield.core.presentation.vineyard_editor

sealed interface VineyardEditorEvent {

    data class ShowSnackbar(
        val message: String
    ) : VineyardEditorEvent

    data class Saved(
        val vineyardId: Long
    ) : VineyardEditorEvent
}