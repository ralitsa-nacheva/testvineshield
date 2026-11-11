package com.rncoding.testvineshield.core.presentation.vineyard_editor

import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
import com.rncoding.testvineshield.core.domain.time.VineyardTimeZoneCatalog
import com.rncoding.testvineshield.core.presentation.formatting.formatPlainDecimal
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rncoding.testvineshield.core.domain.datamodels.VineyardDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.VineyardInput
import com.rncoding.testvineshield.core.domain.error.Result
import com.rncoding.testvineshield.core.domain.error.VineyardField
import com.rncoding.testvineshield.core.domain.error.VineyardValidationError
import com.rncoding.testvineshield.core.domain.usecases.vineyard.CreateVineyardUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.GetVineyardUseCase
import com.rncoding.testvineshield.core.domain.usecases.vineyard.UpdateVineyardUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VineyardEditorViewModel(
    private val mode: VineyardEditorMode,
    private val createVineyardUseCase: CreateVineyardUseCase,
    private val updateVineyardUseCase: UpdateVineyardUseCase,
    private val getVineyardUseCase: GetVineyardUseCase,
    private val timeZoneCatalog: VineyardTimeZoneCatalog = VineyardTimeZoneCatalog()
) : ViewModel() {

    private val _state =
        MutableStateFlow(
            VineyardEditorUiState(
                mode = mode
            )
        )

    val state: StateFlow<VineyardEditorUiState> =
        _state.asStateFlow()

    private val _events =
        Channel<VineyardEditorEvent>(
            capacity = Channel.BUFFERED
        )

    val events =
        _events.receiveAsFlow()

    init {
        if (mode is VineyardEditorMode.Edit) {
            loadVineyard(mode.vineyardId)
        }
    }

    fun applyMapPoint(point: com.rncoding.testvineshield.core.domain.datamodels.VineyardMapPoint,timeZone: String?) {
        if(!_state.value.canEdit) return
        val suggested=timeZone?.takeIf(timeZoneCatalog::contains)
        _state.update { it.copy(latitude=formatPlainDecimal(point.latitude),longitude=formatPlainDecimal(point.longitude),
            latitudeError=null,longitudeError=null,timeZone=suggested ?: it.timeZone,
            timeZoneError=if(suggested!=null) null else it.timeZoneError) }
    }

    fun onNameChange(value: String) {
        _state.update {
            it.copy(
                name = value,
                nameError = null
            )
        }
    }

    fun onSizeChange(value: String) {
        _state.update {
            it.copy(
                size = value,
                sizeError = null
            )
        }
    }

    fun onCountryChange(value: String) {
        _state.update {
            it.copy(
                country = value,
                countryError = null
            )
        }
    }

    fun onCityChange(value: String) {
        _state.update {
            it.copy(
                city = value,
                cityError = null
            )
        }
    }

    fun onLatitudeChange(value: String) {
        _state.update {
            it.copy(
                latitude = value,
                latitudeError = null
            )
        }
    }

    fun onLongitudeChange(value: String) {
        _state.update {
            it.copy(
                longitude = value,
                longitudeError = null
            )
        }
    }

    fun onTimeZoneChange(value: String) {
        if (!_state.value.canEdit || !timeZoneCatalog.contains(value)) return
        _state.update {
            it.copy(
                timeZone = value,
                timeZoneError = null
            )
        }
    }

    fun onElevationChange(value: String) {
        if (!_state.value.canEdit) return
        _state.update {
            it.copy(
                elevation = value,
                elevationError = null
            )
        }
    }

    fun save() {
        val current = _state.value

        if (current.isLoading ||
            current.isSaving ||
            current.loadErrorMessage != null
        ) {
            return
        }

        clearErrors()

        val input = createInput(current)
            ?: return

        _state.update {
            it.copy(
                isSaving = true
            )
        }

        viewModelScope.launch {
            when (val currentMode = mode) {
                VineyardEditorMode.Create ->
                    create(input)

                is VineyardEditorMode.Edit ->
                    update(
                        vineyardId =
                            currentMode.vineyardId,
                        input = input
                    )
            }
        }
    }

    private fun loadVineyard(
        vineyardId: Long
    ) {
        viewModelScope.launch {
            when (
                val result =
                    getVineyardUseCase(vineyardId)
            ) {
                is Result.Success<*> -> {
                    val vineyard =
                        result.data
                                as VineyardDomainModel?

                    if (vineyard == null) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                loadErrorMessage =
                                    "Vineyard could not be found."
                            )
                        }

                        return@launch
                    }

                    populate(vineyard)
                }

                is Result.Error<*> -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            loadErrorMessage =
                                result.error.userMessage
                        )
                    }
                }

                else -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            loadErrorMessage =
                                "Unable to load the vineyard."
                        )
                    }
                }
            }
        }
    }

    fun onSizeUnitChange(unit: AreaUnit?) {
        if (!_state.value.canEdit) return
        _state.update { it.copy(sizeUnit = unit) }
    }

    private fun populate(
        vineyard: VineyardDomainModel
    ) {
        _state.update {
            it.copy(
                name = vineyard.name,
                size = vineyard.size.toString(),
                sizeUnit = vineyard.sizeUnit,
                country = vineyard.country,
                city = vineyard.city,
                latitude =
                    vineyard.latitude.toString(),
                longitude =
                    vineyard.longitude.toString(),
                timeZone = vineyard.timeZone,
                elevation =
                    vineyard.elevation?.let(::formatPlainDecimal).orEmpty(),
                isLoading = false,
                loadErrorMessage = null
            )
        }
    }

    private fun createInput(
        state: VineyardEditorUiState
    ): VineyardInput? {

        val size =
            state.size.trim().toDoubleOrNull()

        if (size == null) {
            setFieldError(
                field = VineyardField.SIZE,
                message =
                    "Enter a valid vineyard size."
            )
        }

        val latitude =
            state.latitude.trim().toDoubleOrNull()

        if (latitude == null) {
            setFieldError(
                field = VineyardField.LATITUDE,
                message =
                    "Enter a valid latitude."
            )
        }

        val longitude =
            state.longitude.trim().toDoubleOrNull()

        if (longitude == null) {
            setFieldError(
                field = VineyardField.LONGITUDE,
                message =
                    "Enter a valid longitude."
            )
        }

        val elevation = when (val result = ElevationInputParser.parse(state.elevation)) {
            is ElevationParseResult.Valid -> result.metres
            ElevationParseResult.Invalid -> {
                setFieldError(
                    field = VineyardField.ELEVATION,
                    message = "Enter decimal metres using a dot or comma, without grouping separators, or leave blank."
                )
                return null
            }
        }

        if (size == null ||
            latitude == null ||
            longitude == null
        ) {
            return null
        }

        return VineyardInput(
            name = state.name,
            size = size,
            sizeUnit = state.sizeUnit,
            country = state.country,
            city = state.city,
            latitude = latitude,
            longitude = longitude,
            timeZone = state.timeZone,
            elevation = elevation
        )
    }

    private suspend fun create(
        input: VineyardInput
    ) {
        when (
            val result =
                createVineyardUseCase(input)
        ) {
            is Result.Success<*> -> {
                val vineyardId =
                    result.data as Long

                _state.update {
                    it.copy(
                        isSaving = false
                    )
                }

                _events.send(
                    VineyardEditorEvent.Saved(
                        vineyardId = vineyardId
                    )
                )
            }

            is Result.Error<*> -> {
                handleSaveError(
                    result.error
                )
            }

            else -> {
                handleUnexpectedSaveError()
            }
        }
    }

    private suspend fun update(
        vineyardId: Long,
        input: VineyardInput
    ) {
        when (
            val result =
                updateVineyardUseCase(
                    vineyardId = vineyardId,
                    input = input
                )
        ) {
            is Result.Success<*> -> {
                _state.update {
                    it.copy(
                        isSaving = false
                    )
                }

                _events.send(
                    VineyardEditorEvent.Saved(
                        vineyardId = vineyardId
                    )
                )
            }

            is Result.Error<*> -> {
                handleSaveError(
                    result.error
                )
            }

            else -> {
                handleUnexpectedSaveError()
            }
        }
    }

    private suspend fun handleSaveError(
        error: com.rncoding.testvineshield.core.domain.error.AppError
    ) {
        _state.update {
            it.copy(
                isSaving = false
            )
        }

        if (error is VineyardValidationError) {
            setFieldError(
                field = error.field,
                message = error.userMessage
            )
        } else {
            _events.send(
                VineyardEditorEvent.ShowSnackbar(
                    message = error.userMessage
                )
            )
        }
    }

    private suspend fun handleUnexpectedSaveError() {
        _state.update {
            it.copy(
                isSaving = false
            )
        }

        _events.send(
            VineyardEditorEvent.ShowSnackbar(
                message =
                    "Unable to save the vineyard."
            )
        )
    }

    private fun clearErrors() {
        _state.update {
            it.copy(
                nameError = null,
                sizeError = null,
                countryError = null,
                cityError = null,
                latitudeError = null,
                longitudeError = null,
                timeZoneError = null,
                elevationError = null
            )
        }
    }

    private fun setFieldError(
        field: VineyardField,
        message: String
    ) {
        _state.update { current ->
            when (field) {
                VineyardField.NAME ->
                    current.copy(
                        nameError = message
                    )

                VineyardField.SIZE ->
                    current.copy(
                        sizeError = message
                    )

                VineyardField.COUNTRY ->
                    current.copy(
                        countryError = message
                    )

                VineyardField.CITY ->
                    current.copy(
                        cityError = message
                    )

                VineyardField.LATITUDE ->
                    current.copy(
                        latitudeError = message
                    )

                VineyardField.LONGITUDE ->
                    current.copy(
                        longitudeError = message
                    )

                VineyardField.TIME_ZONE ->
                    current.copy(
                        timeZoneError = message
                    )

                VineyardField.ELEVATION ->
                    current.copy(
                        elevationError = message
                    )
            }
        }
    }
}