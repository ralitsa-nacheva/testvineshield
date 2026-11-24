package com.rncoding.testvineshield.core.presentation.spray

import androidx.compose.foundation.layout.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
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
    Scaffold(topBar = { RecordTopBar(stringResource(Res.string.spray_title), { if (!vm.state.value.busy) onBack() }, !state.busy) }) { padding ->
        LazyColumn(Modifier.padding(padding).imePadding().fillMaxHeight().widthIn(max = 720.dp).fillMaxWidth(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.busy) item(key = "spray-progress", contentType = "spray-progress") { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error -> item(key = "spray-error", contentType = "spray-error") {
                Text(localizedSprayIssue(error), color = MaterialTheme.colorScheme.error)
                TextButton(onClick = userAction { vm.load(discardDraft = true) }, enabled = !state.busy) { Text(if (state.editing) stringResource(Res.string.spray_reload) else stringResource(Res.string.retry)) }
            } }
            state.snapshot?.let { snapshot ->
                item(key = "spray-heading", contentType = "spray-heading") { Text(snapshot.activity.activityType, style = MaterialTheme.typography.headlineSmall); Text(stringResource(Res.string.spray_season, snapshot.activity.season)) }
                item(key = "spray-purpose", contentType = "spray-purpose") { Text(stringResource(Res.string.spray_purpose)) }
                val old = snapshot.activity
                if (old.treatmentProduct != null || old.treatmentProductQuantity != null || old.waterQuantity != null || old.treatmentProductRate != null) item(key = "spray-legacy", contentType = "spray-legacy") {
                    Text(stringResource(Res.string.spray_legacy_help))
                }
                if (!state.editing) {
                    val record = snapshot.record
                    if (record != null) {
                        item(key = "spray-view-date", contentType = "spray-view-date") { Text(stringResource(Res.string.spray_applied, record.applicationDate.toString(), record.localTime ?: stringResource(Res.string.spray_time_unknown), record.timeZone)) }
                        item(key = "spray-view-area", contentType = "spray-view-area") { Text(stringResource(Res.string.spray_area, amount(record.treatedArea, record.areaUnit?.symbol))) }
                        item(key = "spray-view-water", contentType = "spray-view-water") { Text(stringResource(Res.string.spray_water, amount(record.waterQuantity, record.waterUnit?.label))) }
                        record.products.forEachIndexed { index, line -> item(key = "spray-view-product-${line.lineId}-$index", contentType = "spray-view-product") {
                            OutlinedCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                                Text("${index + 1}. ${line.productName}", style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(Res.string.spray_quantity, amount(line.quantity, line.unit?.label)))
                                Text(stringResource(Res.string.spray_label, line.labelReference ?: stringResource(Res.string.not_recorded)))
                                Text(stringResource(Res.string.spray_recorded_rate, line.rate?.let { rate -> stringResource(Res.string.spray_rate_value, amount(rate.quantity, rate.productUnit.label), amount(rate.basisQuantity, rateBasisText(rate.basis))) } ?: stringResource(Res.string.unknown_value)))
                            } }
                        } }
                        item(key = "spray-view-notes", contentType = "spray-view-notes") { Text(stringResource(Res.string.spray_notes_value, record.notes ?: stringResource(Res.string.not_recorded))) }
                        item(key = "spray-export", contentType = "spray-export") { OutlinedButton(onClick = userAction(onExport), enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.spray_export_saved)) } }
                        item(key = "spray-edit", contentType = "spray-edit") { Button(onClick = userAction(vm::edit), enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.spray_edit)) } }
                        item(key = "spray-delete", contentType = "spray-delete") { OutlinedButton(onClick = userAction(vm::requestDelete), enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.spray_delete)) } }
                    }
                } else {
                    item(key = "spray-edit-help", contentType = "spray-edit-help") { Text(stringResource(Res.string.spray_save_help)) }
                    // Keep each field identity and reuse type stable when banners change list positions.
                    // Notes is multiline; its remembered scroller must never be reused by a single-line field.
                    SprayField.entries.forEach { field -> item(key = "spray-field-${field.name}", contentType = "spray-field-${field.name}") {
                        val label = when (field) { SprayField.Date -> stringResource(Res.string.spray_actual_date); SprayField.Time -> stringResource(Res.string.spray_local_time); SprayField.Area -> stringResource(Res.string.spray_area_optional); SprayField.Water -> stringResource(Res.string.spray_water_optional); SprayField.Notes -> stringResource(Res.string.spray_notes) }
                        val error = state.errors[field.name.lowercase()]
                        OutlinedTextField(value = state.draft.text(field), onValueChange = { activity(); vm.text(field, it) }, enabled = state.canEdit,
                            modifier = Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = field != SprayField.Notes, isError = error != null,
                            supportingText = { error?.let { Text(localizedSprayDraftIssue(it)) } }, keyboardOptions = KeyboardOptions(keyboardType = when (field) { SprayField.Date, SprayField.Time -> KeyboardType.Ascii; SprayField.Area, SprayField.Water -> KeyboardType.Decimal; else -> KeyboardType.Text }))
                        if (field == SprayField.Area) {
                            RecordChoice(stringResource(Res.string.spray_area_unit), state.draft.areaUnit, AreaUnit.entries, state.canEdit, { it.symbol }, vm::areaUnit, nullLabel = stringResource(Res.string.unit_unspecified))
                            state.errors["areaUnit"]?.let { Text(localizedSprayDraftIssue(it), color = MaterialTheme.colorScheme.error) }
                        }
                        if (field == SprayField.Water) {
                            RecordChoice(stringResource(Res.string.spray_water_unit), state.draft.waterUnit, SprayQuantityUnit.entries.filter { it.volume }, state.canEdit, { it.label }, vm::waterUnit, nullLabel = stringResource(Res.string.unit_unspecified))
                            state.errors["waterUnit"]?.let { Text(localizedSprayDraftIssue(it), color = MaterialTheme.colorScheme.error) }
                        }
                    } }
                    item(key = "spray-zone", contentType = "spray-zone") { TimeZonePicker(state.draft.zone, null, state.canEdit, vm::zone); Text(stringResource(Res.string.spray_zone_help)) }
                    state.draft.lines.forEachIndexed { index, line -> item(key = "spray-edit-product-${line.id}-$index", contentType = "spray-edit-product") {
                        SprayLineInputs(line, index, state, vm)
                    } }
                    item(key = "spray-add-product", contentType = "spray-add-product") { OutlinedButton(onClick = userAction(vm::addLine), enabled = state.canEdit && state.draft.lines.size < 20) { Text(stringResource(Res.string.spray_add_product)) } }
                    item(key = "spray-unit-help", contentType = "spray-unit-help") { Text(stringResource(Res.string.spray_unit_help)) }
                    item(key = "spray-save", contentType = "spray-save") { Button(onClick = userAction { vm.save(); onBusyChanged(vm.state.value.saving) }, enabled = state.canEdit, modifier = Modifier.fillMaxWidth()) { Text(stringResource(Res.string.spray_save)) } }
                }
            }
        }
    }
    if (state.confirmDelete) AlertDialog(modifier = Modifier.userActivity(activity), onDismissRequest = userAction(vm::cancelDelete), title = { Text(stringResource(Res.string.spray_delete_title)) },
        text = { Text(stringResource(Res.string.spray_delete_warning)) },
        confirmButton = { TextButton(onClick = userAction { vm.confirmDelete(); onBusyChanged(vm.state.value.deleting) }, enabled = !state.busy) { Text(stringResource(Res.string.delete_record)) } },
        dismissButton = { TextButton(onClick = userAction(vm::cancelDelete), enabled = !state.busy) { Text(stringResource(Res.string.cancel_action)) } })
}

@Composable
private fun amount(value: Double?, unit: String?): String = value?.let { "${if (it.isFinite()) formatPlainDecimal(it) else it.toString()} ${unit ?: stringResource(Res.string.unknown_unit)}" } ?: stringResource(Res.string.unknown_value)

@Composable
internal fun rateBasisText(basis: SprayRateBasis): String {
    val symbol = when (basis) {
        SprayRateBasis.SQUARE_METRE -> "m²"
        SprayRateBasis.HECTARE -> "ha"
        SprayRateBasis.ACRE -> "acre"
        SprayRateBasis.MILLILITRE -> "mL"
        SprayRateBasis.LITRE -> "L"
        SprayRateBasis.CUBIC_METRE -> "m³"
        SprayRateBasis.US_GALLON -> "US gal"
        SprayRateBasis.IMPERIAL_GALLON -> "Imperial gal"
    }
    return stringResource(when (basis) {
        SprayRateBasis.SQUARE_METRE, SprayRateBasis.HECTARE, SprayRateBasis.ACRE -> Res.string.spray_basis_area
        else -> Res.string.spray_basis_water
    }, symbol)
}
