package com.rncoding.testvineshield.core.presentation.spray

import androidx.compose.foundation.layout.*
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
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Product ${index + 1}", style = MaterialTheme.typography.titleMedium)
            SprayLineFieldInput(index, state, vm, SprayLineField.Name, line.name, "Product name * (up to 200 characters)")
            SprayLineFieldInput(index, state, vm, SprayLineField.Quantity, line.quantity, "Actual product quantity (optional)", true)
            RecordChoice("Actual quantity unit", line.unit, SprayQuantityUnit.entries, state.canEdit, { it.label }, { vm.lineUnit(index, it) })
            state.errors["line-$index-unit"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            SprayLineFieldInput(index, state, vm, SprayLineField.Reference, line.reference, "User label/registration reference (optional)")
            Text("Recorded rate / concentration (optional)", style = MaterialTheme.typography.titleSmall)
            Text("Enter all four parts, or leave them all blank. No denominator of one is assumed and no dose is recommended.")
            SprayLineFieldInput(index, state, vm, SprayLineField.RateQuantity, line.rateQuantity, "Rate: product amount", true)
            RecordChoice("Rate: product unit", line.rateUnit, SprayQuantityUnit.entries, state.canEdit, { it.label }, { vm.lineRateUnit(index, it) })
            state.errors["line-$index-rateunit"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            SprayLineFieldInput(index, state, vm, SprayLineField.RateBasisQuantity, line.rateBasisQuantity, "Per: basis amount", true)
            RecordChoice("Per: basis unit", line.rateBasis, SprayRateBasis.entries, state.canEdit, { it.label }, { vm.lineRateBasis(index, it) })
            state.errors["line-$index-ratebasis"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = userAction { vm.clearRate(index) }, enabled = state.canEdit) { Text("Clear recorded rate") }
            OutlinedButton(onClick = userAction { vm.removeLine(index) }, enabled = state.canEdit && state.draft.lines.size > 1) { Text("Remove product ${index + 1}") }
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
