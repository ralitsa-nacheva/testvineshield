package com.rncoding.testvineshield.core.presentation.vineyard_editor

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import com.rncoding.testvineshield.core.domain.datamodels.VineyardMapPoint
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun VineyardEditorScreenRoot(
    mode: VineyardEditorMode,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val viewModel =
        koinViewModel<VineyardEditorViewModel>(
            key = when (mode) {
                VineyardEditorMode.Create ->
                    "vineyard-create"

                is VineyardEditorMode.Edit ->
                    "vineyard-edit-${mode.vineyardId}"
            },
            parameters = {parametersOf(mode)}
        )

    val state by
    viewModel.state
        .collectAsStateWithLifecycle()

    val snackbarHostState =
        remember {
            SnackbarHostState()
        }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is VineyardEditorEvent.ShowSnackbar ->
                    snackbarHostState.showSnackbar(
                        event.message
                    )

                is VineyardEditorEvent.Saved ->
                    onSaved(
                        event.vineyardId
                    )
            }
        }
    }

    var mapOpen by remember { mutableStateOf(false) }
    LaunchedEffect(state.canEdit) { if(!state.canEdit) mapOpen=false }
    if(mapOpen && state.canEdit) {
        val lat=state.latitude.toDoubleOrNull(); val lon=state.longitude.toDoubleOrNull()
        VineyardMapPicker(if(lat!=null && lon!=null) VineyardMapPoint.from(lat,lon) else null,
            onDismiss={ mapOpen=false },onUse={ point,zone -> viewModel.applyMapPoint(point,zone); mapOpen=false })
    }

    VineyardEditorScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onNameChange =
            viewModel::onNameChange,
        onSizeChange =
            viewModel::onSizeChange,
        onSizeUnitChange = viewModel::onSizeUnitChange,
        onCountryChange =
            viewModel::onCountryChange,
        onCityChange =
            viewModel::onCityChange,
        onLatitudeChange =
            viewModel::onLatitudeChange,
        onLongitudeChange =
            viewModel::onLongitudeChange,
        onTimeZoneChange =
            viewModel::onTimeZoneChange,
        onElevationChange =
            viewModel::onElevationChange,
        onMap = { if(state.canEdit) mapOpen=true },
        onSave =
            viewModel::save
    )
}