package com.rncoding.testvineshield.core.presentation.disease_observations

import androidx.compose.foundation.layout.*
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
            title = { Text(observation.diseaseName) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Observed ${observation.observedOn} · ${observation.severity}% affected")
                    Text("Current status: ${observation.status.label()}")
                    observation.curedAt?.let { Text("Resolved on $it") }
                    observation.notes?.let { Text(it, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
                    RecordChoice(
                        label = "New status", selected = selected,
                        values = DiseaseOccurrenceStatus.entries, enabled = !state.saving,
                        text = { it.label() }, changed = { if (it != null) selected = it },
                        allowUnknown = false
                    )
                    if (selected == DiseaseOccurrenceStatus.RESOLVED && selected != observation.status) {
                        Text("Resolution is dated today in the vineyard’s time zone.")
                    }
                    if (selected != DiseaseOccurrenceStatus.ACTIVE && selected != observation.status) {
                        Text("Only active observations are included in active disease summaries.")
                    }
                    state.dialogError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = userAction { model.saveStatus(selected) },
                    enabled = !state.saving && selected != observation.status
                ) { Text(if (state.saving) "Saving…" else "Save status") }
            },
            dismissButton = {
                TextButton(onClick = userAction(model::closeDialog), enabled = !state.saving) { Text("Cancel") }
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
            title = { Text("Remove recorded symptom?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Remove the symptom recorded on ${referenceSymptom.observedOn}.")
                    state.dialogError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = userAction(model::removeReferenceSymptom), enabled = !state.saving) {
                    Text(if (state.saving) "Removing…" else "Remove symptom")
                }
            },
            dismissButton = {
                TextButton(onClick = userAction(model::closeDialog), enabled = !state.saving) { Text("Cancel") }
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
            title = { Text("Remove activity link?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${link.appliedOn} · ${link.activityType}")
                    Text("The activity will remain in the activity notebook.")
                    state.dialogError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = userAction(model::removeActivityLink), enabled = !state.saving) {
                    Text(if (state.saving) "Removing…" else "Remove link")
                }
            },
            dismissButton = {
                TextButton(onClick = userAction(model::closeDialog), enabled = !state.saving) { Text("Cancel") }
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

    Scaffold(topBar = { RecordTopBar("Disease observations", { if (!model.state.value.saving) onBack() }, !state.saving) }) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val error = state.loadError
            when {
                state.loading -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                error != null -> item {
                    Text(error)
                    TextButton(onClick = userAction(model::reload)) { Text("Retry") }
                }
                else -> {
                    if (state.diseases.isEmpty()) item { Text("No built-in diseases are available.") }
                    else item(key = "observation-form") { ObservationForm(state, model::save) }
                    if (state.observations.isEmpty()) {
                        item { Text("No observations for built-in diseases in this block.") }
                    } else {
                        item(key = "observation-filters") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Filter observations", style = MaterialTheme.typography.titleMedium)
                                RecordChoice(
                                    label = "Disease", selected = selectedDisease,
                                    values = state.diseases.filter { !it.code.isNullOrBlank() },
                                    enabled = !state.saving,
                                    text = { it.name }, changed = { diseaseCode = it?.code },
                                    nullLabel = "All diseases"
                                )
                                RecordChoice(
                                    label = "Status", selected = status,
                                    values = DiseaseOccurrenceStatus.entries, enabled = !state.saving,
                                    text = { it.label() }, changed = { statusName = it?.name },
                                    nullLabel = "All statuses"
                                )
                                if (selectedDisease != null || status != null) {
                                    TextButton(
                                        onClick = userAction {
                                            diseaseCode = null
                                            statusName = null
                                        },
                                        enabled = !state.saving
                                    ) { Text("Clear filters") }
                                }
                                if (visibleObservations.isEmpty()) {
                                    Text("No observations match these filters.")
                                }
                            }
                        }
                    }
                    items(visibleObservations, key = { it.id }) { observation ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(observation.diseaseName, style = MaterialTheme.typography.titleMedium)
                                Text("${observation.observedOn} · ${observation.severity}% affected · ${observation.status.label()}")
                                observation.curedAt?.let { Text("Resolved on $it") }
                                observation.notes?.let { Text(it) }
                                observation.referenceSymptoms.forEach { recorded ->
                                    Text("${recorded.kind.label} · ${recorded.observedOn}")
                                    if (recorded.notes.isNotBlank()) Text(recorded.notes)
                                    TextButton(
                                        onClick = userAction { model.openDialog(observation.id, ObservationDialog.Kind.REFERENCE_SYMPTOM, recorded) },
                                        enabled = !state.saving
                                    ) { Text("Edit ${recorded.kind.label.lowercase()}") }
                                    TextButton(
                                        onClick = userAction { model.openDialog(observation.id, ObservationDialog.Kind.REMOVE_REFERENCE_SYMPTOM, recorded) },
                                        enabled = !state.saving
                                    ) { Text("Remove ${recorded.kind.label.lowercase()}") }
                                }
                                if (availableSymptoms(observation).isNotEmpty()) {
                                    TextButton(
                                        onClick = userAction { model.openDialog(observation.id, ObservationDialog.Kind.REFERENCE_SYMPTOM) },
                                        enabled = !state.saving
                                    ) { Text("Record symptom") }
                                }
                                TextButton(
                                    onClick = userAction { model.openDialog(observation.id, ObservationDialog.Kind.STATUS) },
                                    enabled = !state.saving
                                ) { Text("Change status") }
                                TextButton(
                                    onClick = userAction { model.openDialog(observation.id, ObservationDialog.Kind.DETAILS) },
                                    enabled = !state.saving
                                ) { Text("Edit details") }
                                state.activityLinks[observation.id].orEmpty().forEach { link ->
                                    Text(
                                        "Linked activity · ${link.appliedOn} · ${link.activityType}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (link.description.isNotBlank()) Text(link.description)
                                    TextButton(
                                        onClick = userAction { model.openUnlinkDialog(link) },
                                        enabled = !state.saving
                                    ) { Text("Remove activity link") }
                                }
                                TextButton(
                                    onClick = userAction { model.openDialog(observation.id, ObservationDialog.Kind.LINK_ACTIVITY) },
                                    enabled = !state.saving
                                ) { Text("Link completed activity") }
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

    val disease = state.diseases.firstOrNull { it.diseaseId == diseaseId }
    val date = parseObservationDate(dateText)
    val severity = parseObservationSeverity(severityText)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Record an observation", style = MaterialTheme.typography.titleMedium)
        RecordChoice(
            label = "Disease", selected = disease, values = state.diseases, enabled = !state.saving,
            text = { it.name }, changed = { diseaseId = it?.diseaseId },
            allowUnknown = false, nullLabel = "Choose a disease"
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
        ) { Text(if (state.saving) "Saving…" else "Save active observation") }
        state.saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun EditObservationDialog(
    observation: BlockDiseaseObservation,
    saving: Boolean,
    error: String?,
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
        title = { Text("Edit observation") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(observation.diseaseName)
                observation.curedAt?.let { Text("Resolved on $it") }
                ObservationFields(
                    dateText, severityText, notes, !saving,
                    { dateText = it }, { severityText = it }, { notes = it }
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = userAction {
                    if (date != null && severity != null) onSave(date, severity, notes)
                },
                enabled = !saving && changed && date != null && severity != null && notes.trim().length <= 4000
            ) { Text(if (saving) "Saving…" else "Save changes") }
        },
        dismissButton = {
            TextButton(onClick = userAction(onClose), enabled = !saving) { Text("Cancel") }
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
    error: String?,
    onClose: () -> Unit,
    onSave: (ReferenceSymptom, LocalDate, String) -> Unit
) {
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
        title = { Text(if (existing == null) "Record symptom" else "Edit ${existing.kind.label.lowercase()}") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (existing == null) {
                    RecordChoice(
                        label = "Symptom", selected = symptom, values = choices, enabled = !saving,
                        text = { it.label }, changed = { selectedCode = it?.code },
                        allowUnknown = false, nullLabel = "Choose a symptom"
                    )
                }
                Text("Enter the date you saw the symptom on the vine.")
                OutlinedTextField(
                    value = dateText, onValueChange = { activity(); dateText = it },
                    label = { Text("Symptom date (YYYY-MM-DD)") }, enabled = !saving,
                    singleLine = true, isError = dateText.isNotBlank() && date == null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes, onValueChange = { activity(); notes = it },
                    label = { Text("Notes (optional)") }, enabled = !saving, maxLines = 5,
                    isError = notes.trim().length > 4000, modifier = Modifier.fillMaxWidth()
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = userAction { if (symptom != null && date != null) onSave(symptom, date, notes) },
                enabled = !saving && symptom != null && date != null && notes.trim().length <= 4000
            ) { Text(if (saving) "Saving…" else "Save symptom") }
        },
        dismissButton = {
            TextButton(onClick = userAction(onClose), enabled = !saving) { Text("Cancel") }
        }
    )
}

@Composable
private fun LinkActivityDialog(
    activities: List<CompletedBlockActivity>,
    saving: Boolean,
    error: String?,
    onClose: () -> Unit,
    onSave: (CompletedBlockActivity) -> Unit
) {
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected = activities.firstOrNull { it.activityId == selectedId }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Link completed activity") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (activities.isEmpty()) {
                    Text("There are no unlinked completed activities with a completion date in this block.")
                } else {
                    RecordChoice(
                        label = "Activity",
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
                        nullLabel = "Choose a completed activity"
                    )
                    selected?.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = userAction { selected?.let(onSave) },
                enabled = !saving && selected != null
            ) { Text(if (saving) "Linking…" else "Link activity") }
        },
        dismissButton = {
            TextButton(onClick = userAction(onClose), enabled = !saving) { Text("Cancel") }
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
        label = { Text("Observation date (YYYY-MM-DD)") },
        enabled = enabled, singleLine = true,
        isError = dateText.isNotBlank() && parseObservationDate(dateText) == null,
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = severityText, onValueChange = { activity(); onSeverityChanged(it) },
        label = { Text("Affected tissue or area (%)") },
        enabled = enabled, singleLine = true,
        isError = severityText.isNotBlank() && parseObservationSeverity(severityText) == null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = notes, onValueChange = { activity(); onNotesChanged(it) },
        label = { Text("Notes (optional)") }, enabled = enabled, maxLines = 5,
        isError = notes.trim().length > 4000,
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

private fun DiseaseOccurrenceStatus.label(): String = when (this) {
    DiseaseOccurrenceStatus.ACTIVE -> "Active"
    DiseaseOccurrenceStatus.MONITORING -> "Monitoring"
    DiseaseOccurrenceStatus.RESOLVED -> "Resolved"
}
