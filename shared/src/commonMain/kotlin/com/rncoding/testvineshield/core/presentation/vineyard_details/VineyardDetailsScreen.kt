package com.rncoding.testvineshield.core.presentation.vineyard_details

import com.rncoding.testvineshield.core.presentation.components.InfoHelp
import com.rncoding.testvineshield.core.presentation.vineyards.VineyardLoadIssue
import com.rncoding.testvineshield.core.presentation.vineyards.localizedVineyardLoadIssue

import com.rncoding.testvineshield.core.presentation.formatting.formatPlainDecimal
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import com.rncoding.testvineshield.core.domain.datamodels.enums.*
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
import com.rncoding.testvineshield.core.presentation.error.LocalizedErrorSnackbarHost
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
    onWeather: () -> Unit = {},
    onOpenAlert: (Long) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.details?.name
                            ?: stringResource(Res.string.vineyard_title)
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = userAction(onBack)
                    ) {
                        Text(stringResource(Res.string.back_action))
                    }
                },
                actions = {
                    if (state.details != null) {
                        TextButton(
                            onClick = userAction(onEdit),
                            enabled = !state.isDeleting
                        ) {
                            Text(stringResource(Res.string.edit_action))
                        }
                    }
                }
            )
        },
        snackbarHost = {
            LocalizedErrorSnackbarHost(
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

            state.loadIssue != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(localizedVineyardLoadIssue(state.loadIssue))
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
                    item(key = VineyardDetailsItemKeys.INFORMATION, contentType = VineyardDetailsItemKeys.INFORMATION) {
                        VineyardInformationSection(
                            details = details
                        )
                    }

                    item(key = VineyardDetailsItemKeys.WEATHER, contentType = VineyardDetailsItemKeys.WEATHER) {
                        SectionTitle(stringResource(Res.string.weather_monitoring))
                        InfoHelp(title = stringResource(Res.string.weather_monitoring), text = stringResource(Res.string.weather_monitoring_intro))
                        TextButton(onClick = userAction(onWeather)) { Text(stringResource(Res.string.open_weather_monitoring)) }
                        InfoHelp(title = stringResource(Res.string.info_risk), text = stringResource(Res.string.risk_not_assessed))
                    }

                    item(key = VineyardDetailsItemKeys.BLOCKS_HEADING, contentType = VineyardDetailsItemKeys.BLOCKS_HEADING) {
                        TextButton(onClick=userAction(onActivities)) { Text(stringResource(Res.string.vineyard_activities)) }
                        SectionTitle(stringResource(Res.string.selector_blocks))
                        TextButton(onClick=userAction(onBlocks)) { Text(stringResource(Res.string.manage_blocks)) }
                    }

                    if (details.blocks.isEmpty()) {
                        item(key = VineyardDetailsItemKeys.BLOCKS_EMPTY, contentType = VineyardDetailsItemKeys.BLOCKS_EMPTY) {
                            EmptySectionText(
                                stringResource(Res.string.no_blocks_added)
                            )
                        }
                    } else {
                        items(
                            items = details.blocks,
                            key = { VineyardDetailsItemKeys.block(it.blockId) },
                            contentType = { "block-summary" }
                        ) { block ->
                            BlockSummaryCard(block)
                        }
                    }

                    item(key = VineyardDetailsItemKeys.DISEASES_HEADING, contentType = VineyardDetailsItemKeys.DISEASES_HEADING) {
                        SectionTitle(stringResource(Res.string.active_diseases_title))
                    }

                    if (details.activeDiseases.isEmpty()) {
                        item(key = VineyardDetailsItemKeys.DISEASES_EMPTY, contentType = VineyardDetailsItemKeys.DISEASES_EMPTY) {
                            EmptySectionText(
                                stringResource(Res.string.no_active_occurrences)
                            )
                        }
                    } else {
                        items(
                            items = details.activeDiseases,
                            key = { VineyardDetailsItemKeys.occurrence(it.occurrenceId) },
                            contentType = { "occurrence-summary" }
                        ) { disease ->
                            DiseaseSummaryCard(disease)
                        }
                    }

                    item(key = VineyardDetailsItemKeys.ALERTS_HEADING, contentType = VineyardDetailsItemKeys.ALERTS_HEADING) {
                        SectionTitle(stringResource(Res.string.active_alerts_title))
                    }

                    if (details.activeAlerts.isEmpty()) {
                        item(key = VineyardDetailsItemKeys.ALERTS_EMPTY, contentType = VineyardDetailsItemKeys.ALERTS_EMPTY) {
                            EmptySectionText(
                                stringResource(Res.string.no_active_alerts)
                            )
                        }
                    } else {
                        items(
                            items = details.activeAlerts,
                            key = { VineyardDetailsItemKeys.alert(it.alertId) },
                            contentType = { "alert-summary" }
                        ) { alert ->
                            AlertSummaryCard(alert, !state.isDeleting) { onOpenAlert(alert.alertId) }
                        }
                    }

                    item(key = VineyardDetailsItemKeys.DELETE, contentType = VineyardDetailsItemKeys.DELETE) {
                        OutlinedButton(
                            onClick = userAction(onDeleteRequest),
                            enabled = !state.isDeleting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(Res.string.delete_vineyard_action))
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
                    Text(stringResource(Res.string.delete_vineyard_title))
                },
                text = {
                    Text(
                        stringResource(Res.string.delete_vineyard_message)
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
                            Text(stringResource(Res.string.delete_record))
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = userAction(onDeleteCancel),
                        enabled = !state.isDeleting
                    ) {
                        Text(stringResource(Res.string.cancel_action))
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

            Text(stringResource(
                Res.string.size_value, formatPlainDecimal(details.size),
                details.sizeUnit?.symbol ?: stringResource(Res.string.unknown_unit)
            ))

            Text(
                stringResource(
                    Res.string.elevation_value,
                    details.elevation?.let { "${formatPlainDecimal(it)} m" } ?: stringResource(Res.string.unknown_value)
                )
            )

            Text(
                stringResource(Res.string.zone_value, details.timeZone)
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
                stringResource(Res.string.variety_value, block.vineVariety)
            )

            Text(
                stringResource(Res.string.area_value, block.area?.let {
                    "${formatPlainDecimal(it)} ${block.areaUnit?.symbol ?: stringResource(Res.string.unknown_unit)}"
                } ?: stringResource(Res.string.unknown_value))
            )

            Text(stringResource(Res.string.stage_value, localizedStage(block.phenologicalStage)))

            block.latestActivityType?.let {
                Text(
                    stringResource(Res.string.latest_activity_value, it)
                )
            }

            block.latestActivityStatus?.let {
                Text(
                    stringResource(Res.string.activity_status_value, localizedActivityStatus(it))
                )
            }

            Text(
                stringResource(Res.string.active_disease_count, block.activeDiseaseCount)
            )

            Text(
                stringResource(Res.string.active_alert_count, block.activeAlertCount)
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
                        ?.let { stringResource(Res.string.block_value, it) }
                        ?: stringResource(Res.string.whole_vineyard)
            )

            Text(
                stringResource(Res.string.severity_value, "${disease.severity}%")
            )

            Text(
                stringResource(Res.string.observed_value, disease.observedAt.toString())
            )

            Text(
                stringResource(Res.string.status_value, localizedOccurrenceStatus(disease.status))
            )
        }
    }
}

@Composable
private fun AlertSummaryCard(
    alert: AlertUiModel,
    enabled: Boolean,
    onOpen: () -> Unit
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
                    ?.let { stringResource(Res.string.block_value, it) }
                    ?: stringResource(Res.string.whole_vineyard)
            )

            Text(
                stringResource(Res.string.severity_value, localizedAlertSeverity(alert.severity))
            )

            Text(
                stringResource(Res.string.stage_value, localizedStage(alert.phenologicalStage))
            )

            Text(
                stringResource(Res.string.created_value, alert.createdAt.toString())
            )
            TextButton(onClick = userAction(onOpen), enabled = enabled) { Text(stringResource(Res.string.alerts_open)) }
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

