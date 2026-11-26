package com.rncoding.testvineshield.core.presentation.blocks

import com.rncoding.testvineshield.core.presentation.components.InfoButton
import com.rncoding.testvineshield.core.presentation.components.InfoHelp
import com.rncoding.testvineshield.core.presentation.error.localizedAppErrorMessage
import androidx.compose.foundation.layout.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import com.rncoding.testvineshield.core.presentation.vineyard_details.localizedStage
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
    Scaffold(topBar = { RecordTopBar(stringResource(Res.string.blocks_title), onBack) }, floatingActionButton = {
        if (!state.loading && state.error==null) ExtendedFloatingActionButton(onClick = userAction(onCreate)) { Text(stringResource(Res.string.add_block)) }
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading) item(key = "fixed:BlockScreens:1", contentType = "fixed:BlockScreens:1") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error -> item(key = "fixed:BlockScreens:2", contentType = "fixed:BlockScreens:2") { ErrorNotice(localizedAppErrorMessage(error), vm::reload) } }
            if (!state.loading && state.error==null && state.blocks.isEmpty()) item(key = "fixed:BlockScreens:3", contentType = "fixed:BlockScreens:3") {
                Text(stringResource(Res.string.blocks_empty))
            }
            items(state.blocks, key = { "block:${it.blockId}" }, contentType = { "block-card" }) { block ->
                OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(block.name, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(Res.string.variety_value, block.vineVariety))
                        Text(stringResource(Res.string.area_value, localizedBlockArea(block.area, block.areaUnit)))
                        Text(stringResource(Res.string.stage_value, localizedStage(block.phenologicalStage)))
                        TextButton(onClick = userAction { onOpen(block.blockId) }) { Text(stringResource(Res.string.block_open, block.name)) }
                    } }
            }
        }
    }
}

