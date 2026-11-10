package com.rncoding.testvineshield.core.presentation.vineyard_details

import com.rncoding.testvineshield.core.presentation.formatting.formatPlainDecimal
import com.rncoding.testvineshield.core.presentation.formatting.formatBlockArea
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
import com.rncoding.testvineshield.core.presentation.root.userAction
import com.rncoding.testvineshield.core.presentation.root.userActivity
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
    onDeleteCancel: () -> Unit,
    onBlocks: () -> Unit = {},
    onActivities: () -> Unit = {},
    onWeather: () -> Unit = {}
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
                        onClick = userAction(onBack)
                    ) {
                        Text("Back")
                    }
                },
                actions = {
                    if (state.details != null) {
                        TextButton(
                            onClick = userAction(onEdit),
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
                        SectionTitle("Weather and monitoring")
                        Text("Review cached history, forecast age and saved monitoring settings.")
                        TextButton(onClick = userAction(onWeather)) { Text("Open weather and monitoring") }
                        Text("Disease risk is not assessed here. No active alerts does not establish that the vineyard is safe.")
                    }

                    item {
                        TextButton(onClick=userAction(onActivities)) { Text("Vineyard activities") }
                        SectionTitle("Blocks")
                        TextButton(onClick=userAction(onBlocks)) { Text("Manage blocks") }
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
                            onClick = userAction(onDeleteRequest),
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
            modifier = Modifier.userActivity(com.rncoding.testvineshield.core.presentation.root.LocalUserActivity.current),
                onDismissRequest = userAction {
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
                        onClick = userAction(onDeleteConfirm),
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
                        onClick = userAction(onDeleteCancel),
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

            Text("Size: ${details.size} ${details.sizeUnit?.symbol ?: "(unit unknown)"}")

            Text(
                "Elevation: " + (details.elevation?.let { "${formatPlainDecimal(it)} m" } ?: "Unknown")
            )

            Text(
                "Time zone: ${details.timeZone}"
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
                "Area: ${formatBlockArea(block.area, block.areaUnit)}"
            )

            Text("Phenological stage: ${block.phenologicalStage?.name ?: "Unknown"}")

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

