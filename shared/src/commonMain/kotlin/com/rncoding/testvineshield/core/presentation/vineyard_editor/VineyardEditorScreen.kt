package com.rncoding.testvineshield.core.presentation.vineyard_editor

import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import com.rncoding.testvineshield.core.presentation.error.LocalizedErrorSnackbarHost
import com.rncoding.testvineshield.core.presentation.error.localizedAppErrorMessage
import com.rncoding.testvineshield.core.domain.error.VineyardValidationError
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.input.KeyboardType
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.Composable
import com.rncoding.testvineshield.core.presentation.root.LocalUserActivity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.rncoding.testvineshield.core.presentation.root.userAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VineyardEditorScreen(
    state: VineyardEditorUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onSizeChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onTimeZoneChange: (String) -> Unit,
    onElevationChange: (String) -> Unit,
    onSave: () -> Unit,
    onSizeUnitChange: (AreaUnit?) -> Unit,
    onMap: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(when (state.mode) {
                        VineyardEditorMode.Create -> Res.string.create_vineyard
                        is VineyardEditorMode.Edit -> Res.string.edit_vineyard
                    }))
                },
                navigationIcon = {
                    TextButton(
                        onClick = userAction(onBack)
                    ) {
                        Text(stringResource(Res.string.back_action))
                    }
                }
            )
        },
        snackbarHost = {
            LocalizedErrorSnackbarHost(
                hostState = snackbarHostState
            )
        }
    ) { innerPadding ->

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment =
                        Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.loadError != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment =
                        Alignment.Center
                ) {
                    Column(
                        horizontalAlignment =
                            Alignment.CenterHorizontally,
                        verticalArrangement =
                            Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text =
                                localizedAppErrorMessage(state.loadError)
                        )

                        Button(
                            onClick = userAction(onBack)
                        ) {
                            Text(stringResource(Res.string.back_action))
                        }
                    }
                }
            }

            else -> {
                VineyardEditorForm(
                    state = state,
                    contentPadding =
                        innerPadding,
                    onNameChange =
                        onNameChange,
                    onSizeChange =
                        onSizeChange,
                    onCountryChange =
                        onCountryChange,
                    onCityChange =
                        onCityChange,
                    onLatitudeChange =
                        onLatitudeChange,
                    onLongitudeChange =
                        onLongitudeChange,
                    onTimeZoneChange =
                        onTimeZoneChange,
                    onElevationChange =
                        onElevationChange,
                    onSave = onSave,
                    onSizeUnitChange = onSizeUnitChange,
                    onMap = onMap
                )
            }
        }
    }
}

@Composable
private fun VineyardEditorForm(
    state: VineyardEditorUiState,
    contentPadding: PaddingValues,
    onNameChange: (String) -> Unit,
    onSizeChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onTimeZoneChange: (String) -> Unit,
    onElevationChange: (String) -> Unit,
    onSave: () -> Unit,
    onSizeUnitChange: (AreaUnit?) -> Unit,
    onMap: () -> Unit
) {
    val elevationSignDescription = stringResource(Res.string.change_elevation_sign)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            end = 16.dp,
            bottom =
                contentPadding.calculateBottomPadding() +
                        24.dp
        ),
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        item {
            VineyardTextField(
                enabled = state.canEdit,
                value = state.name,
                onValueChange = onNameChange,
                label = stringResource(Res.string.vineyard_name),
                error = state.nameError
            )
        }

        item {
            VineyardTextField(
                enabled = state.canEdit,
                value = state.size,
                onValueChange = onSizeChange,
                label = stringResource(Res.string.vineyard_size),
                error = state.sizeError,
                keyboardType =
                    KeyboardType.Decimal
            )
        }

        item {
            Column(modifier = Modifier.selectableGroup()) {
                Text(stringResource(Res.string.area_unit))
                (listOf<AreaUnit?>(null) + AreaUnit.entries).forEach { unit ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                            selected = state.sizeUnit == unit,
                            enabled = state.canEdit,
                            role = androidx.compose.ui.semantics.Role.RadioButton,
                            onClick = userAction { onSizeUnitChange(unit) }
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = state.sizeUnit == unit, onClick = null, enabled = state.canEdit
                        )
                        Text(unit?.symbol ?: stringResource(Res.string.unit_unspecified))
                    }
                }
                Text(stringResource(Res.string.unit_change_note))
            }
        }

        item {
            VineyardTextField(
                enabled = state.canEdit,
                value = state.country,
                onValueChange = onCountryChange,
                label = stringResource(Res.string.country_label),
                error = state.countryError
            )
        }

        item {
            VineyardTextField(
                enabled = state.canEdit,
                value = state.city,
                onValueChange = onCityChange,
                label = stringResource(Res.string.locality_label),
                error = state.cityError
            )
        }

        item {
            TextButton(onClick=userAction(onMap),enabled=state.canEdit) { Text(stringResource(Res.string.choose_online_map)) }
            Text(stringResource(Res.string.location_change_note))
        }

        item {
            VineyardTextField(
                enabled = state.canEdit,
                value = state.latitude,
                onValueChange =
                    onLatitudeChange,
                label = stringResource(Res.string.latitude_label),
                error = state.latitudeError,
                keyboardType =
                    KeyboardType.Decimal
            )
        }

        item {
            VineyardTextField(
                enabled = state.canEdit,
                value = state.longitude,
                onValueChange =
                    onLongitudeChange,
                label = stringResource(Res.string.longitude_label),
                error = state.longitudeError,
                keyboardType =
                    KeyboardType.Decimal
            )
        }

        item {
            VineyardTimeZonePicker(
                selectedId = state.timeZone,
                errorMessage = state.timeZoneError?.let { localizedVineyardValidationError(it) },
                enabled = state.canEdit,
                onSelect = onTimeZoneChange
            )
        }

        item {
            VineyardTextField(
                enabled = state.canEdit,
                value = state.elevation,
                onValueChange =
                    onElevationChange,
                label = stringResource(Res.string.elevation_label),
                error = state.elevationError,
                trailingIcon = {
                    TextButton(
                        enabled = state.canEdit,
                        modifier = Modifier.semantics { contentDescription = elevationSignDescription },
                        onClick = userAction { onElevationChange(toggleElevationSign(state.elevation)) }
                    ) { Text("±") }
                },
                supportingText = stringResource(Res.string.elevation_help),
                keyboardType = KeyboardType.Decimal
            )
        }

        item {
            Button(
                onClick = userAction(onSave),
                enabled =
                    state.canEdit,
                modifier =
                    Modifier.fillMaxWidth()
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        when (state.mode) {
                            VineyardEditorMode.Create ->
                                stringResource(Res.string.create_vineyard)

                            is VineyardEditorMode.Edit ->
                                stringResource(Res.string.save_changes)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun VineyardTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: VineyardValidationError?,
    keyboardType: KeyboardType =
        KeyboardType.Text,
    supportingText: String? = null,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    val activity = LocalUserActivity.current
    OutlinedTextField(
        value = value,
        onValueChange = { activity(); onValueChange(it) },
        label = {
            Text(label)
        },
        modifier =
            Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        trailingIcon = trailingIcon,
        isError = error != null,
        keyboardOptions =
            KeyboardOptions(
                keyboardType = keyboardType
            ),
        supportingText = {
            when {
                error != null ->
                    Text(localizedVineyardValidationError(error))

                supportingText != null ->
                    Text(supportingText)
            }
        }
    )
}