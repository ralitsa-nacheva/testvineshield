package com.rncoding.testvineshield.core.presentation.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rncoding.testvineshield.core.domain.datamodels.DiseaseAlertInboxItem
import com.rncoding.testvineshield.core.domain.datamodels.enums.AlertStatus
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseCatalogueCodes
import com.rncoding.testvineshield.core.presentation.components.RecordTopBar
import com.rncoding.testvineshield.core.presentation.disease_observations.builtInDiseaseName
import com.rncoding.testvineshield.core.presentation.error.localizedAppErrorMessage
import com.rncoding.testvineshield.core.presentation.root.userAction
import com.rncoding.testvineshield.core.presentation.vineyard_details.localizedAlertSeverity
import com.rncoding.testvineshield.core.presentation.vineyard_details.localizedStage
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DiseaseAlertsScreenRoot(
    model: DiseaseAlertsViewModel,
    vineyardId: Long? = null,
    alertId: Long? = null,
    onBack: (() -> Unit)? = null,
    onOpenAlert: (Long, Long) -> Unit = { _, _ -> },
    onOpenVineyard: (Long) -> Unit = {},
    onOpenBlock: (Long, Long) -> Unit = { _, _ -> },
    onOpenCatalogue: (Long) -> Unit = {},
    onBusyChanged: (Boolean) -> Unit = {}
) {
    val state by model.state.collectAsStateWithLifecycle()
    var activeOnly by rememberSaveable { mutableStateOf(true) }
    val busy = state.savingId != null
    DisposableEffect(busy) { onBusyChanged(busy); onDispose { onBusyChanged(false) } }
    Scaffold(topBar = { RecordTopBar(stringResource(Res.string.alerts_title), onBack, !busy) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max = 840.dp).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "scope") { Text(stringResource(Res.string.alerts_scope)) }
            if (state.loading) item(key = "loading") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error -> item(key = "error") {
                Text(localizedAppErrorMessage(error), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = userAction(model::reload), enabled = !busy) { Text(stringResource(Res.string.retry)) }
            } }
            if (!state.loading && alertId == null) {
                item(key = "filter") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(activeOnly, userAction { activeOnly = true }, label = { Text(stringResource(Res.string.alerts_active)) }, enabled = !busy)
                        FilterChip(!activeOnly, userAction { activeOnly = false }, label = { Text(stringResource(Res.string.alerts_history)) }, enabled = !busy)
                    }
                }
                val rows = state.rows.filter { (!activeOnly || it.status == AlertStatus.ACTIVE) && (vineyardId == null || it.vineyardId == vineyardId) }
                if (rows.isEmpty() && state.error == null) item(key = "empty") { Text(stringResource(Res.string.alerts_empty)) }
                items(rows, key = { it.alertId }, contentType = { "saved-alert" }) { row ->
                    DiseaseAlertCard(row) {
                        TextButton(onClick = userAction { onOpenAlert(row.vineyardId, row.alertId) }, enabled = !busy) { Text(stringResource(Res.string.alerts_open)) }
                    }
                }
            } else if (!state.loading && alertId != null) {
                val row = state.rows.firstOrNull { it.alertId == alertId && it.vineyardId == vineyardId }
                if (row == null && state.error == null) item(key = "missing") { Text(stringResource(Res.string.alerts_missing)) }
                row?.let { alert ->
                    item(key = "detail") { DiseaseAlertCard(alert) {} }
                    item(key = "provenance") { Text(stringResource(Res.string.alerts_provenance)) }
                    if (alert.status == AlertStatus.ACTIVE) item(key = "review") {
                        Text(stringResource(Res.string.alerts_review_help))
                        Button(onClick = userAction { model.markReviewed(alert); onBusyChanged(model.state.value.savingId != null) }, enabled = !busy) {
                            Text(stringResource(Res.string.alerts_review))
                        }
                    }
                    item(key = "vineyard") { OutlinedButton(onClick = userAction { onOpenVineyard(alert.vineyardId) }, enabled = !busy) { Text(stringResource(Res.string.alerts_vineyard)) } }
                    alert.blockId?.let { block -> item(key = "block") { OutlinedButton(onClick = userAction { onOpenBlock(alert.vineyardId, block) }, enabled = !busy) { Text(stringResource(Res.string.alerts_block)) } } }
                    if (alert.diseaseCode in setOf(DiseaseCatalogueCodes.GRAPE_DOWNY_MILDEW, DiseaseCatalogueCodes.GRAPE_POWDERY_MILDEW, DiseaseCatalogueCodes.GRAPE_BOTRYTIS)) {
                        item(key = "catalogue") { OutlinedButton(onClick = userAction { onOpenCatalogue(alert.diseaseId) }, enabled = !busy) { Text(stringResource(Res.string.alerts_catalogue)) } }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DiseaseAlertCard(alert: DiseaseAlertInboxItem, actions: @Composable () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Decorative next to a full text label; do not communicate severity by color alone.
                Icon(painterResource(Res.drawable.ic_disease_alert), contentDescription = null)
                Text(builtInDiseaseName(alert.diseaseCode, alert.diseaseName), style = MaterialTheme.typography.titleMedium)
            }
            Text(alert.vineyardName)
            Text(alert.blockName ?: stringResource(Res.string.alerts_block_unspecified))
            Text(stringResource(Res.string.alerts_created, alert.createdAt.toString()))
            Text(localizedAlertSeverity(alert.severity))
            Text(localizedAlertStatus(alert.status))
            Text(stringResource(Res.string.stage_value, localizedStage(alert.stage)))
            // Legacy saved prose has no reason code. Preserve it as recorded rather than guessing a translation.
            Text(alert.message)
            actions()
        }
    }
}

@Composable
internal fun localizedAlertStatus(status: AlertStatus): String = stringResource(when (status) {
    AlertStatus.ACTIVE -> Res.string.alerts_status_active
    AlertStatus.ACKNOWLEDGED -> Res.string.alerts_status_acknowledged
    AlertStatus.INVALIDATED -> Res.string.alerts_status_invalidated
    AlertStatus.RESOLVED -> Res.string.alerts_status_resolved
})
