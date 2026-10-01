package com.rncoding.testvineshield.core.presentation.vineyard_editor

data class VineyardEditorUiState(
    val mode: VineyardEditorMode,

    val name: String = "",
    val size: String = "",
    val country: String = "",
    val city: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val timeZone: String = "",
    val elevation: String = "",

    val nameError: String? = null,
    val sizeError: String? = null,
    val countryError: String? = null,
    val cityError: String? = null,
    val latitudeError: String? = null,
    val longitudeError: String? = null,
    val timeZoneError: String? = null,
    val elevationError: String? = null,

    val isLoading: Boolean =
        mode is VineyardEditorMode.Edit,

    val isSaving: Boolean = false,

    val loadErrorMessage: String? = null
) {
    val title: String
        get() = when (mode) {
            VineyardEditorMode.Create ->
                "Create vineyard"

            is VineyardEditorMode.Edit ->
                "Edit vineyard"
        }

    val canEdit: Boolean
        get() = !isLoading &&
                !isSaving &&
                loadErrorMessage == null
}