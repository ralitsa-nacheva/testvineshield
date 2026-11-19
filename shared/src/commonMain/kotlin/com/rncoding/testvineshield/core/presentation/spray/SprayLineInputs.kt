package com.rncoding.testvineshield.core.presentation.spray

import androidx.compose.foundation.layout.*
import com.rncoding.testvineshield.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.presentation.components.RecordChoice
import com.rncoding.testvineshield.core.presentation.root.LocalUserActivity
import com.rncoding.testvineshield.core.presentation.root.userAction

@Composable
internal fun SprayLineInputs(line: SprayLineDraft, index: Int, state: SprayState, vm: SprayViewModel) {
    val bases = SprayRateBasis.entries.associateWith { rateBasisText(it) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.spray_product_index, index + 1), style = MaterialTheme.typography.titleMedium)
            SprayLineFieldInput(index, state, vm, SprayLineField.Name, line.name, stringResource(Res.string.spray_product_name))
            SprayLineFieldInput(index, state, vm, SprayLineField.Quantity, line.quantity, stringResource(Res.string.spray_product_quantity), true)
            RecordChoice(stringResource(Res.string.spray_quantity_unit), line.unit, SprayQuantityUnit.entries, state.canEdit, { it.label }, { vm.lineUnit(index, it) }, nullLabel = stringResource(Res.string.unit_unspecified))
            state.errors["line-$index-unit"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            SprayLineFieldInput(index, state, vm, SprayLineField.Reference, line.reference, stringResource(Res.string.spray_reference))
            Text(stringResource(Res.string.spray_rate_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(Res.string.spray_rate_help))
            SprayLineFieldInput(index, state, vm, SprayLineField.RateQuantity, line.rateQuantity, stringResource(Res.string.spray_rate_quantity), true)
            RecordChoice(stringResource(Res.string.spray_rate_unit), line.rateUnit, SprayQuantityUnit.entries, state.canEdit, { it.label }, { vm.lineRateUnit(index, it) }, nullLabel = stringResource(Res.string.unit_unspecified))
            state.errors["line-$index-rateunit"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            SprayLineFieldInput(index, state, vm, SprayLineField.RateBasisQuantity, line.rateBasisQuantity, stringResource(Res.string.spray_basis_quantity), true)
            RecordChoice(stringResource(Res.string.spray_basis_unit), line.rateBasis, SprayRateBasis.entries, state.canEdit, { bases.getValue(it) }, { vm.lineRateBasis(index, it) }, nullLabel = stringResource(Res.string.unit_unspecified))
            state.errors["line-$index-ratebasis"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = userAction { vm.clearRate(index) }, enabled = state.canEdit) { Text(stringResource(Res.string.spray_clear_rate)) }
            OutlinedButton(onClick = userAction { vm.removeLine(index) }, enabled = state.canEdit && state.draft.lines.size > 1) { Text(stringResource(Res.string.spray_remove_index, index + 1)) }
        }
    }
}

@Composable
private fun SprayLineFieldInput(index: Int, state: SprayState, vm: SprayViewModel, kind: SprayLineField, value: String, label: String, decimal: Boolean = false) {
    val activity = LocalUserActivity.current
    val error = state.errors["line-$index-${kind.name.lowercase()}"]
    OutlinedTextField(value = value, onValueChange = { activity(); vm.lineText(index, kind, it) }, enabled = state.canEdit,
        modifier = Modifier.fillMaxWidth(), label = { Text(label) }, singleLine = true, isError = error != null,
        supportingText = { error?.let { Text(it) } }, keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Text))
}
