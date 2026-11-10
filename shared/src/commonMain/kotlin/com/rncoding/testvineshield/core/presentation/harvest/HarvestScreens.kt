package com.rncoding.testvineshield.core.presentation.harvest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rncoding.testvineshield.core.domain.usecases.harvest.HarvestScope
import com.rncoding.testvineshield.core.presentation.components.RecordChoice
import com.rncoding.testvineshield.core.presentation.components.RecordTopBar
import com.rncoding.testvineshield.core.presentation.root.LocalUserActivity
import com.rncoding.testvineshield.core.presentation.root.userAction
import com.rncoding.testvineshield.core.presentation.root.userActivity
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun HarvestListScreenRoot(ownerId: Long, scope: HarvestScope, onBack: () -> Unit, onCreate: () -> Unit, onOpen: (Long) -> Unit) {
    val vm = koinViewModel<HarvestListViewModel>(key = "harvests-$ownerId-${scope.vineyardId}-${scope.blockId}", parameters = { parametersOf(scope) })
    val state by vm.state.collectAsStateWithLifecycle()
    var season by rememberSaveable(ownerId, scope.vineyardId, scope.blockId) { mutableStateOf<String?>(null) }
    Scaffold(topBar = { RecordTopBar("Harvest records", onBack) }, floatingActionButton = {
        if (!state.loading && state.snapshot != null) ExtendedFloatingActionButton(onClick = userAction(onCreate)) { Text("Add harvest") }
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { item { HarvestErrorNotice(it, vm::reload) } }
            state.snapshot?.let { snapshot ->
                item { Text(snapshot.blockName, style = MaterialTheme.typography.headlineSmall) }
                item { RecordChoice("Season", season, snapshot.records.map { it.season }.distinct().sortedDescending(), true, { it }, { season = it }, nullLabel = "All seasons") }
                val visible = snapshot.records.filter { season == null || it.season == season }
                if (visible.isEmpty()) item { Text(if (snapshot.records.isEmpty()) "No harvest records yet. Record a harvest for this block." else "No records for the selected season. Choose All seasons to see every record.") }
                items(visible, key = { it.harvestId }) { record ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(record.harvestDate.toString(), style = MaterialTheme.typography.titleMedium)
                            Text("Season: ${record.season}")
                            Text("Quantity: ${harvestNumberText(record.quantity)} ${record.units.ifBlank { "(unit unknown)" }}")
                            Text("Brix: ${record.brixSugarContent?.let(::harvestNumberText) ?: "Not measured / not recorded"}")
                            TextButton(onClick = userAction { onOpen(record.harvestId) }) { Text("Open harvest") }
                        }
                    }
                }
                item { Text("Quantities are shown in their recorded units. No total combines different units.") }
            }
        }
    }
}

@Composable
fun HarvestDetailsScreenRoot(ownerId: Long, key: HarvestKey, onBack: () -> Unit, onEdit: () -> Unit, onDeleted: () -> Unit, onBusyChanged: (Boolean) -> Unit = {}) {
    val vm = koinViewModel<HarvestDetailsViewModel>(key = "harvest-$ownerId-${key.scope.vineyardId}-${key.scope.blockId}-${key.harvestId}", parameters = { parametersOf(key) })
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.load() }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }
    DisposableEffect(state.deleting) {
        onBusyChanged(state.deleting)
        onDispose { onBusyChanged(false) }
    }
    Scaffold(topBar = { RecordTopBar("Harvest details", { if (!vm.state.value.deleting) onBack() }, !state.deleting) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading || state.deleting) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { item { HarvestErrorNotice(it, if (state.deleting) null else vm::load) } }
            state.record?.let { record ->
                item { Text(record.harvestDate.toString(), style = MaterialTheme.typography.headlineSmall) }
                item { Text("Season: ${record.season}") }
                item { Text("Quantity: ${harvestNumberText(record.quantity)} ${record.units.ifBlank { "(unit unknown)" }}") }
                item { Text("Brix: ${record.brixSugarContent?.let(::harvestNumberText) ?: "Not measured / not recorded"}") }
                item { Button(onClick = userAction(onEdit), enabled = !state.deleting, modifier = Modifier.fillMaxWidth()) { Text("Edit harvest") } }
                item { OutlinedButton(onClick = userAction(vm::requestDelete), enabled = !state.deleting, modifier = Modifier.fillMaxWidth()) { Text("Delete harvest") } }
            }
        }
    }
    if (state.confirmDelete) {
        val activity = LocalUserActivity.current
        AlertDialog(modifier = Modifier.userActivity(activity), onDismissRequest = userAction(vm::cancelDelete), title = { Text("Delete harvest?") }, text = { Text("This harvest record will be permanently deleted. This cannot be undone.") }, confirmButton = {
            TextButton(onClick = userAction { vm.confirmDelete(); onBusyChanged(vm.state.value.deleting) }, enabled = !state.deleting) { Text("Delete permanently") }
        }, dismissButton = { TextButton(onClick = userAction(vm::cancelDelete), enabled = !state.deleting) { Text("Cancel") } })
    }
}

