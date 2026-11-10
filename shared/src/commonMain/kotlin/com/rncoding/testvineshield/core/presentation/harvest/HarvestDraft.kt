package com.rncoding.testvineshield.core.presentation.harvest

import com.rncoding.testvineshield.core.domain.datamodels.HarvestDomainModel
import com.rncoding.testvineshield.core.domain.usecases.harvest.HarvestScope
import com.rncoding.testvineshield.core.presentation.formatting.*
import kotlinx.datetime.LocalDate

enum class HarvestField { Season, Date, Quantity, Brix }

data class HarvestDraft(
    val season: String = "",
    val date: String = "",
    val quantity: String = "",
    val units: String? = null,
    val brix: String = ""
) {
    fun text(field: HarvestField): String = when (field) {
        HarvestField.Season -> season
        HarvestField.Date -> date
        HarvestField.Quantity -> quantity
        HarvestField.Brix -> brix
    }
    fun changed(field: HarvestField, value: String): HarvestDraft = when (field) {
        HarvestField.Season -> copy(season = value)
        HarvestField.Date -> copy(date = value)
        HarvestField.Quantity -> copy(quantity = value)
        HarvestField.Brix -> copy(brix = value)
    }
    fun savedValues(): ArrayList<String> = arrayListOf(season, date, quantity, units.orEmpty(), brix)
    companion object {
        val commonUnits = listOf("kg", "t", "lb", "g")
        fun from(record: HarvestDomainModel) = HarvestDraft(
            record.season, record.harvestDate.toString(), harvestNumberText(record.quantity),
            record.units.takeIf { it.isNotBlank() }, record.brixSugarContent?.let(::harvestNumberText).orEmpty()
        )
        fun restore(values: List<String>): HarvestDraft? = if (values.size == 5)
            HarvestDraft(values[0], values[1], values[2], values[3].takeIf { it.isNotEmpty() }, values[4]) else null
    }
}

// Historical invalid values must remain visible/correctable rather than crash the form.
internal fun harvestNumberText(value: Double): String =
    if (value.isFinite()) formatPlainDecimal(value) else value.toString()

internal data class HarvestDraftResult(
    val record: HarvestDomainModel?,
    val errors: Map<HarvestField, String>,
    val unitError: String? = null
)

internal fun HarvestDraft.parsed(scope: HarvestScope, original: HarvestDomainModel?): HarvestDraftResult {
    val errors = mutableMapOf<HarvestField, String>()
    if (season.isBlank()) errors[HarvestField.Season] = "Enter a season, e.g. 2026 or 2026/2027."
    val actualDate = try { LocalDate.parse(date.trim()) } catch (_: IllegalArgumentException) {
        errors[HarvestField.Date] = "Enter a valid ISO date, e.g. 2026-11-09."
        null
    }
    fun decimal(field: HarvestField, required: Boolean): Double? {
        val parsed = OptionalDecimalInput.parse(text(field))
        if (parsed is DecimalInputResult.Invalid) {
            errors[field] = "Use one dot or comma decimal separator; no thousands grouping."
            return null
        }
        val value = (parsed as DecimalInputResult.Valid).value
        if (value == null && required) errors[field] = "An explicit quantity is required."
        return value
    }
    val amount = decimal(HarvestField.Quantity, true)
    if (amount != null && amount <= 0.0) errors[HarvestField.Quantity] = "Quantity must be greater than zero."
    val measuredBrix = decimal(HarvestField.Brix, false)
    if (measuredBrix != null && measuredBrix !in 0.0..100.0) errors[HarvestField.Brix] = "Brix must be between 0 and 100."
    val selectedUnit = units?.trim()
    val unitError = if (selectedUnit.isNullOrBlank()) "Choose the unit used for this quantity." else null
    if (errors.isNotEmpty() || unitError != null || actualDate == null || amount == null || selectedUnit.isNullOrBlank()) {
        return HarvestDraftResult(null, errors, unitError)
    }
    val template = original ?: HarvestDomainModel(0, scope.blockId, "", actualDate, "", amount, null)
    return HarvestDraftResult(template.copy(season = season.trim(), harvestDate = actualDate,
        quantity = amount, units = selectedUnit, brixSugarContent = measuredBrix), emptyMap())
}
