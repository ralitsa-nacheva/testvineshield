package com.rncoding.testvineshield.core.presentation.activities

import androidx.compose.foundation.layout.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import com.rncoding.testvineshield.core.presentation.vineyard_details.localizedActivityStatus
import com.rncoding.testvineshield.core.presentation.vineyard_details.localizedStage
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rncoding.testvineshield.core.domain.datamodels.enums.*
import com.rncoding.testvineshield.core.domain.usecases.activity.ActivityScope
import com.rncoding.testvineshield.core.presentation.components.*
import com.rncoding.testvineshield.core.presentation.root.*
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val draftSaver=listSaver<ArrayList<String>,String>(save={it.toList()},restore={ArrayList(it)})

@Composable
fun ActivityListScreenRoot(ownerId: Long,scope: ActivityScope=ActivityScope(),onBack: (()->Unit)?=null,
    onVineyard: (Long)->Unit={},onCreate: ()->Unit={},onOpen: (Long)->Unit={}) {
    val vm=koinViewModel<ActivityListViewModel>(key="activity-list-$ownerId-${scope.vineyardId}-${scope.blockId}",parameters={parametersOf(scope)})
    val state by vm.state.collectAsStateWithLifecycle()
    var statusFilter by rememberSaveable(ownerId,scope.vineyardId,scope.blockId) {mutableStateOf("")}
    val home=scope.vineyardId==0L
    val statuses = ActivityStatus.entries.associateWith { localizedActivityStatus(it) }
    Scaffold(topBar={RecordTopBar(if(home) stringResource(Res.string.activity_title) else state.snapshot.title,onBack)},floatingActionButton={
        if(!home && !state.loading && state.error==null) ExtendedFloatingActionButton(onClick=userAction(onCreate)) {Text(stringResource(Res.string.add_activity))}
    }) {padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max=720.dp).fillMaxWidth(),
            contentPadding=PaddingValues(16.dp,16.dp,16.dp,100.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(state.loading) item {LinearProgressIndicator(Modifier.fillMaxWidth())}
            state.error?.let {message -> item {ActivityErrorNotice(message,vm::reload)} }
            if(home && !state.loading && state.error==null) {
                item {Text(stringResource(Res.string.activity_choose_vineyard))}
                if(state.snapshot.vineyards.isEmpty()) item {Text(stringResource(Res.string.activity_create_vineyard))}
                items(state.snapshot.vineyards,key={it.id}) {parent ->
                    OutlinedButton(onClick=userAction {onVineyard(parent.id)},modifier=Modifier.fillMaxWidth()) {Text(parent.name)}
                }
            } else if(!home && state.error==null && !state.loading) {
                item {RecordChoice(stringResource(Res.string.status_filter),ActivityStatus.entries.firstOrNull {it.dbValue==statusFilter},ActivityStatus.entries,true,
                    {statuses.getValue(it)},{statusFilter=it?.dbValue.orEmpty()},nullLabel=stringResource(Res.string.all_statuses))}
                val selectedFilter=ActivityStatus.entries.firstOrNull {it.dbValue==statusFilter}
                val records=state.snapshot.activities.filter {selectedFilter==null || it.status==selectedFilter.dbValue}
                if(records.isEmpty()) item {Text(stringResource(Res.string.activities_empty))}
                items(records,key={it.activityId}) {record ->
                    OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(record.activityType,style=MaterialTheme.typography.titleMedium)
                        Text(stringResource(Res.string.activity_summary, statusText(record.status), priorityText(record.priority), record.season))
                        Text(stringResource(Res.string.activity_planned, record.plannedAt.toString()))
                        record.completedAt?.let {Text(stringResource(Res.string.activity_completed, it.toString()))}
                        Text(record.blockId?.let {id -> state.snapshot.blocks.firstOrNull {it.blockId==id}?.name ?: stringResource(Res.string.block_unavailable)} ?: stringResource(Res.string.whole_vineyard))
                        TextButton(onClick=userAction {onOpen(record.activityId)}) {Text(stringResource(Res.string.open_activity))}
                    } }
                }
            }
        }
    }
}

