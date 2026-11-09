package com.rncoding.testvineshield.core.presentation.blocks

import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import com.rncoding.testvineshield.core.domain.datamodels.units.*
import com.rncoding.testvineshield.core.presentation.formatting.*
import kotlinx.datetime.LocalDate

enum class BlockField { Name, Variety, Color, RootStock, Area, Rows, Spacing, Planted }

data class BlockDraft(
    val name: String = "",
    val variety: String = "",
    val color: String = "",
    val rootStock: String = "",
    val area: String = "",
    val rows: String = "",
    val spacing: String = "",
    val planted: String = "",
    val areaUnit: AreaUnit? = null,
    val spacingUnit: LengthUnit? = null,
    val stage: PhenologicalStage? = null
) {
    fun text(field: BlockField): String = when (field) {
        BlockField.Name -> name
        BlockField.Variety -> variety
        BlockField.Color -> color
        BlockField.RootStock -> rootStock
        BlockField.Area -> area
        BlockField.Rows -> rows
        BlockField.Spacing -> spacing
        BlockField.Planted -> planted
    }
    fun changed(field: BlockField, value: String): BlockDraft = when (field) {
        BlockField.Name -> copy(name = value)
        BlockField.Variety -> copy(variety = value)
        BlockField.Color -> copy(color = value)
        BlockField.RootStock -> copy(rootStock = value)
        BlockField.Area -> copy(area = value)
        BlockField.Rows -> copy(rows = value)
        BlockField.Spacing -> copy(spacing = value)
        BlockField.Planted -> copy(planted = value)
    }
    // Primitive state only; stable unit codes, never enum ordinals. Not a database write.
    fun savedValues(): ArrayList<String> = arrayListOf(name, variety, color, rootStock, area, rows, spacing, planted, areaUnit?.code.orEmpty(), spacingUnit?.code.orEmpty(), stage?.dbValue.orEmpty())
    companion object {
        fun from(block: BlockDomainModel) = BlockDraft(
            name = block.name,
            variety = block.vineVariety,
            color = block.color,
            rootStock = block.rootStock,
            area = block.area?.let(::formatPlainDecimal).orEmpty(),
            rows = block.rowCount?.toString().orEmpty(),
            spacing = block.rowSpacing?.let(::formatPlainDecimal).orEmpty(),
            planted = block.plantedAt?.toString().orEmpty(),
            areaUnit = block.areaUnit,
            spacingUnit = block.rowSpacingUnit,
            stage = block.phenologicalStage
        )
        fun restored(values: List<String>): BlockDraft? {
            if (values.size != 11) return null
            return try { BlockDraft(values[0], values[1], values[2], values[3], values[4], values[5], values[6], values[7], values[8].takeIf { it.isNotEmpty() }?.let(AreaUnit::fromCode), values[9].takeIf { it.isNotEmpty() }?.let(LengthUnit::fromCode), values[10].takeIf { it.isNotEmpty() }?.let(PhenologicalStage::fromDbValue)) }
            catch (_: IllegalArgumentException) { null }
        }
    }
}
internal data class BlockDraftResult(val block: BlockDomainModel?, val errors: Map<BlockField, String>)
internal fun BlockDraft.parsed(vineyardId: Long, blockId: Long): BlockDraftResult {
    val errors = mutableMapOf<BlockField, String>()
    if (name.isBlank()) errors[BlockField.Name] = "Block name is required."
    if (variety.isBlank()) errors[BlockField.Variety] = "Vine variety is required."
    fun measure(field: BlockField): Double? = when (val parsed = OptionalDecimalInput.parse(text(field))) {
        DecimalInputResult.Invalid -> { errors[field] = "Use a finite decimal with one dot or comma, or leave blank."
            null }
        is DecimalInputResult.Valid -> parsed.value.also { if (it != null && it <= 0.0) errors[field] = "Known value must be greater than zero." }
    }
    val areaValue = measure(BlockField.Area)
    val spacingValue = measure(BlockField.Spacing)
    val count = if (rows.isBlank()) null else rows.trim().takeIf { it.isNotEmpty() && it.all { character -> character in '0'..'9' } }?.toIntOrNull().also {
        if (it == null || it <= 0) errors[BlockField.Rows] = "Use a positive whole number, or leave blank."
    }
    val date = if (planted.isBlank()) null else try { LocalDate.parse(planted.trim()) }
    catch (_: IllegalArgumentException) { errors[BlockField.Planted] = "Use a valid ISO date, e.g. 2020-04-15, or leave blank."
        null }
    return BlockDraftResult(if (errors.isEmpty()) BlockDomainModel(blockId, vineyardId, name.trim(), areaValue, variety.trim(), color.trim(), rootStock.trim(), count, spacingValue, date, stage, areaUnit, spacingUnit) else null, errors)
}
