package com.rncoding.testvineshield.core.presentation.spray

import androidx.compose.foundation.layout.*
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
    var formatCode by rememberSaveable(ownerId, key.vineyardId, key.activityId) { mutableStateOf("JSON") }
    val format = DocumentExportFormat.entries.firstOrNull { it.name == formatCode } ?: DocumentExportFormat.JSON
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.load() }
    DisposableEffect(vm) { onDispose { vm.cancelExport() } }
    DisposableEffect(state.busy) { onBusyChanged(state.busy); onDispose { onBusyChanged(false) } }
    Scaffold(topBar = { RecordTopBar("Export saved spray application", { if (!vm.state.value.busy) onBack() }, !state.busy) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading || state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.snapshot?.let { snapshot ->
                item { Text(snapshot.activity.activityType, style = MaterialTheme.typography.headlineSmall); Text("Season: ${snapshot.activity.season}") }
                item { Text("One saved application, ${snapshot.record?.products?.size ?: 0} product lines. Export reloads saved data; unsaved edits are not included.") }
                item { RecordChoice("File format", format, DocumentExportFormat.entries, !state.busy, { if (it == DocumentExportFormat.JSON) "JSON — exact text" else "CSV — spreadsheet view" }, { it?.let { choice -> formatCode = choice.name } }, allowUnknown = false) }
                item { Text("Includes application date/local time/zone, area/carrier water, products, recorded rate/basis, references and notes. Old single-product activity fields are excluded and their presence is flagged. This is not a backup, label recommendation or compliant reporting template.") }
                item { Text("Files are plaintext. Choose a local folder for offline output; the system may also offer cloud providers. JSON preserves exact strings. CSV prefixes formula-like text for spreadsheet viewing; spreadsheet/re-save behavior varies.") }
                if (format == DocumentExportFormat.CSV) item { Text("CSV has one row per product. Application fields repeat on each row: do not sum repeated area or water values. Use UTF-8/comma delimiter and decimal dots when importing.") }
                item { Button(onClick = userAction { vm.export(format, saver); onBusyChanged(vm.state.value.busy) }, enabled = !state.loading && !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Choose location and save") } }
            }
            state.error?.let { error -> item { Text(error, color = MaterialTheme.colorScheme.error); if (state.snapshot == null) TextButton(onClick = userAction(vm::load), enabled = !state.busy) { Text("Retry") } } }
            state.message?.let { item { Text(it) } }
            if (state.busy) item { OutlinedButton(onClick = userAction(vm::cancelExport)) { Text("Cancel export") } }
        }
    }
}
