package com.rncoding.testvineshield.core.presentation.vineyard_editor

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

    VineyardEditorScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onNameChange =
            viewModel::onNameChange,
        onSizeChange =
            viewModel::onSizeChange,
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
        onSave =
            viewModel::save
    )
}