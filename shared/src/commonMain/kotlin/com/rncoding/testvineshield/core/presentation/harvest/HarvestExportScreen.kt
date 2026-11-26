package com.rncoding.testvineshield.core.presentation.harvest

import com.rncoding.testvineshield.core.presentation.components.InfoHelp
import androidx.compose.foundation.layout.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rncoding.testvineshield.core.domain.export.HarvestExportFormat
import com.rncoding.testvineshield.core.domain.usecases.harvest.HarvestScope
import com.rncoding.testvineshield.core.presentation.components.RecordChoice
import com.rncoding.testvineshield.core.presentation.components.RecordTopBar
import com.rncoding.testvineshield.core.presentation.export.rememberDocumentSaver
import com.rncoding.testvineshield.core.presentation.root.userAction
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun HarvestExportScreenRoot(ownerId: Long, scope: HarvestScope, initialSeason: String?, onBack: () -> Unit, onBusyChanged: (Boolean) -> Unit = {}) {
    val records = koinViewModel<HarvestListViewModel>(key = "harvest-export-preview-$ownerId-${scope.vineyardId}-${scope.blockId}", parameters = { parametersOf(scope) })
    val list by records.state.collectAsStateWithLifecycle()
    val vm = koinViewModel<HarvestExportViewModel>(key = "harvest-export-$ownerId-${scope.vineyardId}-${scope.blockId}")
    val state by vm.state.collectAsStateWithLifecycle()
    val saver = rememberDocumentSaver()
    val formatLabels = mapOf(HarvestExportFormat.JSON to stringResource(Res.string.harvest_export_json_exact),
        HarvestExportFormat.CSV to stringResource(Res.string.export_csv_view))
    var season by rememberSaveable(ownerId, scope.vineyardId, scope.blockId, initialSeason) { mutableStateOf(initialSeason) }
    var formatCode by rememberSaveable(ownerId, scope.vineyardId, scope.blockId) { mutableStateOf("JSON") }
    val format = HarvestExportFormat.entries.firstOrNull { it.name == formatCode } ?: HarvestExportFormat.JSON
    DisposableEffect(vm) { onDispose { vm.cancel() } }
    DisposableEffect(state.busy) {
        onBusyChanged(state.busy)
        onDispose { onBusyChanged(false) }
    }
    LaunchedEffect(list.error) { if (list.error != null) vm.cancel() }
    Scaffold(topBar = { RecordTopBar(stringResource(Res.string.harvest_export_records), { if (!vm.state.value.busy) onBack() }, !state.busy) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (list.loading || state.busy) item(key = "harvest-HarvestExportScreen-1", contentType = "harvest-HarvestExportScreen-1") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            list.error?.let { error -> item(key = "harvest-HarvestExportScreen-2", contentType = "harvest-HarvestExportScreen-2") { Text(localizedHarvestIssue(error), color = MaterialTheme.colorScheme.error); TextButton(onClick = userAction(records::reload), enabled = !state.busy) { Text(stringResource(Res.string.retry)) } } }
            list.snapshot?.let { snapshot ->
                val count = snapshot.records.count { season == null || it.season == season }
                item(key = "harvest-HarvestExportScreen-3", contentType = "harvest-HarvestExportScreen-3") { Text(snapshot.blockName, style = MaterialTheme.typography.headlineSmall) }
                item(key = "harvest-HarvestExportScreen-4", contentType = "harvest-HarvestExportScreen-4") { RecordChoice(stringResource(Res.string.harvest_export_season_scope), season, snapshot.records.map { it.season }.distinct().sortedDescending(), !state.busy, { it }, { season = it }, nullLabel = stringResource(Res.string.all_seasons)) }
                item(key = "harvest-HarvestExportScreen-5", contentType = "harvest-HarvestExportScreen-5") { InfoHelp(title = stringResource(Res.string.info_harvest_count, count), text = stringResource(Res.string.harvest_export_summary, count)) }
                item(key = "harvest-HarvestExportScreen-6", contentType = "harvest-HarvestExportScreen-6") { RecordChoice(stringResource(Res.string.export_file_format), format, HarvestExportFormat.entries, !state.busy, { formatLabels.getValue(it) }, { it?.let { choice -> formatCode = choice.name } }, allowUnknown = false) }
                item(key = "harvest-HarvestExportScreen-7", contentType = "harvest-HarvestExportScreen-7") { InfoHelp(title = stringResource(Res.string.info_export_contents), text = stringResource(Res.string.harvest_export_contents)) }
                item(key = "harvest-HarvestExportScreen-8", contentType = "harvest-HarvestExportScreen-8") { InfoHelp(title = stringResource(Res.string.info_export_storage), text = stringResource(Res.string.harvest_export_plaintext)) }
                if (format == HarvestExportFormat.CSV) item(key = "harvest-HarvestExportScreen-9", contentType = "harvest-HarvestExportScreen-9") { InfoHelp(title = stringResource(Res.string.export_file_format), text = stringResource(Res.string.harvest_export_csv_help)) }
                item(key = "harvest-HarvestExportScreen-10", contentType = "harvest-HarvestExportScreen-10") { Button(onClick = userAction { vm.export(scope, season, format, saver); onBusyChanged(vm.state.value.busy) }, enabled = !list.loading && count > 0 && !state.busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.export_choose_save)) } }
                if (count == 0) item(key = "harvest-HarvestExportScreen-11", contentType = "harvest-HarvestExportScreen-11") { Text(stringResource(Res.string.harvest_export_empty)) }
            }
            state.notice?.let { notice -> item(key = "harvest-HarvestExportScreen-12", contentType = "harvest-HarvestExportScreen-12") { Text(when (notice) {
                is HarvestExportNotice.Saved -> stringResource(Res.string.harvest_export_success, notice.recordCount)
                HarvestExportNotice.Cancelled -> stringResource(Res.string.export_cancelled)
                HarvestExportNotice.Interrupted -> stringResource(Res.string.export_interrupted)
            }) } }
            state.error?.let { item(key = "harvest-HarvestExportScreen-13", contentType = "harvest-HarvestExportScreen-13") { Text(localizedHarvestIssue(it), color = MaterialTheme.colorScheme.error) } }
            if (state.busy) item(key = "harvest-HarvestExportScreen-14", contentType = "harvest-HarvestExportScreen-14") { OutlinedButton(onClick = userAction(vm::cancel), modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.export_cancel)) } }
        }
    }
}
