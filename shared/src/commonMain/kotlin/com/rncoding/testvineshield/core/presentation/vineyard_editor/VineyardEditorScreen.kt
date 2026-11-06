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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onSizeUnitChange: (AreaUnit?) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(state.title)
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack
                    ) {
                        Text("Back")
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
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

            state.loadErrorMessage != null -> {
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
                                state.loadErrorMessage
                        )

                        Button(
                            onClick = onBack
                        ) {
                            Text("Back")
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
                    onSizeUnitChange = onSizeUnitChange
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
    onSizeUnitChange: (AreaUnit?) -> Unit
) {
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
                label = "Vineyard name",
                error = state.nameError
            )
        }

        item {
            VineyardTextField(
                enabled = state.canEdit,
                value = state.size,
                onValueChange = onSizeChange,
                label = "Size (in the selected unit)",
                error = state.sizeError,
                keyboardType =
                    KeyboardType.Decimal
            )
        }

        item {
            Column(modifier = Modifier.selectableGroup()) {
                Text("Area unit")
                (listOf<AreaUnit?>(null) + AreaUnit.entries).forEach { unit ->
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(
                            selected = state.sizeUnit == unit,
                            enabled = state.canEdit,
                            role = androidx.compose.ui.semantics.Role.RadioButton,
                            onClick = { onSizeUnitChange(unit) }
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.material3.RadioButton(
                            selected = state.sizeUnit == unit, onClick = null, enabled = state.canEdit
                        )
                        Text(unit?.symbol ?: "Unit not specified")
                    }
                }
                Text("Changing the unit does not convert the entered number.")
            }
        }

        item {
            VineyardTextField(
                enabled = state.canEdit,
                value = state.country,
                onValueChange = onCountryChange,
                label = "Country",
                error = state.countryError
            )
        }

        item {
            VineyardTextField(
                enabled = state.canEdit,
                value = state.city,
                onValueChange = onCityChange,
                label = "City",
                error = state.cityError
            )
        }

        item {
            VineyardTextField(
                enabled = state.canEdit,
                value = state.latitude,
                onValueChange =
                    onLatitudeChange,
                label = "Latitude",
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
                label = "Longitude",
                error = state.longitudeError,
                keyboardType =
                    KeyboardType.Decimal
            )
        }

        item {
            VineyardTimeZonePicker(
                selectedId = state.timeZone,
                errorMessage = state.timeZoneError,
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
                label = "Elevation (m, optional)",
                error = state.elevationError,
                trailingIcon = {
                    TextButton(
                        enabled = state.canEdit,
                        modifier = Modifier.semantics { contentDescription = "Change elevation sign" },
                        onClick = { onElevationChange(toggleElevationSign(state.elevation)) }
                    ) { Text("±") }
                },
                supportingText = "Blank means unknown. Decimal dot or comma; no grouping separators.",
                keyboardType = KeyboardType.Decimal
            )
        }

        item {
            Button(
                onClick = onSave,
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
                                "Create vineyard"

                            is VineyardEditorMode.Edit ->
                                "Save changes"
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
    error: String?,
    keyboardType: KeyboardType =
        KeyboardType.Text,
    supportingText: String? = null,
    enabled: Boolean = true,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
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
                    Text(error)

                supportingText != null ->
                    Text(supportingText)
            }
        }
    )
}