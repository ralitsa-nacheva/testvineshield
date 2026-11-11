package com.rncoding.testvineshield.core.presentation.blocks

import androidx.compose.foundation.layout.*
import com.rncoding.testvineshield.core.presentation.components.RecordChoice
import com.rncoding.testvineshield.core.presentation.components.RecordTopBar
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import com.rncoding.testvineshield.core.domain.datamodels.units.*
import com.rncoding.testvineshield.core.presentation.formatting.*
import com.rncoding.testvineshield.core.presentation.root.LocalUserActivity
import com.rncoding.testvineshield.core.presentation.root.userAction
import com.rncoding.testvineshield.core.presentation.root.userActivity
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val draftValuesSaver = listSaver<ArrayList<String>, String>(
    save = { it.toList() },
    restore = { ArrayList(it) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockListScreenRoot(vineyardId: Long, ownerId: Long, onBack: ()->Unit, onCreate: ()->Unit, onOpen: (Long)->Unit) {
    val vm = koinViewModel<BlockListViewModel>(key = "blocks-$ownerId-$vineyardId", parameters = { parametersOf(vineyardId) })
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { RecordTopBar("Blocks", onBack) }, floatingActionButton = {
        if (!state.loading && state.error==null) ExtendedFloatingActionButton(onClick = userAction(onCreate)) { Text("Add block") }
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error -> item { ErrorNotice(error, vm::reload) } }
            if (!state.loading && state.error==null && state.blocks.isEmpty()) item {
                Text("No blocks yet. Add a block to record its variety, measurements and planting details.")
            }
            items(state.blocks, key = {it.blockId}) { block ->
                OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(block.name, style = MaterialTheme.typography.titleMedium)
                        Text("Variety: ${block.vineVariety}")
                        Text("Area: ${formatBlockArea(block.area,block.areaUnit)}")
                        Text("Stage: ${stageLabel(block.phenologicalStage)}")
                        TextButton(onClick = userAction { onOpen(block.blockId) }) { Text("Open ${block.name}") }
                    } }
            }
        }
    }
}

@Composable
fun BlockDetailsScreenRoot(vineyardId: Long, blockId: Long, ownerId: Long, onBack: ()->Unit, onEdit: ()->Unit, onDeleted: ()->Unit, onBusyChanged: (Boolean)->Unit = {}, onActivities: ()->Unit = {}, onHarvests: ()->Unit = {}) {
    val vm = koinViewModel<BlockDetailsViewModel>(key = "block-$ownerId-$vineyardId-$blockId", parameters = { parametersOf(BlockKey(vineyardId, blockId)) })
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.load() }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }
    DisposableEffect(state.deleting) {
        onBusyChanged(state.deleting)
        onDispose { onBusyChanged(false) }
    }
    Scaffold(topBar = { RecordTopBar("Block details", { if (!vm.state.value.deleting) onBack() }, !state.deleting) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading || state.deleting) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error -> item { ErrorNotice(error, vm::load) } }
            state.block?.let { block ->
                item { Text(block.name, style = MaterialTheme.typography.headlineSmall) }
                item { TextButton(onClick=userAction(onActivities),enabled=!state.deleting) { Text("Block activities") } }
                item { TextButton(onClick=userAction(onHarvests),enabled=!state.deleting) { Text("Harvest records") } }
                item { Text("Variety: ${block.vineVariety}") }
                item { Text("Area: ${formatBlockArea(block.area,block.areaUnit)}") }
                item { Text("Rows: ${block.rowCount?.toString() ?: "Unknown"}") }
                item { Text("Row spacing: ${block.rowSpacing?.let { "${formatPlainDecimal(it)} ${block.rowSpacingUnit?.symbol ?: "(unit unknown)"}" } ?: "Unknown"}") }
                item { Text("Planted: ${block.plantedAt?.toString() ?: "Unknown"}") }
                item {
                    Text("Stage (manual): ${stageLabel(block.phenologicalStage)}")
                    if (block.phenologicalStage != null) {
                        Text("Observed on: ${block.stageObservedOn ?: "Date unknown"}")
                    }
                }
                item { Text("Rootstock: ${block.rootStock.ifBlank { "Unknown" }}") }
                item { Text("Colour: ${block.color.ifBlank { "Unknown" }}") }
                item { Button(onClick = userAction(onEdit), enabled = !state.deleting, modifier = Modifier.fillMaxWidth()) { Text("Edit block") } }
                item { OutlinedButton(onClick = userAction(vm::requestDelete), enabled = !state.deleting, modifier = Modifier.fillMaxWidth()) { Text("Delete block") } }
            }
        }
    }
    if (state.confirmDelete) {
        val activity = LocalUserActivity.current
        AlertDialog(modifier = Modifier.userActivity(activity), onDismissRequest = userAction(vm::cancelDelete), title = { Text("Delete block?") }, text = { Text("Related records will also be deleted. This cannot be undone.") }, confirmButton = { TextButton(onClick = userAction { vm.confirmDelete(); onBusyChanged(vm.state.value.deleting) }, enabled = !state.deleting) { Text("Delete permanently") } }, dismissButton = { TextButton(onClick = userAction(vm::cancelDelete), enabled = !state.deleting) { Text("Cancel") } })
    }
}

