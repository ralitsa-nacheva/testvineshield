package com.rncoding.testvineshield.core.presentation.harvest

import androidx.compose.foundation.layout.*
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
    var season by rememberSaveable(ownerId, scope.vineyardId, scope.blockId, initialSeason) { mutableStateOf(initialSeason) }
    var formatCode by rememberSaveable(ownerId, scope.vineyardId, scope.blockId) { mutableStateOf("JSON") }
    val format = HarvestExportFormat.entries.firstOrNull { it.name == formatCode } ?: HarvestExportFormat.JSON
    DisposableEffect(vm) { onDispose { vm.cancel() } }
    DisposableEffect(state.busy) {
        onBusyChanged(state.busy)
        onDispose { onBusyChanged(false) }
    }
    LaunchedEffect(list.error) { if (list.error != null) vm.cancel() }
    Scaffold(topBar = { RecordTopBar("Export harvest records", { if (!vm.state.value.busy) onBack() }, !state.busy) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (list.loading || state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            list.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); TextButton(onClick = userAction(records::reload), enabled = !state.busy) { Text("Retry") } } }
            list.snapshot?.let { snapshot ->
                val count = snapshot.records.count { season == null || it.season == season }
                item { Text(snapshot.blockName, style = MaterialTheme.typography.headlineSmall) }
                item { RecordChoice("Season scope", season, snapshot.records.map { it.season }.distinct().sortedDescending(), !state.busy, { it }, { season = it }, nullLabel = "All seasons") }
                item { Text("$count records currently in this scope. Saving reloads the records; the file is a snapshot at generation time.") }
                item { RecordChoice("File format", format, HarvestExportFormat.entries, !state.busy, { if (it == HarvestExportFormat.JSON) "JSON — exact record text" else "CSV — spreadsheet view" }, { it?.let { choice -> formatCode = choice.name } }, allowUnknown = false) }
                item { Text("Includes vineyard/block names, local record IDs, season, dates, quantities, units and Brix status. Unknown Brix stays unknown; different units are not totalled.") }
                item { Text("Files are not encrypted. Choose a local folder for an offline copy; your system may also offer cloud storage providers. This exports harvest records, not an app backup or regulatory report.") }
                if (format == HarvestExportFormat.CSV) item { Text("CSV uses comma-separated columns, UTF-8 and decimal dots. Formula-like text receives a visible protective prefix; spreadsheet apps differ. Use JSON when exact text matters.") }
                item { Button(onClick = userAction { vm.export(scope, season, format, saver); onBusyChanged(vm.state.value.busy) }, enabled = !list.loading && count > 0 && !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Choose location and save") } }
                if (count == 0) item { Text("No records to export in this scope.") }
            }
            state.message?.let { item { Text(it) } }
            state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (state.busy) item { OutlinedButton(onClick = userAction(vm::cancel), modifier = Modifier.fillMaxWidth()) { Text("Cancel export") } }
        }
    }
}
