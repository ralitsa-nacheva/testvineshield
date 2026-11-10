package com.rncoding.testvineshield.core.presentation.spray

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
import com.rncoding.testvineshield.core.domain.usecases.activity.SprayKey
import com.rncoding.testvineshield.core.presentation.components.*
import com.rncoding.testvineshield.core.presentation.formatting.formatPlainDecimal
import com.rncoding.testvineshield.core.presentation.root.*
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val spraySaver = listSaver<ArrayList<String>, String>(save = { it.toList() }, restore = { ArrayList(it) })

@Composable
fun SprayScreenRoot(ownerId: Long, key: SprayKey, onBack: () -> Unit, onFinished: () -> Unit, onBusyChanged: (Boolean) -> Unit = {}, onExport: () -> Unit = {}) {
    var savedDraft by rememberSaveable(ownerId, key.vineyardId, key.activityId, stateSaver = spraySaver) { mutableStateOf(arrayListOf<String>()) }
    val vm = koinViewModel<SprayViewModel>(key = "spray-$ownerId-${key.vineyardId}-${key.activityId}", parameters = { parametersOf(SprayRequest(key, SprayDraft.restore(savedDraft))) })
    val state by vm.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refreshIfViewing() }
    LaunchedEffect(state.draft, state.editing, state.saved, state.removed) {
        if (state.saved || state.removed) { savedDraft = arrayListOf(); onFinished() }
        else if (state.snapshot != null) savedDraft = if (state.editing) state.draft.savedValues() else arrayListOf()
    }
    DisposableEffect(state.saving, state.deleting) {
        onBusyChanged(state.saving || state.deleting)
        onDispose { onBusyChanged(false) }
    }
    val activity = LocalUserActivity.current
    Scaffold(topBar = { RecordTopBar("Spray record", { if (!vm.state.value.busy) onBack() }, !state.busy) }) { padding ->
        LazyColumn(Modifier.padding(padding).imePadding().fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error -> item {
                Text(error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = userAction { vm.load(discardDraft = true) }, enabled = !state.busy) { Text(if (state.editing) "Reload saved record (discard draft)" else "Retry") }
            } }
            state.snapshot?.let { snapshot ->
                item { Text(snapshot.activity.activityType, style = MaterialTheme.typography.headlineSmall); Text("Season: ${snapshot.activity.season}") }
                item { Text("This records what was applied. It does not recommend products/doses or confirm legal approval. Saving does not change activity status or completion date.") }
                val old = snapshot.activity
                if (old.treatmentProduct != null || old.treatmentProductQuantity != null || old.waterQuantity != null || old.treatmentProductRate != null) item {
                    Text("Prior single-product fields remain unchanged in Activity details. They are not automatically copied here; their water/rate units may be unknown.")
                }
                if (!state.editing) {
                    val record = snapshot.record
                    if (record != null) {
                        item { Text("Applied: ${record.applicationDate} · ${record.localTime ?: "time unknown"} · ${record.timeZone}") }
                        item { Text("Treated area: ${amount(record.treatedArea, record.areaUnit?.symbol)}") }
                        item { Text("Carrier water: ${amount(record.waterQuantity, record.waterUnit?.label)}") }
                        record.products.forEachIndexed { index, line -> item {
                            OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                                Text("${index + 1}. ${line.productName}", style = MaterialTheme.typography.titleMedium)
                                Text("Quantity: ${amount(line.quantity, line.unit?.label)}")
                                Text("Label/reference as recorded: ${line.labelReference ?: "Not recorded"}")
                                Text("Recorded rate: ${line.rate?.let { rate -> amount(rate.quantity, rate.productUnit.label) + " per " + amount(rate.basisQuantity, rate.basis.label) } ?: "Unknown"}")
                            } }
                        } }
                        item { Text("Notes: ${record.notes ?: "Not recorded"}") }
                        item { OutlinedButton(onClick = userAction(onExport), enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Export saved spray application") } }
                        item { Button(onClick = userAction(vm::edit), enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Edit spray record") } }
                        item { OutlinedButton(onClick = userAction(vm::requestDelete), enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Delete spray record") } }
                    }
                } else {
                    item { Text("Save to keep changes. Back discards unsaved changes. Quantities may be unknown; a known quantity requires its unit. Product lines describe one application, not a recommended tank mix.") }
                    SprayField.entries.forEach { field -> item {
                        val label = when (field) { SprayField.Date -> "Actual application date *"; SprayField.Time -> "Local time (HH:mm, optional)"; SprayField.Area -> "Treated area (optional)"; SprayField.Water -> "Carrier water quantity (optional)"; SprayField.Notes -> "Notes (up to 4000 characters)" }
                        val error = state.errors[field.name.lowercase()]
                        OutlinedTextField(value = state.draft.text(field), onValueChange = { activity(); vm.text(field, it) }, enabled = state.canEdit,
                            modifier = Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = field != SprayField.Notes, isError = error != null,
                            supportingText = { error?.let { Text(it) } }, keyboardOptions = KeyboardOptions(keyboardType = when (field) { SprayField.Date, SprayField.Time -> KeyboardType.Ascii; SprayField.Area, SprayField.Water -> KeyboardType.Decimal; else -> KeyboardType.Text }))
                        if (field == SprayField.Area) {
                            RecordChoice("Treated-area unit", state.draft.areaUnit, AreaUnit.entries, state.canEdit, { it.symbol }, vm::areaUnit)
                            state.errors["areaUnit"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                        if (field == SprayField.Water) {
                            RecordChoice("Carrier-water unit", state.draft.waterUnit, SprayQuantityUnit.entries.filter { it.volume }, state.canEdit, { it.label }, vm::waterUnit)
                            state.errors["waterUnit"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    } }
                    item { TimeZonePicker(state.draft.zone, null, state.canEdit, vm::zone); Text("Initially uses the vineyard time zone; choose the zone that applied to this application. Unknown local time remains blank; no UTC instant is inferred.") }
                    state.draft.lines.forEachIndexed { index, line -> item {
                        SprayLineInputs(line, index, state, vm)
                    } }
                    item { OutlinedButton(onClick = userAction(vm::addLine), enabled = state.canEdit && state.draft.lines.size < 20) { Text("Add product line") } }
                    item { Text("Changing a unit only relabels the number. No conversion, dose-rate calculation or label check is performed. Export uses saved records only.") }
                    item { Button(onClick = userAction { vm.save(); onBusyChanged(vm.state.value.saving) }, enabled = state.canEdit, modifier = Modifier.fillMaxWidth()) { Text("Save spray record") } }
                }
            }
        }
    }
    if (state.confirmDelete) AlertDialog(modifier = Modifier.userActivity(activity), onDismissRequest = userAction(vm::cancelDelete), title = { Text("Delete spray record?") },
        text = { Text("This removes the structured application and all its product lines. The activity, its prior treatment fields and disease links remain. This cannot be undone.") },
        confirmButton = { TextButton(onClick = userAction { vm.confirmDelete(); onBusyChanged(vm.state.value.deleting) }, enabled = !state.busy) { Text("Delete permanently") } },
        dismissButton = { TextButton(onClick = userAction(vm::cancelDelete), enabled = !state.busy) { Text("Cancel") } })
}

private fun amount(value: Double?, unit: String?): String = value?.let { "${if (it.isFinite()) formatPlainDecimal(it) else it.toString()} ${unit ?: "(unit unknown)"}" } ?: "Unknown"