@Composable
fun BlockEditorScreenRoot(vineyardId: Long, blockId: Long = 0, ownerId: Long, onBack: ()->Unit, onSaved: ()->Unit, onBusyChanged: (Boolean)->Unit = {}) {
    // Nav-entry SaveableStateHolder preserves primitive draft values on recreation/process restore.
    var savedDraft by rememberSaveable(ownerId, vineyardId, blockId, stateSaver = draftValuesSaver) { mutableStateOf(arrayListOf<String>()) }
    val vm = koinViewModel<BlockEditorViewModel>(key = "block-editor-$ownerId-$vineyardId-$blockId", parameters = {
        parametersOf(BlockEditorRequest(vineyardId, blockId, BlockDraft.restored(savedDraft)))
    })
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.draft, state.ready, state.savedId) {
        if (state.savedId != null) {
            savedDraft = arrayListOf()
            onSaved()
        } else if (state.ready) {
            savedDraft = state.draft.savedValues()
        }
    }
    DisposableEffect(state.saving) {
        onBusyChanged(state.saving)
        onDispose { onBusyChanged(false) }
    }
    BlockEditorScreen(
        state = state,
        editing = blockId != 0L,
        onBack = { if (!vm.state.value.saving) onBack() },
        onSave = {
            vm.save()
            // Set the shell guard in the same event, before a deferred recomposition.
            onBusyChanged(vm.state.value.saving)
        },
        onRetry = vm::load,
        onText = vm::text,
        onAreaUnit = vm::areaUnit,
        onSpacingUnit = vm::spacingUnit,
        onStage = vm::stage
    )
}

@Composable
fun BlockEditorScreen(state: BlockEditorState, editing: Boolean, onBack: ()->Unit, onSave: ()->Unit, onRetry: ()->Unit, onText: (BlockField, String)->Unit, onAreaUnit: (AreaUnit?)->Unit, onSpacingUnit: (LengthUnit?)->Unit, onStage: (PhenologicalStage?)->Unit) {
    val activity = LocalUserActivity.current
    Scaffold(topBar = { RecordTopBar(if (editing) "Edit block" else "New block", onBack, !state.saving) }) { padding ->
        LazyColumn(Modifier.padding(padding).imePadding().fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading || state.saving) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error -> item { ErrorNotice(error, if (!state.ready) onRetry else null) } }
            item { Text("Save to keep changes. Back discards unsaved changes. Leave optional measurements and planting date blank when unknown.") }
            items(BlockField.entries.filter { it != BlockField.StageObservedOn }, key = { it.name }) { field ->
                val (label, help, keyboard) = when (field) {
                    BlockField.StageObservedOn -> error("Observation date is rendered with the stage picker.")
                    BlockField.Name -> Triple("Block name *", "Required", KeyboardType.Text)
                    BlockField.Variety -> Triple("Vine variety *", "Required", KeyboardType.Text)
                    BlockField.Color -> Triple("Colour", "Optional description", KeyboardType.Text)
                    BlockField.RootStock -> Triple("Rootstock", "Optional description", KeyboardType.Text)
                    BlockField.Area -> Triple("Area (${state.draft.areaUnit?.symbol ?: "unit unknown"})", "Optional; one dot or comma decimal separator; no thousands grouping", KeyboardType.Decimal)
                    BlockField.Rows -> Triple("Row count", "Optional positive whole number", KeyboardType.Number)
                    BlockField.Spacing -> Triple("Row spacing (${state.draft.spacingUnit?.symbol ?: "unit unknown"})", "Optional; one dot or comma decimal separator", KeyboardType.Decimal)
                    BlockField.Planted -> Triple("Planting date", "Optional ISO date, e.g. 2020-04-15", KeyboardType.Ascii)
                }
                OutlinedTextField(value = state.draft.text(field), onValueChange = { activity()
                    onText(field, it) }, enabled = state.canEdit, modifier = Modifier.fillMaxWidth(), label = {Text(label)}, singleLine = true, isError = field in state.errors, supportingText = {Text(state.errors[field]?: help)}, keyboardOptions = KeyboardOptions(keyboardType = keyboard))
                if (field==BlockField.Area) RecordChoice("Area unit", state.draft.areaUnit, AreaUnit.entries, state.canEdit, {it.symbol}, onAreaUnit)
                if (field==BlockField.Spacing) RecordChoice("Spacing unit", state.draft.spacingUnit, LengthUnit.entries, state.canEdit, {it.symbol}, onSpacingUnit)
            }
            item {
                RecordChoice("Observed growth stage", state.draft.stage, PhenologicalStage.entries, state.canEdit, { stageLabel(it) }, onStage)
                if (state.draft.stage != null) {
                    OutlinedTextField(
                        value = state.draft.stageObservedOn,
                        onValueChange = {
                            activity()
                            onText(BlockField.StageObservedOn, it)
                        },
                        label = { Text("Stage observation date") },
                        supportingText = {
                            Text(state.errors[BlockField.StageObservedOn] ?: "Optional. Use YYYY-MM-DD.")
                        },
                        isError = BlockField.StageObservedOn in state.errors,
                        enabled = state.canEdit,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )
                }
            }
            item { Text("No stage is inferred from planting age. Unknown units never imply metres or hectares.") }
            item { Button(onClick = userAction(onSave), enabled = state.canEdit, modifier = Modifier.fillMaxWidth()) { Text(if (state.saving) "Saving…" else "Save block") } }
        }
    }
}

@Composable
private fun ErrorNotice(message: String, retry: (()->Unit)?) {
    Column { Text(message, color = MaterialTheme.colorScheme.error)
        retry?.let {TextButton(onClick = userAction(it)) {Text("Retry")} } }
}
private fun stageLabel(stage: PhenologicalStage?) = stage?.dbValue?.replace('_', ' ')?.replaceFirstChar {it.uppercaseChar()} ?: "Unknown"
