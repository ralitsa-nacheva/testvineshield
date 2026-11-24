package com.rncoding.testvineshield.core.presentation.disease_observations

import androidx.compose.foundation.layout.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import com.rncoding.testvineshield.core.presentation.vineyard_details.localizedOccurrenceStatus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rncoding.testvineshield.core.domain.datamodels.CompletedBlockActivity
import com.rncoding.testvineshield.core.domain.datamodels.BlockDiseaseObservation
import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import com.rncoding.testvineshield.core.domain.datamodels.ObservedReferenceSymptom
import com.rncoding.testvineshield.core.domain.datamodels.ReferenceSymptom
import com.rncoding.testvineshield.core.presentation.components.RecordChoice
import com.rncoding.testvineshield.core.presentation.components.RecordTopBar
import com.rncoding.testvineshield.core.presentation.root.LocalUserActivity
import com.rncoding.testvineshield.core.presentation.root.userAction
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun BlockDiseaseObservationsScreen(
    ownerId: Long,
    vineyardId: Long,
    blockId: Long,
    onBack: () -> Unit,
    onBusyChanged: (Boolean) -> Unit
) {
    val model = koinViewModel<BlockDiseaseObservationsViewModel>(
        key = "observations-$ownerId-$vineyardId-$blockId",
        parameters = { parametersOf(DiseaseObservationsKey(ownerId, vineyardId, blockId)) }
    )
    val state by model.state.collectAsStateWithLifecycle()
    val diseaseLabels = state.diseases.associate { it.diseaseId to builtInDiseaseName(it.code, it.name) }
    val statusLabels = DiseaseOccurrenceStatus.entries.associateWith { localizedOccurrenceStatus(it) }
    val busyChanged by rememberUpdatedState(onBusyChanged)
    DisposableEffect(state.saving) {
        busyChanged(state.saving)
        onDispose { busyChanged(false) }
    }

    state.dialog?.takeIf { it.kind == ObservationDialog.Kind.STATUS }?.let { dialog ->
        val observation = dialog.observation
        var selected by rememberSaveable(observation.id, observation.updatedAt) {
            mutableStateOf(observation.status)
        }
        AlertDialog(
            onDismissRequest = model::closeDialog,
            title = { Text(builtInDiseaseName(observation.diseaseCode, observation.diseaseName)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.observation_observed, observation.observedOn.toString(), observation.severity))
                    Text(stringResource(Res.string.observation_current_status, localizedOccurrenceStatus(observation.status)))
                    observation.curedAt?.let { Text(stringResource(Res.string.observation_resolved_on, it.toString())) }
                    observation.notes?.let { Text(it, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                    RecordChoice(
                        label = stringResource(Res.string.observation_new_status), selected = selected,
                        values = DiseaseOccurrenceStatus.entries, enabled = !state.saving,
                        text = { statusLabels.getValue(it) }, changed = { if (it != null) selected = it },
                        allowUnknown = false
                    )
                    if (selected == DiseaseOccurrenceStatus.RESOLVED && selected != observation.status) {
                        Text(stringResource(Res.string.observation_resolution_help))
                    }
                    if (selected != DiseaseOccurrenceStatus.ACTIVE && selected != observation.status) {
                        Text(stringResource(Res.string.observation_active_help))
                    }
                    state.dialogError?.let { Text(localizedObservationIssue(it), color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = userAction { model.saveStatus(selected) },
                    enabled = !state.saving && selected != observation.status
                ) { Text(if (state.saving) stringResource(Res.string.saving_action) else stringResource(Res.string.observation_save_status)) }
            },
            dismissButton = {
                TextButton(onClick = userAction(model::closeDialog), enabled = !state.saving) { Text(stringResource(Res.string.cancel_action)) }
            }
        )
    }

    state.dialog?.takeIf { it.kind == ObservationDialog.Kind.DETAILS }?.let { dialog ->
        EditObservationDialog(
            dialog.observation, state.saving, state.dialogError, model::closeDialog, model::saveDetails
        )
    }

    state.dialog?.takeIf { it.kind == ObservationDialog.Kind.REFERENCE_SYMPTOM }?.let { dialog ->
        ReferenceSymptomDialog(dialog.observation, dialog.symptomToChange, state.saving, state.dialogError, model::closeDialog, model::saveReferenceSymptom)
    }

    state.dialog?.takeIf { it.kind == ObservationDialog.Kind.REMOVE_REFERENCE_SYMPTOM }?.symptomToChange?.let { referenceSymptom ->
        AlertDialog(
            onDismissRequest = model::closeDialog,
            title = { Text(stringResource(Res.string.observation_remove_symptom_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(Res.string.observation_remove_symptom_date, referenceSymptom.observedOn.toString()))
                    state.dialogError?.let { Text(localizedObservationIssue(it), color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = userAction(model::removeReferenceSymptom), enabled = !state.saving) {
                    Text(if (state.saving) stringResource(Res.string.removing_action) else stringResource(Res.string.observation_remove_symptom))
                }
            },
            dismissButton = {
                TextButton(onClick = userAction(model::closeDialog), enabled = !state.saving) { Text(stringResource(Res.string.cancel_action)) }
            }
        )
    }

    state.dialog?.takeIf { it.kind == ObservationDialog.Kind.LINK_ACTIVITY }?.let { dialog ->
        val linkedIds = state.activityLinks[dialog.observation.id].orEmpty().map { it.activityId }.toSet()
        LinkActivityDialog(
            activities = state.completedActivities.filterNot { it.activityId in linkedIds },
            saving = state.saving,
            error = state.dialogError,
            onClose = model::closeDialog,
            onSave = model::saveActivityLink
        )
    }

    state.dialog?.activityToUnlink?.let { link ->
        AlertDialog(
            onDismissRequest = model::closeDialog,
            title = { Text(stringResource(Res.string.observation_unlink_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${link.appliedOn} · ${link.activityType}")
                    Text(stringResource(Res.string.observation_keep_activity))
                    state.dialogError?.let { Text(localizedObservationIssue(it), color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = userAction(model::removeActivityLink), enabled = !state.saving) {
                    Text(if (state.saving) stringResource(Res.string.removing_action) else stringResource(Res.string.observation_remove_link))
                }
            },
            dismissButton = {
                TextButton(onClick = userAction(model::closeDialog), enabled = !state.saving) { Text(stringResource(Res.string.cancel_action)) }
            }
        )
    }

    state.dialog?.takeIf { it.kind == ObservationDialog.Kind.DELETE }?.let { dialog ->
        val observation = dialog.observation
        AlertDialog(
            onDismissRequest = model::closeDialog,
            title = { Text(stringResource(Res.string.observation_delete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${builtInDiseaseName(observation.diseaseCode, observation.diseaseName)} · ${observation.observedOn}")
                    Text(stringResource(Res.string.observation_delete_warning))
                    state.dialogError?.let { Text(localizedObservationIssue(it), color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = userAction(model::deleteObservation), enabled = !state.saving) {
                    Text(if (state.saving) stringResource(Res.string.deleting_action) else stringResource(Res.string.observation_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = userAction(model::closeDialog), enabled = !state.saving) { Text(stringResource(Res.string.cancel_action)) }
            }
        )
    }

    var diseaseCode by rememberSaveable(ownerId, vineyardId, blockId) { mutableStateOf<String?>(null) }
    var statusName by rememberSaveable(ownerId, vineyardId, blockId) { mutableStateOf<String?>(null) }
    val status = DiseaseOccurrenceStatus.entries.firstOrNull { it.name == statusName }
    val selectedDisease = state.diseases.firstOrNull { it.code != null && it.code == diseaseCode }
    val visibleObservations = remember(state.observations, selectedDisease?.code, status) {
        val selectedCode = selectedDisease?.code
        state.observations.filter { observation ->
            (selectedCode == null || observation.diseaseCode == selectedCode) &&
                (status == null || observation.status == status)
        }
    }

    Scaffold(topBar = { RecordTopBar(stringResource(Res.string.disease_observations), { if (!model.state.value.saving) onBack() }, !state.saving) }) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val error = state.loadError
            when {
                state.loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                error != null -> item {
                    Text(localizedObservationIssue(error))
                    TextButton(onClick = userAction(model::reload)) { Text(stringResource(Res.string.retry)) }
                }
                else -> {
                    if (state.diseases.isEmpty()) item { Text(stringResource(Res.string.observation_no_diseases)) }
                    else item(key = "observation-form") { ObservationForm(state, model::save) }
                    if (state.observations.isEmpty()) {
                        item { Text(stringResource(Res.string.observation_empty)) }
                    } else {
                        item(key = "observation-filters") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(Res.string.observation_filters), style = MaterialTheme.typography.titleMedium)
                                RecordChoice(
                                    label = stringResource(Res.string.disease_label), selected = selectedDisease,
                                    values = state.diseases.filter { !it.code.isNullOrBlank() },
                                    enabled = !state.saving,
                                    text = { diseaseLabels.getValue(it.diseaseId) }, changed = { diseaseCode = it?.code },
                                    nullLabel = stringResource(Res.string.all_diseases)
                                )
                                RecordChoice(
                                    label = stringResource(Res.string.status_label), selected = status,
                                    values = DiseaseOccurrenceStatus.entries, enabled = !state.saving,
                                    text = { statusLabels.getValue(it) }, changed = { statusName = it?.name },
                                    nullLabel = stringResource(Res.string.all_statuses)
                                )
                                if (selectedDisease != null || status != null) {
                                    TextButton(
                                        onClick = userAction {
                                            diseaseCode = null
                                            statusName = null
                                        },
                                        enabled = !state.saving
                                    ) { Text(stringResource(Res.string.clear_filters)) }
                                }
                                if (visibleObservations.isEmpty()) {
                                    Text(stringResource(Res.string.observation_no_matches))
                                }
                            }
                        }
                    }
                    items(visibleObservations, key = { it.id }) { observation ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(builtInDiseaseName(observation.diseaseCode, observation.diseaseName), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(Res.string.observation_summary, observation.observedOn.toString(), observation.severity, localizedOccurrenceStatus(observation.status)))
                                observation.curedAt?.let { Text(stringResource(Res.string.observation_resolved_on, it.toString())) }
                                observation.notes?.let { Text(it) }
                                observation.referenceSymptoms.forEach { recorded ->
                                    Text("${referenceSymptomName(recorded.kind)} · ${recorded.observedOn}")
                                    if (recorded.notes.isNotBlank()) Text(recorded.notes)
                                    TextButton(
                                        onClick = userAction { model.openDialog(observation.id, ObservationDialog.Kind.REFERENCE_SYMPTOM, recorded) },
                                        enabled = !state.saving
                                    ) { Text(stringResource(Res.string.observation_edit_symptom, referenceSymptomName(recorded.kind))) }
                                    TextButton(
                                        onClick = userAction { model.openDialog(observation.id, ObservationDialog.Kind.REMOVE_REFERENCE_SYMPTOM, recorded) },
                                        enabled = !state.saving
                                    ) { Text(stringResource(Res.string.observation_remove_symptom_named, referenceSymptomName(recorded.kind))) }
                                }
                                if (availableSymptoms(observation).isNotEmpty()) {
                                    TextButton(
                                        onClick = userAction { model.openDialog(observation.id, ObservationDialog.Kind.REFERENCE_SYMPTOM) },
                                        enabled = !state.saving
                                    ) { Text(stringResource(Res.string.observation_record_symptom)) }
                                }
                                TextButton(
                                    onClick = userAction { model.openDialog(observation.id, ObservationDialog.Kind.STATUS) },
                                    enabled = !state.saving
                                ) { Text(stringResource(Res.string.observation_change_status)) }
                                TextButton(
                                    onClick = userAction { model.openDialog(observation.id, ObservationDialog.Kind.DETAILS) },
                                    enabled = !state.saving
                                ) { Text(stringResource(Res.string.observation_edit_details)) }
                                state.activityLinks[observation.id].orEmpty().forEach { link ->
                                    Text(
                                        stringResource(Res.string.observation_linked_activity, link.appliedOn.toString(), link.activityType),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (link.description.isNotBlank()) Text(link.description)
                                    TextButton(
                                        onClick = userAction { model.openUnlinkDialog(link) },
                                        enabled = !state.saving
                                    ) { Text(stringResource(Res.string.observation_unlink)) }
                                }
                                TextButton(
                                    onClick = userAction { model.openDialog(observation.id, ObservationDialog.Kind.LINK_ACTIVITY) },
                                    enabled = !state.saving
                                ) { Text(stringResource(Res.string.observation_link_completed)) }
                                TextButton(
                                    onClick = userAction { model.openDialog(observation.id, ObservationDialog.Kind.DELETE) },
                                    enabled = !state.saving
                                ) { Text(stringResource(Res.string.observation_delete), color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ObservationForm(
    state: DiseaseObservationsState,
    onSave: (Long, LocalDate, Int, String) -> Unit
) {
    var diseaseId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dateText by rememberSaveable { mutableStateOf("") }
    var severityText by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var clearedAfterSave by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(state.lastSavedId) {
        if (state.lastSavedId != null && state.lastSavedId != clearedAfterSave) {
            clearedAfterSave = state.lastSavedId
            diseaseId = null
            dateText = ""
            severityText = ""
            notes = ""
        }
    }

    val diseaseLabels = state.diseases.associate { it.diseaseId to builtInDiseaseName(it.code, it.name) }
    val disease = state.diseases.firstOrNull { it.diseaseId == diseaseId }
    val date = parseObservationDate(dateText)
    val severity = parseObservationSeverity(severityText)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.observation_record), style = MaterialTheme.typography.titleMedium)
        RecordChoice(
            label = stringResource(Res.string.disease_label), selected = disease, values = state.diseases, enabled = !state.saving,
            text = { diseaseLabels.getValue(it.diseaseId) }, changed = { diseaseId = it?.diseaseId },
            allowUnknown = false, nullLabel = stringResource(Res.string.choose_disease)
        )
        ObservationFields(
            dateText, severityText, notes, !state.saving,
            { dateText = it }, { severityText = it }, { notes = it }
        )
        Button(
            onClick = userAction {
                if (disease != null && date != null && severity != null) onSave(disease.diseaseId, date, severity, notes)
            },
            enabled = !state.saving && disease != null && date != null && severity != null && notes.trim().length <= 4000
        ) { Text(if (state.saving) stringResource(Res.string.saving_action) else stringResource(Res.string.observation_save_active)) }
        state.saveError?.let { Text(localizedObservationIssue(it), color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun EditObservationDialog(
    observation: BlockDiseaseObservation,
    saving: Boolean,
    error: ObservationIssue?,
    onClose: () -> Unit,
    onSave: (LocalDate, Int, String) -> Unit
) {
    var dateText by rememberSaveable(observation.id, observation.updatedAt) {
        mutableStateOf(observation.observedOn.toString())
    }
    var severityText by rememberSaveable(observation.id, observation.updatedAt) {
        mutableStateOf(observation.severity.toString())
    }
    var notes by rememberSaveable(observation.id, observation.updatedAt) {
        mutableStateOf(observation.notes.orEmpty())
    }
    val date = parseObservationDate(dateText)
    val severity = parseObservationSeverity(severityText)
    val changed = date != observation.observedOn || severity != observation.severity ||
        notes.trim().ifBlank { null } != observation.notes
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(Res.string.observation_edit)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(builtInDiseaseName(observation.diseaseCode, observation.diseaseName))
                observation.curedAt?.let { Text(stringResource(Res.string.observation_resolved_on, it.toString())) }
                ObservationFields(
                    dateText, severityText, notes, !saving,
                    { dateText = it }, { severityText = it }, { notes = it }
                )
                error?.let { Text(localizedObservationIssue(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = userAction {
                    if (date != null && severity != null) onSave(date, severity, notes)
                },
                enabled = !saving && changed && date != null && severity != null && notes.trim().length <= 4000
            ) { Text(if (saving) stringResource(Res.string.saving_action) else stringResource(Res.string.save_changes)) }
        },
        dismissButton = {
            TextButton(onClick = userAction(onClose), enabled = !saving) { Text(stringResource(Res.string.cancel_action)) }
        }
    )
}

private fun availableSymptoms(observation: BlockDiseaseObservation): List<ReferenceSymptom> =
    ReferenceSymptom.forDisease(observation.diseaseCode).filter { kind ->
        observation.referenceSymptoms.none { it.kind == kind }
    }

@Composable
private fun ReferenceSymptomDialog(
    observation: BlockDiseaseObservation,
    existing: ObservedReferenceSymptom?,
    saving: Boolean,
    error: ObservationIssue?,
    onClose: () -> Unit,
    onSave: (ReferenceSymptom, LocalDate, String) -> Unit
) {
    val symptomLabels = ReferenceSymptom.entries.associateWith { referenceSymptomName(it) }
    val choices = if (existing == null) availableSymptoms(observation) else listOf(existing.kind)
    var selectedCode by rememberSaveable(observation.id, existing?.symptomId) {
        mutableStateOf(choices.singleOrNull()?.code)
    }
    val symptom = choices.firstOrNull { it.code == selectedCode }
    var dateText by rememberSaveable(observation.id, existing?.symptomId) {
        mutableStateOf(existing?.observedOn?.toString().orEmpty())
    }
    var notes by rememberSaveable(observation.id, existing?.symptomId) {
        mutableStateOf(existing?.notes.orEmpty())
    }
    val date = parseObservationDate(dateText)
    val activity = LocalUserActivity.current
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (existing == null) stringResource(Res.string.observation_record_symptom) else stringResource(Res.string.observation_edit_symptom, referenceSymptomName(existing.kind))) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (existing == null) {
                    RecordChoice(
                        label = stringResource(Res.string.symptom_label), selected = symptom, values = choices, enabled = !saving,
                        text = { symptomLabels.getValue(it) }, changed = { selectedCode = it?.code },
                        allowUnknown = false, nullLabel = stringResource(Res.string.choose_symptom)
                    )
                }
                Text(stringResource(Res.string.symptom_date_help))
                OutlinedTextField(
                    value = dateText, onValueChange = { activity(); dateText = it },
                    label = { Text(stringResource(Res.string.symptom_date_label)) }, enabled = !saving,
                    singleLine = true, isError = dateText.isNotBlank() && date == null,
                    supportingText = { if (dateText.isNotBlank() && date == null) Text(stringResource(Res.string.observation_error_date_format)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes, onValueChange = { activity(); notes = it },
                    label = { Text(stringResource(Res.string.notes_optional)) }, enabled = !saving, maxLines = 5,
                    isError = notes.trim().length > 4000,
        supportingText = { if (notes.trim().length > 4000) Text(stringResource(Res.string.disease_error_notes)) }, modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(localizedObservationIssue(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = userAction { if (symptom != null && date != null) onSave(symptom, date, notes) },
                enabled = !saving && symptom != null && date != null && notes.trim().length <= 4000
            ) { Text(if (saving) stringResource(Res.string.saving_action) else stringResource(Res.string.symptom_save)) }
        },
        dismissButton = {
            TextButton(onClick = userAction(onClose), enabled = !saving) { Text(stringResource(Res.string.cancel_action)) }
        }
    )
}

@Composable
private fun LinkActivityDialog(
    activities: List<CompletedBlockActivity>,
    saving: Boolean,
    error: ObservationIssue?,
    onClose: () -> Unit,
    onSave: (CompletedBlockActivity) -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = activities.firstOrNull { it.activityId == selectedId }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(Res.string.observation_link_completed)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (activities.isEmpty()) {
                    Text(stringResource(Res.string.no_completed_activity_links))
                } else {
                    RecordChoice(
                        label = stringResource(Res.string.activity_label),
                        selected = selected,
                        values = activities,
                        enabled = !saving,
                        text = { activity ->
                            val description = activity.description.take(60)
                            "${activity.completedOn} · ${activity.activityType}" +
                                if (description.isBlank()) "" else " · $description"
                        },
                        changed = { selectedId = it?.activityId },
                        allowUnknown = false,
                        nullLabel = stringResource(Res.string.choose_completed_activity)
                    )
                    selected?.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                error?.let { Text(localizedObservationIssue(it), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = userAction { selected?.let(onSave) },
                enabled = !saving && selected != null
            ) { Text(if (saving) stringResource(Res.string.linking_action) else stringResource(Res.string.link_activity_action)) }
        },
        dismissButton = {
            TextButton(onClick = userAction(onClose), enabled = !saving) { Text(stringResource(Res.string.cancel_action)) }
        }
    )
}

@Composable
private fun ObservationFields(
    dateText: String,
    severityText: String,
    notes: String,
    enabled: Boolean,
    onDateChanged: (String) -> Unit,
    onSeverityChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit
) {
    val activity = LocalUserActivity.current
    OutlinedTextField(
        value = dateText, onValueChange = { activity(); onDateChanged(it) },
        label = { Text(stringResource(Res.string.observation_date_label)) },
        enabled = enabled, singleLine = true,
        isError = dateText.isNotBlank() && parseObservationDate(dateText) == null,
        supportingText = { if (dateText.isNotBlank() && parseObservationDate(dateText) == null) Text(stringResource(Res.string.observation_error_date_format)) },
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = severityText, onValueChange = { activity(); onSeverityChanged(it) },
        label = { Text(stringResource(Res.string.observation_severity_label)) },
        enabled = enabled, singleLine = true,
        isError = severityText.isNotBlank() && parseObservationSeverity(severityText) == null,
        supportingText = { if (severityText.isNotBlank() && parseObservationSeverity(severityText) == null) Text(stringResource(Res.string.disease_error_severity)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = notes, onValueChange = { activity(); onNotesChanged(it) },
        label = { Text(stringResource(Res.string.notes_optional)) }, enabled = enabled, maxLines = 5,
        isError = notes.trim().length > 4000,
        supportingText = { if (notes.trim().length > 4000) Text(stringResource(Res.string.disease_error_notes)) },
        modifier = Modifier.fillMaxWidth()
    )
}

private fun parseObservationDate(text: String): LocalDate? = try {
    LocalDate.parse(text.trim())
} catch (_: IllegalArgumentException) {
    null
}

private fun parseObservationSeverity(text: String): Int? =
    text.trim().toIntOrNull()?.takeIf { it in 0..100 }

