package com.rncoding.testvineshield.core.presentation.spray

import com.rncoding.testvineshield.core.domain.datamodels.*
import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
import com.rncoding.testvineshield.core.presentation.formatting.*
import kotlinx.datetime.LocalDate

enum class SprayField { Date, Time, Area, Water, Notes }
enum class SprayLineField { Name, Quantity, Reference, RateQuantity, RateBasisQuantity }

data class SprayLineDraft(val id: Long = 0, val name: String = "", val quantity: String = "", val unit: SprayQuantityUnit? = null, val reference: String = "", val rateQuantity: String = "", val rateUnit: SprayQuantityUnit? = null, val rateBasisQuantity: String = "", val rateBasis: SprayRateBasis? = null)
data class SprayDraft(
    val date: String = "", val time: String = "", val zone: String = "",
    val area: String = "", val areaUnit: AreaUnit? = null,
    val water: String = "", val waterUnit: SprayQuantityUnit? = null, val notes: String = "",
    val lines: List<SprayLineDraft> = listOf(SprayLineDraft()),
    val baseRevision: Long = 0, val baseGeneration: String? = null
) {
    fun text(field: SprayField): String = when (field) {
        SprayField.Date -> date; SprayField.Time -> time; SprayField.Area -> area; SprayField.Water -> water; SprayField.Notes -> notes
    }
    fun changed(field: SprayField, value: String): SprayDraft = when (field) {
        SprayField.Date -> copy(date = value); SprayField.Time -> copy(time = value); SprayField.Area -> copy(area = value)
        SprayField.Water -> copy(water = value); SprayField.Notes -> copy(notes = value)
    }
    fun savedValues(): ArrayList<String> = arrayListOf("2", date, time, zone, area, areaUnit?.code.orEmpty(), water, waterUnit?.code.orEmpty(), notes, lines.size.toString(), baseRevision.toString(), baseGeneration.orEmpty()).also { values ->
        lines.forEach { values.addAll(listOf(it.id.toString(), it.name, it.quantity, it.unit?.code.orEmpty(), it.reference, it.rateQuantity, it.rateUnit?.code.orEmpty(), it.rateBasisQuantity, it.rateBasis?.code.orEmpty())) }
    }
    companion object {
        fun from(record: SprayRecord) = SprayDraft(record.applicationDate.toString(), record.localTime.orEmpty(), record.timeZone,
            record.treatedArea?.let(::sprayNumberText).orEmpty(), record.areaUnit, record.waterQuantity?.let(::sprayNumberText).orEmpty(),
            record.waterUnit, record.notes.orEmpty(), record.products.map { SprayLineDraft(it.lineId, it.productName, it.quantity?.let(::sprayNumberText).orEmpty(), it.unit, it.labelReference.orEmpty(), it.rate?.quantity?.let(::sprayNumberText).orEmpty(), it.rate?.productUnit, it.rate?.basisQuantity?.let(::sprayNumberText).orEmpty(), it.rate?.basis) }, record.revision, record.generation)
        fun restore(values: List<String>): SprayDraft? {
            if (values.size < 12 || values[0] !in listOf("1", "2")) return null
            val count = values[9].toIntOrNull() ?: return null
            val stride = if (values[0] == "1") 5 else 9
            if (count !in 1..20 || values.size != 12 + count * stride) return null
            return try {
                val lines = (0 until count).map { index ->
                    val at = 12 + index * stride
                    val id = values[at].toLongOrNull() ?: return null
                    if (id < 0) return null
                    SprayLineDraft(id, values[at + 1], values[at + 2], values[at + 3].takeIf { it.isNotEmpty() }?.let(SprayQuantityUnit::fromCode), values[at + 4],
                        if (stride == 9) values[at + 5] else "",
                        if (stride == 9) values[at + 6].takeIf { it.isNotEmpty() }?.let(SprayQuantityUnit::fromCode) else null,
                        if (stride == 9) values[at + 7] else "",
                        if (stride == 9) values[at + 8].takeIf { it.isNotEmpty() }?.let(SprayRateBasis::fromCode) else null)
                }
                SprayDraft(values[1], values[2], values[3], values[4], values[5].takeIf { it.isNotEmpty() }?.let(AreaUnit::fromCode),
                    values[6], values[7].takeIf { it.isNotEmpty() }?.let(SprayQuantityUnit::fromCode), values[8], lines, values[10].toLongOrNull()?.takeIf { it >= 0 } ?: return null, values[11].takeIf { it.isNotEmpty() })
            } catch (_: IllegalArgumentException) { null }
        }
    }
}

