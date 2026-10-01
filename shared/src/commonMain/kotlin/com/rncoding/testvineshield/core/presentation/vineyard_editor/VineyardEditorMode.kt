package com.rncoding.testvineshield.core.presentation.vineyard_editor

sealed interface VineyardEditorMode {

    data object Create : VineyardEditorMode

    data class Edit(
        val vineyardId: Long
    ) : VineyardEditorMode
}