private val harvestDraftSaver = listSaver<ArrayList<String>, String>(save = { it.toList() }, restore = { ArrayList(it) })

@Composable
fun HarvestEditorScreenRoot(ownerId: Long, key: HarvestKey, onBack: () -> Unit, onSaved: () -> Unit, onBusyChanged: (Boolean) -> Unit = {}) {
    var savedDraft by rememberSaveable(ownerId, key.scope.vineyardId, key.scope.blockId, key.harvestId, stateSaver = harvestDraftSaver) { mutableStateOf(arrayListOf<String>()) }
    val vm = koinViewModel<HarvestEditorViewModel>(key = "harvest-editor-$ownerId-${key.scope.vineyardId}-${key.scope.blockId}-${key.harvestId}", parameters = { parametersOf(HarvestEditorRequest(key, HarvestDraft.restore(savedDraft))) })
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.draft, state.ready, state.saved) {
        if (state.saved) { savedDraft = arrayListOf(); onSaved() }
        else if (state.ready) savedDraft = state.draft.savedValues()
    }
    DisposableEffect(state.saving) {
        onBusyChanged(state.saving)
        onDispose { onBusyChanged(false) }
    }
    val activity = LocalUserActivity.current
    Scaffold(topBar = { RecordTopBar(if (key.harvestId == 0L) "New harvest" else "Edit harvest", { if (!vm.state.value.saving) onBack() }, !state.saving) }) { padding ->
        LazyColumn(Modifier.padding(padding).imePadding().fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading || state.saving) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { item { HarvestErrorNotice(it, if (state.ready) null else vm::load) } }
            item { Text(state.blockName, style = MaterialTheme.typography.titleMedium) }
            item { Text("Save to keep changes. Back discards unsaved changes. Leave Brix blank when it was not measured.") }
            items(HarvestField.entries, key = { it.name }) { field ->
                val (label, help, keyboard) = when (field) {
                    HarvestField.Season -> Triple("Season *", "Explicit season, e.g. 2026 or 2026/2027", KeyboardType.Text)
                    HarvestField.Date -> Triple("Harvest date *", "Actual calendar date: YYYY-MM-DD", KeyboardType.Ascii)
                    HarvestField.Quantity -> Triple("Quantity *", "Positive amount; one dot or comma decimal separator; no thousands grouping", KeyboardType.Decimal)
                    HarvestField.Brix -> Triple("Brix (°Bx)", "Optional measured value, 0–100; blank means not recorded", KeyboardType.Decimal)
                }
                OutlinedTextField(value = state.draft.text(field), onValueChange = { activity(); vm.text(field, it) }, enabled = state.canEdit, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, isError = field in state.errors, supportingText = { Text(state.errors[field] ?: help) }, keyboardOptions = KeyboardOptions(keyboardType = keyboard))
                if (field == HarvestField.Quantity) {
                    // Keep arbitrary legacy unit labels visible and selectable without interpreting them.
                    val units = (HarvestDraft.commonUnits + listOfNotNull(state.legacyUnit, state.draft.units)).distinct()
                    RecordChoice("Quantity unit *", state.draft.units, units, state.canEdit,
                        { when (it) { "t" -> "t (tonne)"; "lb" -> "lb (pound)"; else -> it } }, vm::units, nullLabel = "Select a unit")
                    state.unitError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            item { Text("Changing the unit only changes its label; it does not convert the number. Existing other unit labels are retained. Zero Brix is a recorded value, not an alias for unknown.") }
            item { Button(onClick = userAction { vm.save(); onBusyChanged(vm.state.value.saving) }, enabled = state.canEdit, modifier = Modifier.fillMaxWidth()) { Text(if (state.saving) "Saving…" else "Save harvest") } }
        }
    }
}

@Composable
private fun HarvestErrorNotice(message: String, retry: (() -> Unit)?) {
    Column {
        Text(message, color = MaterialTheme.colorScheme.error)
        retry?.let { TextButton(onClick = userAction(it)) { Text("Retry") } }
    }
}
