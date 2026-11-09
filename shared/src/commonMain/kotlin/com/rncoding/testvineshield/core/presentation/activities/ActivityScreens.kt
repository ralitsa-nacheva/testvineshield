package com.rncoding.testvineshield.core.presentation.activities

import androidx.compose.foundation.layout.*
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
    Scaffold(topBar={RecordTopBar(state.snapshot.title,onBack)},floatingActionButton={
        if(!home && !state.loading && state.error==null) ExtendedFloatingActionButton(onClick=userAction(onCreate)) {Text("Add activity")}
    }) {padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max=720.dp).fillMaxWidth(),
            contentPadding=PaddingValues(16.dp,16.dp,16.dp,100.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(state.loading) item {LinearProgressIndicator(Modifier.fillMaxWidth())}
            state.error?.let {message -> item {ActivityErrorNotice(message,vm::reload)} }
            if(home && !state.loading && state.error==null) {
                item {Text("Choose a vineyard to view its activity notebook.")}
                if(state.snapshot.vineyards.isEmpty()) item {Text("Create a vineyard in the Vineyards tab first.")}
                items(state.snapshot.vineyards,key={it.id}) {parent ->
                    OutlinedButton(onClick=userAction {onVineyard(parent.id)},modifier=Modifier.fillMaxWidth()) {Text(parent.name)}
                }
            } else if(!home && state.error==null && !state.loading) {
                item {RecordChoice("Status filter",ActivityStatus.entries.firstOrNull {it.dbValue==statusFilter},ActivityStatus.entries,true,
                    {label(it.dbValue)},{statusFilter=it?.dbValue.orEmpty()},nullLabel="All statuses")}
                val selectedFilter=ActivityStatus.entries.firstOrNull {it.dbValue==statusFilter}
                val records=state.snapshot.activities.filter {selectedFilter==null || it.status==selectedFilter.dbValue}
                if(records.isEmpty()) item {Text("No activities match this view. Add one or change the status filter.")}
                items(records,key={it.activityId}) {record ->
                    OutlinedCard(Modifier.fillMaxWidth()) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text(record.activityType,style=MaterialTheme.typography.titleMedium)
                        Text("${label(record.status)} · ${label(record.priority)} priority · ${record.season}")
                        Text("Planned: ${record.plannedAt}")
                        record.completedAt?.let {Text("Completed: $it")}
                        Text(record.blockId?.let {id -> state.snapshot.blocks.firstOrNull {it.blockId==id}?.name ?: "Block unavailable"} ?: "Whole vineyard")
                        TextButton(onClick=userAction {onOpen(record.activityId)}) {Text("Open activity")}
                    } }
                }
            }
        }
    }
}