@Composable
fun BlockDetailsScreenRoot(vineyardId: Long, blockId: Long, ownerId: Long, onBack: ()->Unit, onEdit: ()->Unit, onDeleted: ()->Unit, onBusyChanged: (Boolean)->Unit = {}, onActivities: ()->Unit = {}, onHarvests: ()->Unit = {}, onDiseaseObservations: ()->Unit = {}) {
    val vm = koinViewModel<BlockDetailsViewModel>(key = "block-$ownerId-$vineyardId-$blockId", parameters = { parametersOf(BlockKey(vineyardId, blockId)) })
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.load() }
    LaunchedEffect(state.deleted) { if (state.deleted) onDeleted() }
    DisposableEffect(state.deleting) {
        onBusyChanged(state.deleting)
        onDispose { onBusyChanged(false) }
    }
    Scaffold(topBar = { RecordTopBar(stringResource(Res.string.block_details), { if (!vm.state.value.deleting) onBack() }, !state.deleting) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading || state.deleting) item(key = "fixed:BlockScreens:4", contentType = "fixed:BlockScreens:4") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error -> item(key = "fixed:BlockScreens:5", contentType = "fixed:BlockScreens:5") { ErrorNotice(localizedAppErrorMessage(error), vm::load) } }
            state.block?.let { block ->
                item(key = "fixed:BlockScreens:6", contentType = "fixed:BlockScreens:6") { Text(block.name, style = MaterialTheme.typography.headlineSmall) }
                item(key = "fixed:BlockScreens:7", contentType = "fixed:BlockScreens:7") { TextButton(onClick=userAction(onActivities),enabled=!state.deleting) { Text(stringResource(Res.string.block_activities)) } }
                item(key = "fixed:BlockScreens:8", contentType = "fixed:BlockScreens:8") { TextButton(onClick=userAction(onHarvests),enabled=!state.deleting) { Text(stringResource(Res.string.harvest_records)) } }
                item(key = "fixed:BlockScreens:9", contentType = "fixed:BlockScreens:9") { TextButton(onClick = userAction(onDiseaseObservations), enabled = !state.deleting) { Text(stringResource(Res.string.disease_observations)) } }
                item(key = "fixed:BlockScreens:10", contentType = "fixed:BlockScreens:10") { Text(stringResource(Res.string.variety_value, block.vineVariety)) }
                item(key = "fixed:BlockScreens:11", contentType = "fixed:BlockScreens:11") { Text(stringResource(Res.string.area_value, localizedBlockArea(block.area, block.areaUnit))) }
                item(key = "fixed:BlockScreens:12", contentType = "fixed:BlockScreens:12") { Text(stringResource(Res.string.block_rows, block.rowCount?.toString() ?: stringResource(Res.string.unknown_value))) }
                item(key = "fixed:BlockScreens:13", contentType = "fixed:BlockScreens:13") { Text(stringResource(Res.string.block_spacing, block.rowSpacing?.let { "${formatPlainDecimal(it)} ${block.rowSpacingUnit?.symbol ?: stringResource(Res.string.unknown_unit)}" } ?: stringResource(Res.string.unknown_value))) }
                item(key = "fixed:BlockScreens:14", contentType = "fixed:BlockScreens:14") { Text(stringResource(Res.string.block_planted, block.plantedAt?.toString() ?: stringResource(Res.string.unknown_value))) }
                item(key = "fixed:BlockScreens:15", contentType = "fixed:BlockScreens:15") {
                    Text(stringResource(Res.string.block_manual_stage, localizedStage(block.phenologicalStage)))
                    if (block.phenologicalStage != null) {
                        Text(stringResource(Res.string.block_observed_on, block.stageObservedOn?.toString() ?: stringResource(Res.string.date_unknown)))
                    }
                }
                item(key = "fixed:BlockScreens:16", contentType = "fixed:BlockScreens:16") { Text(stringResource(Res.string.recent_stage_observations), style = MaterialTheme.typography.titleMedium) }
                val historyError = state.stageHistoryError
                when {
                    historyError != null -> item(key = "fixed:BlockScreens:17", contentType = "fixed:BlockScreens:17") { ErrorNotice(localizedAppErrorMessage(historyError), vm::load) }
                    state.stageObservations.isEmpty() -> item(key = "fixed:BlockScreens:18", contentType = "fixed:BlockScreens:18") { Text(stringResource(Res.string.no_stage_observations)) }
                    else -> items(state.stageObservations, key = { "stage:${it.observedOn}" }, contentType = { "stage-history" }) {
                        Text("${it.observedOn}: ${localizedStage(it.stage)}")
                    }
                }
                item(key = "fixed:BlockScreens:19", contentType = "fixed:BlockScreens:19") { Text(stringResource(Res.string.block_rootstock, block.rootStock.ifBlank { stringResource(Res.string.unknown_value) })) }
                item(key = "fixed:BlockScreens:20", contentType = "fixed:BlockScreens:20") { Text(stringResource(Res.string.block_colour, block.color.ifBlank { stringResource(Res.string.unknown_value) })) }
                item(key = "fixed:BlockScreens:21", contentType = "fixed:BlockScreens:21") { Button(onClick = userAction(onEdit), enabled = !state.deleting, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.edit_block)) } }
                item(key = "fixed:BlockScreens:22", contentType = "fixed:BlockScreens:22") { OutlinedButton(onClick = userAction(vm::requestDelete), enabled = !state.deleting, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.delete_block)) } }
            }
        }
    }
    if (state.confirmDelete) {
        val activity = LocalUserActivity.current
        AlertDialog(modifier = Modifier.userActivity(activity), onDismissRequest = userAction(vm::cancelDelete), title = { Text(stringResource(Res.string.delete_block_title)) }, text = { Text(stringResource(Res.string.delete_related_warning)) }, confirmButton = { TextButton(onClick = userAction { vm.confirmDelete(); onBusyChanged(vm.state.value.deleting) }, enabled = !state.deleting) { Text(stringResource(Res.string.delete_record)) } }, dismissButton = { TextButton(onClick = userAction(vm::cancelDelete), enabled = !state.deleting) { Text(stringResource(Res.string.cancel_action)) } })
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
    val stageLabels = PhenologicalStage.entries.associateWith { localizedStage(it) }
    Scaffold(topBar = { RecordTopBar(if (editing) stringResource(Res.string.edit_block) else stringResource(Res.string.new_block), onBack, !state.saving) }) { padding ->
        LazyColumn(Modifier.padding(padding).imePadding().fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.loading || state.saving) item(key = "fixed:BlockScreens:23", contentType = "fixed:BlockScreens:23") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error -> item(key = "fixed:BlockScreens:24", contentType = "fixed:BlockScreens:24") { ErrorNotice(localizedBlockEditorIssue(error), if (!state.ready) onRetry else null) } }
            item(key = "fixed:BlockScreens:25", contentType = "fixed:BlockScreens:25") { InfoHelp(title = stringResource(Res.string.info_form), text = stringResource(Res.string.block_save_help)) }
            items(BlockField.entries.filter { it != BlockField.StageObservedOn }, key = { "block-field:${it.name}" }, contentType = { "block-field:${it.name}" }) { field ->
                val (label, help, keyboard) = when (field) {
                    BlockField.StageObservedOn -> error("Observation date is rendered with the stage picker.")
                    BlockField.Name -> Triple(stringResource(Res.string.block_name), stringResource(Res.string.required_field), KeyboardType.Text)
                    BlockField.Variety -> Triple(stringResource(Res.string.vine_variety), stringResource(Res.string.required_field), KeyboardType.Text)
                    BlockField.Color -> Triple(stringResource(Res.string.colour_label), stringResource(Res.string.optional_description), KeyboardType.Text)
                    BlockField.RootStock -> Triple(stringResource(Res.string.rootstock_label), stringResource(Res.string.optional_description), KeyboardType.Text)
                    BlockField.Area -> Triple(stringResource(Res.string.block_area_label, state.draft.areaUnit?.symbol ?: stringResource(Res.string.unknown_unit)), stringResource(Res.string.optional_decimal), KeyboardType.Decimal)
                    BlockField.Rows -> Triple(stringResource(Res.string.row_count), stringResource(Res.string.optional_positive_integer), KeyboardType.Number)
                    BlockField.Spacing -> Triple(stringResource(Res.string.block_spacing_label, state.draft.spacingUnit?.symbol ?: stringResource(Res.string.unknown_unit)), stringResource(Res.string.optional_decimal_short), KeyboardType.Decimal)
                    BlockField.Planted -> Triple(stringResource(Res.string.planting_date), stringResource(Res.string.optional_iso_date), KeyboardType.Ascii)
                }
                OutlinedTextField(value = state.draft.text(field), onValueChange = { activity()
                    onText(field, it) }, enabled = state.canEdit, modifier = Modifier.fillMaxWidth(), label = {Text(label)}, singleLine = true, isError = field in state.errors, supportingText = { state.errors[field]?.let { Text(localizedBlockDraftIssue(it)) } }, trailingIcon = { InfoButton(label, help) }, keyboardOptions = KeyboardOptions(keyboardType = keyboard))
                if (field==BlockField.Area) RecordChoice(stringResource(Res.string.area_unit), state.draft.areaUnit, AreaUnit.entries, state.canEdit, {it.symbol}, onAreaUnit, nullLabel = stringResource(Res.string.unit_unspecified))
                if (field==BlockField.Spacing) RecordChoice(stringResource(Res.string.spacing_unit), state.draft.spacingUnit, LengthUnit.entries, state.canEdit, {it.symbol}, onSpacingUnit, nullLabel = stringResource(Res.string.unit_unspecified))
            }
            item(key = "fixed:BlockScreens:26", contentType = "fixed:BlockScreens:26") {
                RecordChoice(stringResource(Res.string.observed_growth_stage), state.draft.stage, PhenologicalStage.entries, state.canEdit, { stageLabels.getValue(it) }, onStage, nullLabel = stringResource(Res.string.unknown_value))
                if (state.draft.stage != null) {
                    OutlinedTextField(
                        value = state.draft.stageObservedOn,
                        onValueChange = {
                            activity()
                            onText(BlockField.StageObservedOn, it)
                        },
                        label = { Text(stringResource(Res.string.stage_observation_date)) },
                        trailingIcon = { InfoButton(stringResource(Res.string.stage_observation_date), stringResource(Res.string.stage_date_help)) },
                        supportingText = {
                            state.errors[BlockField.StageObservedOn]?.let { Text(localizedBlockDraftIssue(it)) }
                        },
                        isError = BlockField.StageObservedOn in state.errors,
                        enabled = state.canEdit,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                    )
                }
            }
            item(key = "fixed:BlockScreens:27", contentType = "fixed:BlockScreens:27") { InfoHelp(title = stringResource(Res.string.info_measurements), text = stringResource(Res.string.block_unknown_help)) }
            item(key = "fixed:BlockScreens:28", contentType = "fixed:BlockScreens:28") { Button(onClick = userAction(onSave), enabled = state.canEdit, modifier = Modifier.fillMaxWidth()) { Text(if (state.saving) stringResource(Res.string.saving_action) else stringResource(Res.string.save_block)) } }
        }
    }
}

@Composable
private fun ErrorNotice(message: String, retry: (()->Unit)?) {
    Column { Text(message, color = MaterialTheme.colorScheme.error)
        retry?.let {TextButton(onClick = userAction(it)) {Text(stringResource(Res.string.retry))} } }
}
@Composable
private fun localizedBlockArea(area: Double?, unit: AreaUnit?): String =
    area?.let { "${formatPlainDecimal(it)} ${unit?.symbol ?: stringResource(Res.string.unknown_unit)}" }
        ?: stringResource(Res.string.unknown_value)
