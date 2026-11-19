package com.rncoding.testvineshield.core.presentation.harvest

import androidx.compose.foundation.layout.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
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
fun HarvestListScreenRoot(ownerId: Long, scope: HarvestScope, onBack: () -> Unit, onCreate: () -> Unit, onOpen: (Long) -> Unit, onExport: (String?) -> Unit = {}) {
    val vm = koinViewModel<HarvestListViewModel>(key = "harvests-$ownerId-${scope.vineyardId}-${scope.blockId}", parameters = { parametersOf(scope) })
    val state by vm.state.collectAsStateWithLifecycle()
    var season by rememberSaveable(ownerId, scope.vineyardId, scope.blockId) { mutableStateOf<String?>(null) }
    Scaffold(topBar = { RecordTopBar(stringResource(Res.string.harvest_records), onBack) }, floatingActionButton = {
        if (!state.loading && state.snapshot != null) ExtendedFloatingActionButton(onClick = userAction(onCreate)) { Text(stringResource(Res.string.harvest_add)) }
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { item { HarvestErrorNotice(it, vm::reload) } }
            state.snapshot?.let { snapshot ->
                item { Text(snapshot.blockName, style = MaterialTheme.typography.headlineSmall) }
                item { OutlinedButton(onClick = userAction { onExport(season) }, enabled = snapshot.records.any { season == null || it.season == season }) { Text(stringResource(Res.string.harvest_export_records)) } }
                item { RecordChoice(stringResource(Res.string.season_filter_label), season, snapshot.records.map { it.season }.distinct().sortedDescending(), true, { it }, { season = it }, nullLabel = stringResource(Res.string.all_seasons)) }
                val visible = snapshot.records.filter { season == null || it.season == season }
                if (visible.isEmpty()) item { Text(if (snapshot.records.isEmpty()) stringResource(Res.string.harvest_empty) else stringResource(Res.string.harvest_season_empty)) }
                items(visible, key = { it.harvestId }) { record ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(record.harvestDate.toString(), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(Res.string.activity_season, record.season))
                            Text(stringResource(Res.string.harvest_quantity_value, harvestNumberText(record.quantity), record.units.ifBlank { stringResource(Res.string.unknown_unit) }))
                            Text(stringResource(Res.string.harvest_brix_value, record.brixSugarContent?.let(::harvestNumberText) ?: stringResource(Res.string.harvest_not_measured)))
                            TextButton(onClick = userAction { onOpen(record.harvestId) }) { Text(stringResource(Res.string.harvest_open)) }
                        }
                    }
                }
                item { Text(stringResource(Res.string.harvest_units_help)) }
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
    Scaffold(topBar = { RecordTopBar(stringResource(Res.string.harvest_details), { if (!vm.state.value.deleting) onBack() }, !state.deleting) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading || state.deleting) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { item { HarvestErrorNotice(it, if (state.deleting) null else vm::load) } }
            state.record?.let { record ->
                item { Text(record.harvestDate.toString(), style = MaterialTheme.typography.headlineSmall) }
                item { Text(stringResource(Res.string.activity_season, record.season)) }
                item { Text(stringResource(Res.string.harvest_quantity_value, harvestNumberText(record.quantity), record.units.ifBlank { stringResource(Res.string.unknown_unit) })) }
                item { Text(stringResource(Res.string.harvest_brix_value, record.brixSugarContent?.let(::harvestNumberText) ?: stringResource(Res.string.harvest_not_measured))) }
                item { Button(onClick = userAction(onEdit), enabled = !state.deleting, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.harvest_edit)) } }
                item { OutlinedButton(onClick = userAction(vm::requestDelete), enabled = !state.deleting, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.harvest_delete)) } }
            }
        }
    }
    if (state.confirmDelete) {
        val activity = LocalUserActivity.current
        AlertDialog(modifier = Modifier.userActivity(activity), onDismissRequest = userAction(vm::cancelDelete), title = { Text(stringResource(Res.string.harvest_delete_title)) }, text = { Text(stringResource(Res.string.harvest_delete_warning)) }, confirmButton = {
            TextButton(onClick = userAction { vm.confirmDelete(); onBusyChanged(vm.state.value.deleting) }, enabled = !state.deleting) { Text(stringResource(Res.string.delete_record)) }
        }, dismissButton = { TextButton(onClick = userAction(vm::cancelDelete), enabled = !state.deleting) { Text(stringResource(Res.string.cancel_action)) } })
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
    val tonneLabel = stringResource(Res.string.unit_tonne)
    val poundLabel = stringResource(Res.string.unit_pound)
    Scaffold(topBar = { RecordTopBar(if (key.harvestId == 0L) stringResource(Res.string.harvest_new) else stringResource(Res.string.harvest_edit), { if (!vm.state.value.saving) onBack() }, !state.saving) }) { padding ->
        LazyColumn(Modifier.padding(padding).imePadding().fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading || state.saving) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { item { HarvestErrorNotice(it, if (state.ready) null else vm::load) } }
            item { Text(state.blockName, style = MaterialTheme.typography.titleMedium) }
            item { Text(stringResource(Res.string.harvest_save_help)) }
            items(HarvestField.entries, key = { it.name }) { field ->
                val (label, help, keyboard) = when (field) {
                    HarvestField.Season -> Triple(stringResource(Res.string.harvest_season_required), stringResource(Res.string.harvest_season_help), KeyboardType.Text)
                    HarvestField.Date -> Triple(stringResource(Res.string.harvest_date_required), stringResource(Res.string.harvest_date_help), KeyboardType.Ascii)
                    HarvestField.Quantity -> Triple(stringResource(Res.string.harvest_quantity_required), stringResource(Res.string.harvest_quantity_help), KeyboardType.Decimal)
                    HarvestField.Brix -> Triple(stringResource(Res.string.harvest_brix_label), stringResource(Res.string.harvest_brix_help), KeyboardType.Decimal)
                }
                OutlinedTextField(value = state.draft.text(field), onValueChange = { activity(); vm.text(field, it) }, enabled = state.canEdit, modifier = Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, isError = field in state.errors, supportingText = { Text(state.errors[field] ?: help) }, keyboardOptions = KeyboardOptions(keyboardType = keyboard))
                if (field == HarvestField.Quantity) {
                    // Keep arbitrary legacy unit labels visible and selectable without interpreting them.
                    val units = (HarvestDraft.commonUnits + listOfNotNull(state.legacyUnit, state.draft.units)).distinct()
                    RecordChoice(stringResource(Res.string.harvest_unit_required), state.draft.units, units, state.canEdit,
                        { when (it) { "t" -> tonneLabel; "lb" -> poundLabel; else -> it } }, vm::units, nullLabel = stringResource(Res.string.select_unit))
                    state.unitError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
            item { Text(stringResource(Res.string.harvest_unit_change_help)) }
            item { Button(onClick = userAction { vm.save(); onBusyChanged(vm.state.value.saving) }, enabled = state.canEdit, modifier = Modifier.fillMaxWidth()) { Text(if (state.saving) stringResource(Res.string.saving_action) else stringResource(Res.string.harvest_save)) } }
        }
    }
}

@Composable
private fun HarvestErrorNotice(message: String, retry: (() -> Unit)?) {
    Column {
        Text(message, color = MaterialTheme.colorScheme.error)
        retry?.let { TextButton(onClick = userAction(it)) { Text(stringResource(Res.string.retry)) } }
    }
}