@Composable
fun ActivityDetailsScreenRoot(ownerId: Long,vineyardId: Long,activityId: Long,onBack: ()->Unit,onEdit: ()->Unit,
    onComplete: ()->Unit,onDeleted: ()->Unit,onBusyChanged: (Boolean)->Unit={}) {
    val vm=koinViewModel<ActivityDetailsViewModel>(key="activity-$ownerId-$vineyardId-$activityId",parameters={parametersOf(ActivityKey(vineyardId,activityId))})
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {vm.load()}
    LaunchedEffect(state.deleted) {if(state.deleted) onDeleted()}
    DisposableEffect(state.deleting) {onBusyChanged(state.deleting);onDispose {onBusyChanged(false)}}
    Scaffold(topBar={RecordTopBar("Activity details",{if(!vm.state.value.deleting) onBack()},!state.deleting)}) {padding ->
        LazyColumn(Modifier.padding(padding).fillMaxHeight().widthIn(max=720.dp).fillMaxWidth(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(state.loading || state.deleting) item {LinearProgressIndicator(Modifier.fillMaxWidth())}
            state.error?.let {message -> item {ActivityErrorNotice(message,vm::load)} }
            state.record?.let {record ->
                item {Text(record.activityType,style=MaterialTheme.typography.headlineSmall)}
                item {Text("Scope: ${state.scopeLabel}")}
                item {Text("Season: ${record.season}")}
                item {Text("Status: ${label(record.status)} · Priority: ${label(record.priority)}")}
                item {Text("Planned: ${record.plannedAt} · Completed: ${record.completedAt?.toString() ?: "Not recorded"}")}
                item {Text("Stage: ${record.phenologicalStage?.let(::label) ?: "Unknown"}")}
                item {Text("Description: ${record.activityDescription.ifBlank {"Not recorded"}}")}
                item {Text("Tool: ${record.tool ?: "Not recorded"}")}
                item {Text("Notes: ${record.notes ?: "Not recorded"}")}
                // Existing structured treatment data is read-only here and preserved by basic edits.
                record.treatmentProduct?.let {value -> item {Text("Recorded treatment product: $value")} }
                record.treatmentProductQuantity?.let {value -> item {Text("Recorded treatment quantity: $value ${record.treatmentProductUnits ?: "(unit unknown)"}")} }
                record.waterQuantity?.let {value -> item {Text("Recorded water quantity: $value (unit not stored)")} }
                record.treatmentProductRate?.let {value -> item {Text("Recorded rate: $value (rate units not stored)")} }
                item {Button(onClick=userAction(onEdit),enabled=!state.deleting,modifier=Modifier.fillMaxWidth()) {Text("Edit activity")} }
                if(record.status!=ActivityStatus.COMPLETED.dbValue) item {
                    OutlinedButton(onClick=userAction(onComplete),enabled=!state.deleting,modifier=Modifier.fillMaxWidth()) {Text("Record completion")}
                }
                item {OutlinedButton(onClick=userAction(vm::requestDelete),enabled=!state.deleting,modifier=Modifier.fillMaxWidth()) {Text("Delete activity")} }
            }
        }
    }
    if(state.confirmDelete) AlertDialog(modifier=Modifier.userActivity(LocalUserActivity.current),onDismissRequest=userAction(vm::cancelDelete),
        title={Text("Delete activity?")},text={Text("This cannot be undone. Activities linked to a disease observation cannot be deleted.")},
        confirmButton={TextButton(onClick=userAction {vm.confirmDelete();onBusyChanged(vm.state.value.deleting)},enabled=!state.deleting) {Text("Delete")}},
        dismissButton={TextButton(onClick=userAction(vm::cancelDelete),enabled=!state.deleting) {Text("Cancel")}})
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
    Scaffold(topBar={RecordTopBar(if(activityId==0L) "New activity" else "Edit activity",{if(!vm.state.value.saving) onBack()},!state.saving)}) {padding ->
        LazyColumn(Modifier.padding(padding).imePadding().fillMaxHeight().widthIn(max=720.dp).fillMaxWidth(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            if(state.loading || state.saving) item {LinearProgressIndicator(Modifier.fillMaxWidth())}
            state.error?.let {message -> item {ActivityErrorNotice(message,if(!state.ready) vm::load else null)} }
            if(state.ready) item {Text("Scope: ${state.scopeLabel}")}
            item {Text("Basic notebook entry. Detailed spray application records are not available in this form. Save to keep changes; Back discards unsaved changes.")}
            items(ActivityField.entries,key={it.name}) {field ->
                val title=when(field) {
                    ActivityField.Type -> "Activity type * (e.g. pruning)"
                    ActivityField.Description -> "Description"
                    ActivityField.Season -> "Season * (e.g. 2026 or 2026/2027)"
                    ActivityField.Planned -> "Planned date * (YYYY-MM-DD)"
                    ActivityField.Completed -> "Completion date (YYYY-MM-DD)"
                    ActivityField.Tool -> "Tool / equipment"
                    ActivityField.Notes -> "Notes"
                }
                OutlinedTextField(value=state.draft.text(field),onValueChange={activity();vm.text(field,it)},label={Text(title)},
                    singleLine=field!=ActivityField.Description && field!=ActivityField.Notes,enabled=state.canEdit,modifier=Modifier.fillMaxWidth(),
                    keyboardOptions=KeyboardOptions(keyboardType=if(field==ActivityField.Planned || field==ActivityField.Completed) KeyboardType.Ascii else KeyboardType.Text),
                    isError=field in state.errors,supportingText={Text(state.errors[field] ?: if(field==ActivityField.Completed) "Required only for Completed; clear it for other statuses." else "")})
            }
            item {RecordChoice("Status",state.draft.status,ActivityStatus.entries,state.canEdit,{label(it.dbValue)},{it?.let(vm::status)},allowUnknown=false)}
            item {RecordChoice("Priority",state.draft.priority,ActivityPriority.entries,state.canEdit,{label(it.dbValue)},{it?.let(vm::priority)},allowUnknown=false)}
            item {RecordChoice("Observed vine stage",state.draft.stage,PhenologicalStage.entries,state.canEdit,{label(it.dbValue)},vm::stage)}
            item {Text("Leave stage unknown when unobserved. Planned is the target date; completion is when the work was done and may be earlier. Keep both dates accurate.")}
            item {Button(onClick=userAction {vm.save();onBusyChanged(vm.state.value.saving)},enabled=state.canEdit,modifier=Modifier.fillMaxWidth()) {Text("Save activity")} }
        }
    }
}
@Composable
private fun ActivityErrorNotice(message: String,retry: (()->Unit)?) {
    Column {Text(message,color=MaterialTheme.colorScheme.error);retry?.let {TextButton(onClick=userAction(it)) {Text("Retry")}}}
}
private fun label(value: String): String = value.replace('_',' ').replaceFirstChar {it.uppercaseChar()}