@Composable
fun ActivityDetailsScreenRoot(ownerId: Long,vineyardId: Long,activityId: Long,onBack: ()->Unit,onEdit: ()->Unit,
    onComplete: ()->Unit,onDeleted: ()->Unit,onBusyChanged: (Boolean)->Unit={},onSpray: ()->Unit={}) {
    val vm=koinViewModel<ActivityDetailsViewModel>(key="activity-$ownerId-$vineyardId-$activityId",parameters={parametersOf(ActivityKey(vineyardId,activityId))})
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {vm.load()}
    LaunchedEffect(state.deleted) {if(state.deleted) onDeleted()}
    DisposableEffect(state.deleting) {onBusyChanged(state.deleting);onDispose {onBusyChanged(false)}}
    Scaffold(topBar={RecordTopBar(stringResource(Res.string.activity_details),{if(!vm.state.value.deleting) onBack()},!state.deleting)}) {padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max=720.dp).fillMaxWidth(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(state.loading || state.deleting) item {LinearProgressIndicator(Modifier.fillMaxWidth())}
            state.error?.let {message -> item {ActivityErrorNotice(message,vm::load)} }
            state.record?.let {record ->
                item {Text(record.activityType,style=MaterialTheme.typography.headlineSmall)}
                item {Text(stringResource(Res.string.activity_scope, when {
                    state.scopeIsWholeVineyard -> stringResource(Res.string.whole_vineyard)
                    state.scopeUnavailable -> stringResource(Res.string.block_unavailable)
                    else -> state.scopeLabel
                }))}
                item {Text(stringResource(Res.string.activity_season, record.season))}
                item {Text(stringResource(Res.string.activity_status_priority, statusText(record.status), priorityText(record.priority)))}
                item {Text(stringResource(Res.string.activity_dates, record.plannedAt.toString(), record.completedAt?.toString() ?: stringResource(Res.string.not_recorded)))}
                item {Text(stringResource(Res.string.stage_value, stageText(record.phenologicalStage)))}
                item {Text(stringResource(Res.string.activity_description, record.activityDescription.ifBlank {stringResource(Res.string.not_recorded)}))}
                item {Text(stringResource(Res.string.activity_tool, record.tool ?: stringResource(Res.string.not_recorded)))}
                item {Text(stringResource(Res.string.activity_notes, record.notes ?: stringResource(Res.string.not_recorded)))}
                item {OutlinedButton(onClick=userAction(onSpray),enabled=!state.deleting,modifier=Modifier.fillMaxWidth()) {Text(stringResource(Res.string.spray_record_lines))} }
                // Prior single-product treatment data is read-only here and preserved by basic edits.
                record.treatmentProduct?.let {value -> item {Text(stringResource(Res.string.legacy_product, value))} }
                record.treatmentProductQuantity?.let {value -> item {Text(stringResource(Res.string.legacy_quantity, value.toString(), record.treatmentProductUnits ?: stringResource(Res.string.unknown_unit)))} }
                record.waterQuantity?.let {value -> item {Text(stringResource(Res.string.legacy_water, value.toString()))} }
                record.treatmentProductRate?.let {value -> item {Text(stringResource(Res.string.legacy_rate, value.toString()))} }
                item {Button(onClick=userAction(onEdit),enabled=!state.deleting,modifier=Modifier.fillMaxWidth()) {Text(stringResource(Res.string.edit_activity))} }
                if(record.status!=ActivityStatus.COMPLETED.dbValue) item {
                    OutlinedButton(onClick=userAction(onComplete),enabled=!state.deleting,modifier=Modifier.fillMaxWidth()) {Text(stringResource(Res.string.record_completion))}
                }
                item {OutlinedButton(onClick=userAction(vm::requestDelete),enabled=!state.deleting,modifier=Modifier.fillMaxWidth()) {Text(stringResource(Res.string.delete_activity))} }
            }
        }
    }
    if(state.confirmDelete) AlertDialog(modifier=Modifier.userActivity(LocalUserActivity.current),onDismissRequest=userAction(vm::cancelDelete),
        title={Text(stringResource(Res.string.delete_activity_title))},text={Text(stringResource(Res.string.delete_activity_warning))},
        confirmButton={TextButton(onClick=userAction {vm.confirmDelete();onBusyChanged(vm.state.value.deleting)},enabled=!state.deleting) {Text(stringResource(Res.string.delete_record))}},
        dismissButton={TextButton(onClick=userAction(vm::cancelDelete),enabled=!state.deleting) {Text(stringResource(Res.string.cancel_action))}})
}

