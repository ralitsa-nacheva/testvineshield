package com.rncoding.testvineshield.core.presentation.vineyard_editor

import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.VineyardValidationError
data class VineyardEditorUiState(
    val mode: VineyardEditorMode,

    val name: String = "",
    val size: String = "",
    val sizeUnit: AreaUnit? = null,
    val country: String = "",
    val city: String = "",
    val latitude: String = "",
    val longitude: String = "",
    val timeZone: String = "",
    val elevation: String = "",

    val nameError: VineyardValidationError? = null,
    val sizeError: VineyardValidationError? = null,
    val countryError: VineyardValidationError? = null,
    val cityError: VineyardValidationError? = null,
    val latitudeError: VineyardValidationError? = null,
    val longitudeError: VineyardValidationError? = null,
    val timeZoneError: VineyardValidationError? = null,
    val elevationError: VineyardValidationError? = null,

    val isLoading: Boolean =
        mode is VineyardEditorMode.Edit,

    val isSaving: Boolean = false,

    val loadError: AppError? = null
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
                loadError == null
}