internal data class SprayDraftResult(val record: SprayRecord?, val errors: Map<String, String>)
internal fun SprayDraft.parsed(activityId: Long): SprayDraftResult {
    val errors = mutableMapOf<String, String>()
    val actualDate = try { LocalDate.parse(date.trim()) } catch (_: IllegalArgumentException) { errors["date"] = "Enter the actual application date as YYYY-MM-DD."; null }
    val localTime = time.trim().takeIf { it.isNotEmpty() }
    if (localTime != null && !Regex("(?:[01][0-9]|2[0-3]):[0-5][0-9]").matches(localTime)) errors["time"] = "Use HH:mm or leave blank when unknown."
    fun decimal(text: String, field: String): Double? = when (val value = OptionalDecimalInput.parse(text)) {
        DecimalInputResult.Invalid -> { errors[field] = "Use a finite positive decimal, without thousands grouping."; null }
        is DecimalInputResult.Valid -> value.value.also { if (it != null && it <= 0.0) errors[field] = "A known amount must be greater than zero." }
    }
    val treated = decimal(area, "area"); val carrier = decimal(water, "water")
    if (treated != null && areaUnit == null) errors["areaUnit"] = "Choose the treated-area unit."
    if (carrier != null && waterUnit == null) errors["waterUnit"] = "Choose the carrier-water unit."
    val products = lines.mapIndexed { index, line ->
        val amount = decimal(line.quantity, "line-$index-quantity")
        if (line.name.isBlank()) errors["line-$index-name"] = "Product name is required."
        if (amount != null && line.unit == null) errors["line-$index-unit"] = "Choose this product's quantity unit."
        val hasRate = line.rateQuantity.isNotBlank() || line.rateUnit != null || line.rateBasisQuantity.isNotBlank() || line.rateBasis != null
        val rateAmount = if (hasRate) decimal(line.rateQuantity, "line-$index-ratequantity") else null
        val basisAmount = if (hasRate) decimal(line.rateBasisQuantity, "line-$index-ratebasisquantity") else null
        if (hasRate && rateAmount == null) errors["line-$index-ratequantity"] = "Enter the recorded product amount, or clear the whole rate."
        if (hasRate && basisAmount == null) errors["line-$index-ratebasisquantity"] = "Enter an explicit basis amount; it is not assumed to be one."
        if (hasRate && line.rateUnit == null) errors["line-$index-rateunit"] = "Choose the rate product unit."
        if (hasRate && line.rateBasis == null) errors["line-$index-ratebasis"] = "Choose treated area or carrier-water basis."
        val rate = if (hasRate && rateAmount != null && basisAmount != null && line.rateUnit != null && line.rateBasis != null)
            SprayRate(rateAmount, line.rateUnit, basisAmount, line.rateBasis) else null
        SprayProductLine(line.id, line.name.trim(), amount, line.unit, line.reference.trim().takeIf { it.isNotEmpty() }, rate)
    }
    if (errors.isNotEmpty() || actualDate == null) return SprayDraftResult(null, errors)
    return SprayDraftResult(SprayRecord(activityId, actualDate, localTime, zone, treated, areaUnit, carrier, waterUnit,
        notes.trim().takeIf { it.isNotEmpty() }, baseRevision, products, baseGeneration), emptyMap())
}

private fun sprayNumberText(value: Double): String = if (value.isFinite()) formatPlainDecimal(value) else value.toString()
