package com.rncoding.testvineshield.core.presentation.disease_observations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                    if (state.observations.isEmpty()) item { Text("No observations for built-in diseases in this block.") }
                    items(state.observations, key = { it.id }) { observation ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(observation.diseaseName, style = MaterialTheme.typography.titleMedium)
                                Text("${observation.observedOn} · ${observation.severity}% affected · ${observation.status.dbValue}")
                                observation.notes?.let { Text(it) }
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
    val activity = LocalUserActivity.current
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
    val date = try { LocalDate.parse(dateText.trim()) } catch (_: IllegalArgumentException) { null }
    val severity = severityText.trim().toIntOrNull()?.takeIf { it in 0..100 }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Record an observation", style = MaterialTheme.typography.titleMedium)
        RecordChoice(
            label = "Disease", selected = disease, values = state.diseases, enabled = !state.saving,
            text = { it.name }, changed = { diseaseId = it?.diseaseId },
            allowUnknown = false, nullLabel = "Choose a disease"
        )
        OutlinedTextField(
            value = dateText, onValueChange = { activity(); dateText = it },
            label = { Text("Observation date (YYYY-MM-DD)") },
            enabled = !state.saving, singleLine = true,
            isError = dateText.isNotBlank() && date == null,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = severityText, onValueChange = { activity(); severityText = it },
            label = { Text("Affected tissue or area (%)") },
            enabled = !state.saving, singleLine = true,
            isError = severityText.isNotBlank() && severity == null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = notes, onValueChange = { activity(); notes = it },
            label = { Text("Notes (optional)") }, enabled = !state.saving,
            isError = notes.trim().length > 4000,
            modifier = Modifier.fillMaxWidth()
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
