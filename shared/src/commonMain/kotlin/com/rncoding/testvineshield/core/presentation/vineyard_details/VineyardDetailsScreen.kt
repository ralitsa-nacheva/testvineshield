package com.rncoding.testvineshield.core.presentation.vineyard_details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VineyardDetailsScreen(
    state: VineyardDetailsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteCancel: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.details?.name
                            ?: "Vineyard"
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack
                    ) {
                        Text("Back")
                    }
                },
                actions = {
                    if (state.details != null) {
                        TextButton(
                            onClick = onEdit,
                            enabled = !state.isDeleting
                        ) {
                            Text("Edit")
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState =
                    snackbarHostState
            )
        }
    )

    { innerPadding ->

        val details = state.details
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.errorMessage)
                }
            }

            details != null -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        VineyardInformationSection(
                            details = details
                        )
                    }

                    item {
                        WeatherSection(
                            weather = details.weather
                        )
                    }

                    item {
                        WeatherRiskSection(
                            risk = details.weatherRisk
                        )
                    }

                    item {
                        SectionTitle("Blocks")
                    }

                    if (details.blocks.isEmpty()) {
                        item {
                            EmptySectionText(
                                "No blocks have been added."
                            )
                        }
                    } else {
                        items(
                            items = details.blocks,
                            key = { it.blockId }
                        ) { block ->
                            BlockSummaryCard(block)
                        }
                    }

                    item {
                        SectionTitle("Active diseases")
                    }

                    if (details.activeDiseases.isEmpty()) {
                        item {
                            EmptySectionText(
                                "No active disease occurrences."
                            )
                        }
                    } else {
                        items(
                            items = details.activeDiseases,
                            key = { it.occurrenceId }
                        ) { disease ->
                            DiseaseSummaryCard(disease)
                        }
                    }

                    item {
                        SectionTitle("Active alerts")
                    }

                    if (details.activeAlerts.isEmpty()) {
                        item {
                            EmptySectionText(
                                "No active disease alerts."
                            )
                        }
                    } else {
                        items(
                            items = details.activeAlerts,
                            key = { it.alertId }
                        ) { alert ->
                            AlertSummaryCard(alert)
                        }
                    }

                    item {
                        OutlinedButton(
                            onClick = onDeleteRequest,
                            enabled = !state.isDeleting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete vineyard")
                        }
                    }
                }
            }
        }

        if (state.showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = {
                    if (!state.isDeleting) {
                        onDeleteCancel()
                    }
                },
                title = {
                    Text("Delete vineyard?")
                },
                text = {
                    Text(
                        "This will permanently delete the vineyard " +
                                "and its related local data."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = onDeleteConfirm,
                        enabled = !state.isDeleting
                    ) {
                        if (state.isDeleting) {
                            CircularProgressIndicator()
                        } else {
                            Text("Delete")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = onDeleteCancel,
                        enabled = !state.isDeleting
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

    @Composable
    private fun VineyardInformationSection(
        details: VineyardDetailsUiModel
    ) {
        Column(
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = details.name,
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = listOf(
                    details.city,
                    details.country
                )
                    .filter { it.isNotBlank() }
                    .joinToString(", ")
            )

            Text("Size: ${details.size}")

            Text(
                "Elevation: ${details.elevation} m"
            )

            Text(
                "Time zone: ${details.timeZone}"
            )
        }
    }

    @Composable
    private fun WeatherSection(
        weather: WeatherUiModel?
    ) {
        Column(
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            SectionTitle("Current weather")

            if (weather == null) {
                EmptySectionText(
                    "Weather data is not available yet."
                )
                return
            }

            Text(
                "Temperature: ${weather.temperature} °C"
            )

            Text(
                "Humidity: ${weather.relativeHumidity} %"
            )

            Text(
                "Precipitation: ${weather.precipitation} mm"
            )

            Text(
                "Wind: ${weather.windSpeed} km/h"
            )
        }
    }
//temporary
@Composable
private fun WeatherRiskSection(
    risk: WeatherRiskUiModel?
) {
    Column(
        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {
        SectionTitle("Disease risk")

        if (risk == null) {
            EmptySectionText(
                "Disease risk has not been calculated yet."
            )
            return
        }

        Text(
            "Infection score: ${risk.infectionScore}"
        )

        Text(
            if (risk.powderyMildewInitialInfection) {
                "Powdery mildew infection conditions detected."
            } else {
                "No initial powdery mildew infection conditions detected."
            }
        )

        Text(
            "Wetness hours: ${risk.wetnessHoursAt10C}"
        )
    }
}

@Composable
private fun BlockSummaryCard(
    block: BlockUiModel
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = block.name,
                style =
                    MaterialTheme.typography.titleMedium
            )

            Text(
                "Variety: ${block.vineVariety}"
            )

            Text(
                "Area: ${block.area}"
            )

            block.phenologicalStage?.let {
                Text(
                    "Phenological stage: ${it.name}"
                )
            }

            block.latestActivityType?.let {
                Text(
                    "Latest activity: $it"
                )
            }

            block.latestActivityStatus?.let {
                Text(
                    "Activity status: ${it.name}"
                )
            }

            Text(
                "Active diseases: " +
                        block.activeDiseaseCount
            )

            Text(
                "Active alerts: " +
                        block.activeAlertCount
            )
        }
    }
}

@Composable
private fun DiseaseSummaryCard(
    disease: DiseaseUiModel
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = disease.diseaseName,
                style =
                    MaterialTheme.typography.titleMedium
            )

            Text(
                text =
                    disease.blockName
                        ?.let { "Block: $it" }
                        ?: "Whole vineyard"
            )

            Text(
                "Severity: ${disease.severity}%"
            )

            Text(
                "Observed: ${disease.observedAt}"
            )

            Text(
                "Status: ${disease.status.name}"
            )
        }
    }
}

@Composable
private fun AlertSummaryCard(
    alert: AlertUiModel
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = alert.diseaseName,
                style =
                    MaterialTheme.typography.titleMedium
            )

            Text(alert.alert)

            Text(
                alert.blockName
                    ?.let { "Block: $it" }
                    ?: "Whole vineyard"
            )

            Text(
                "Severity: ${alert.severity.name}"
            )

            Text(
                "Phenological stage: " +
                        alert.phenologicalStage.name
            )

            Text(
                "Created: ${alert.createdAt}"
            )
        }
    }
}

@Composable
private fun SectionTitle(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge
    )
}

@Composable
private fun EmptySectionText(
    text: String
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium
    )
}