@Composable
fun ActivityEditorScreenRoot(ownerId: Long,scope: ActivityScope,activityId: Long=0,complete: Boolean=false,
    onBack: ()->Unit,onSaved: ()->Unit,onBusyChanged: (Boolean)->Unit={}) {
    var draftValues by rememberSaveable(ownerId,scope.vineyardId,scope.blockId,activityId,complete,stateSaver=draftSaver) {mutableStateOf(arrayListOf<String>())}
    val vm=koinViewModel<ActivityEditorViewModel>(key="activity-editor-$ownerId-${scope.vineyardId}-${scope.blockId}-$activityId-$complete",parameters={
        parametersOf(ActivityEditorRequest(scope,activityId,complete,ActivityDraft.restore(draftValues)))
    })
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.draft,state.ready,state.saved) {
        if(state.saved) {draftValues=arrayListOf();onSaved()}
        else if(state.ready) draftValues=state.draft.savedValues()
    }
    DisposableEffect(state.saving) {onBusyChanged(state.saving);onDispose {onBusyChanged(false)}}
    val activity=LocalUserActivity.current
    val statuses = ActivityStatus.entries.associateWith { localizedActivityStatus(it) }
    val priorities = ActivityPriority.entries.associateWith { priorityText(it.dbValue) }
    val stages = PhenologicalStage.entries.associateWith { localizedStage(it) }
    Scaffold(topBar={RecordTopBar(if(activityId==0L) stringResource(Res.string.new_activity) else stringResource(Res.string.edit_activity),{if(!vm.state.value.saving) onBack()},!state.saving)}) {padding ->
        LazyColumn(Modifier.padding(padding).imePadding().fillMaxHeight().widthIn(max=720.dp).fillMaxWidth(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(state.loading || state.saving) item {LinearProgressIndicator(Modifier.fillMaxWidth())}
            state.error?.let {message -> item {ActivityErrorNotice(message,if(!state.ready) vm::load else null)} }
            if(state.ready) item {Text(stringResource(Res.string.activity_scope, when {
                    state.scopeIsWholeVineyard -> stringResource(Res.string.whole_vineyard)
                    state.scopeUnavailable -> stringResource(Res.string.block_unavailable)
                    else -> state.scopeLabel
                }))}
            item {Text(stringResource(Res.string.activity_basic_help))}
            items(ActivityField.entries,key={it.name}) {field ->
                val title=when(field) {
                    ActivityField.Type -> stringResource(Res.string.activity_type_label)
                    ActivityField.Description -> stringResource(Res.string.description_label)
                    ActivityField.Season -> stringResource(Res.string.season_label)
                    ActivityField.Planned -> stringResource(Res.string.planned_date_label)
                    ActivityField.Completed -> stringResource(Res.string.completed_date_label)
                    ActivityField.Tool -> stringResource(Res.string.tool_label)
                    ActivityField.Notes -> stringResource(Res.string.notes_label)
                }
                OutlinedTextField(value=state.draft.text(field),onValueChange={activity();vm.text(field,it)},label={Text(title)},
                    singleLine=field!=ActivityField.Description && field!=ActivityField.Notes,enabled=state.canEdit,modifier=Modifier.fillMaxWidth(),
                    keyboardOptions=KeyboardOptions(keyboardType=if(field==ActivityField.Planned || field==ActivityField.Completed) KeyboardType.Ascii else KeyboardType.Text),
                    isError=field in state.errors,supportingText={Text(state.errors[field] ?: if(field==ActivityField.Completed) stringResource(Res.string.completion_date_help) else "")})
            }
            item {RecordChoice(stringResource(Res.string.status_label),state.draft.status,ActivityStatus.entries,state.canEdit,{statuses.getValue(it)},{it?.let(vm::status)},allowUnknown=false)}
            item {RecordChoice(stringResource(Res.string.priority_label),state.draft.priority,ActivityPriority.entries,state.canEdit,{priorities.getValue(it)},{it?.let(vm::priority)},allowUnknown=false)}
            item {RecordChoice(stringResource(Res.string.observed_vine_stage),state.draft.stage,PhenologicalStage.entries,state.canEdit,{stages.getValue(it)},vm::stage,nullLabel=stringResource(Res.string.unknown_value))}
            item {Text(stringResource(Res.string.activity_dates_help))}
            item {Button(onClick=userAction {vm.save();onBusyChanged(vm.state.value.saving)},enabled=state.canEdit,modifier=Modifier.fillMaxWidth()) {Text(stringResource(Res.string.save_activity))} }
        }
    }
}
@Composable
private fun ActivityErrorNotice(message: String,retry: (()->Unit)?) {
    Column {Text(message,color=MaterialTheme.colorScheme.error);retry?.let {TextButton(onClick=userAction(it)) {Text(stringResource(Res.string.retry))}}}
}
@Composable
private fun statusText(value: String): String = ActivityStatus.entries.firstOrNull { it.dbValue == value }
    ?.let { localizedActivityStatus(it) } ?: value

@Composable
private fun priorityText(value: String): String = when (ActivityPriority.entries.firstOrNull { it.dbValue == value }) {
    ActivityPriority.LOW -> stringResource(Res.string.priority_low)
    ActivityPriority.MEDIUM -> stringResource(Res.string.priority_medium)
    ActivityPriority.HIGH -> stringResource(Res.string.priority_high)
    ActivityPriority.URGENT -> stringResource(Res.string.priority_urgent)
    null -> value
}

@Composable
private fun stageText(value: String?): String = when {
    value == null -> localizedStage(null)
    else -> PhenologicalStage.entries.firstOrNull { it.dbValue == value }?.let { localizedStage(it) } ?: value
}
