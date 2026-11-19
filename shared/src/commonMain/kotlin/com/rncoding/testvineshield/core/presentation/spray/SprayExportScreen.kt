package com.rncoding.testvineshield.core.presentation.spray

import androidx.compose.foundation.layout.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rncoding.testvineshield.core.domain.export.DocumentExportFormat
import com.rncoding.testvineshield.core.domain.usecases.activity.SprayKey
import com.rncoding.testvineshield.core.presentation.components.*
import com.rncoding.testvineshield.core.presentation.export.rememberDocumentSaver
import com.rncoding.testvineshield.core.presentation.root.userAction
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SprayExportScreenRoot(ownerId: Long, key: SprayKey, onBack: () -> Unit, onBusyChanged: (Boolean) -> Unit = {}) {
    val vm = koinViewModel<SprayExportViewModel>(key = "spray-export-$ownerId-${key.vineyardId}-${key.activityId}", parameters = { parametersOf(key) })
    val state by vm.state.collectAsStateWithLifecycle()
    val saver = rememberDocumentSaver()
    val formatLabels = mapOf(DocumentExportFormat.JSON to stringResource(Res.string.export_json_exact),
        DocumentExportFormat.CSV to stringResource(Res.string.export_csv_view))
    var formatCode by rememberSaveable(ownerId, key.vineyardId, key.activityId) { mutableStateOf("JSON") }
    val format = DocumentExportFormat.entries.firstOrNull { it.name == formatCode } ?: DocumentExportFormat.JSON
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.load() }
    DisposableEffect(vm) { onDispose { vm.cancelExport() } }
    DisposableEffect(state.busy) { onBusyChanged(state.busy); onDispose { onBusyChanged(false) } }
    Scaffold(topBar = { RecordTopBar(stringResource(Res.string.spray_export_saved), { if (!vm.state.value.busy) onBack() }, !state.busy) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading || state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.snapshot?.let { snapshot ->
                item { Text(snapshot.activity.activityType, style = MaterialTheme.typography.headlineSmall); Text(stringResource(Res.string.spray_season, snapshot.activity.season)) }
                item { Text(stringResource(Res.string.spray_export_summary, snapshot.record?.products?.size ?: 0)) }
                item { RecordChoice(stringResource(Res.string.export_file_format), format, DocumentExportFormat.entries, !state.busy, { formatLabels.getValue(it) }, { it?.let { choice -> formatCode = choice.name } }, allowUnknown = false) }
                item { Text(stringResource(Res.string.spray_export_contents)) }
                item { Text(stringResource(Res.string.spray_export_plaintext)) }
                if (format == DocumentExportFormat.CSV) item { Text(stringResource(Res.string.spray_export_csv_help)) }
                item { Button(onClick = userAction { vm.export(format, saver); onBusyChanged(vm.state.value.busy) }, enabled = !state.loading && !state.busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.export_choose_save)) } }
            }
            state.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); if (state.snapshot == null) TextButton(onClick = userAction(vm::load), enabled = !state.busy) { Text(stringResource(Res.string.retry)) } } }
            if (state.noSavedApplication) item {
                Text(stringResource(Res.string.spray_export_missing), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = userAction(vm::load), enabled = !state.busy) { Text(stringResource(Res.string.retry)) }
            }
            state.notice?.let { notice -> item { Text(when (notice) {
                is SprayExportNotice.Saved -> stringResource(Res.string.spray_export_success, notice.productCount)
                SprayExportNotice.Cancelled -> stringResource(Res.string.export_cancelled)
                SprayExportNotice.Interrupted -> stringResource(Res.string.export_interrupted)
            }) } }
            if (state.busy) item { OutlinedButton(onClick = userAction(vm::cancelExport)) { Text(stringResource(Res.string.export_cancel)) } }
        }
    }
}
