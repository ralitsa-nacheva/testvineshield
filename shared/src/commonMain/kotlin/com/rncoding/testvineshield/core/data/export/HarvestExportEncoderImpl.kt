package com.rncoding.testvineshield.core.data.export

import com.rncoding.testvineshield.core.domain.export.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/** Versioned portable records. No unit conversion, statistical inference or import promise. */
class HarvestExportEncoderImpl : HarvestExportEncoder {
    companion object {
        const val FORMAT_VERSION = 1
        const val MAX_RECORDS = 10_000
        const val MAX_UTF8_BYTES = 5 * 1024 * 1024
        const val MAX_INPUT_CHARACTERS = 512 * 1024
    }
    private val jsonFormat = Json { prettyPrint = true }

    override suspend fun encode(snapshot: HarvestExportSnapshot, format: HarvestExportFormat): PreparedHarvestExport = withContext(Dispatchers.Default) {
        if (snapshot.records.size > MAX_RECORDS) throw HarvestExportLimitException()
        var inputCharacters = snapshot.vineyardName.length.toLong() + snapshot.blockName.length + (snapshot.season?.length ?: 0)
        for (record in snapshot.records) {
            currentCoroutineContext().ensureActive()
            inputCharacters += record.season.length.toLong() + record.units.length + 32
            if (inputCharacters > MAX_INPUT_CHARACTERS) throw HarvestExportLimitException()
        }
        if (inputCharacters > MAX_INPUT_CHARACTERS) throw HarvestExportLimitException()
        val sorted = snapshot.copy(records = snapshot.records.sortedWith(compareByDescending<com.rncoding.testvineshield.core.domain.datamodels.HarvestDomainModel> { it.harvestDate }.thenByDescending { it.harvestId }))
        val content = when (format) {
            HarvestExportFormat.JSON -> json(sorted)
            HarvestExportFormat.CSV -> csv(sorted)
        }
        currentCoroutineContext().ensureActive()
        if (content.encodeToByteArray().size > MAX_UTF8_BYTES) throw HarvestExportLimitException()
        PreparedHarvestExport("vineshield-harvest-block-${snapshot.scope.blockId}-${snapshot.generatedAtMillis}.${format.extension}", format.mimeType, content, sorted.records.size)
    }

    private suspend fun json(snapshot: HarvestExportSnapshot): String {
        val rows = snapshot.records.map { record ->
            currentCoroutineContext().ensureActive()
            buildJsonObject {
                put("harvest_id", record.harvestId)
                put("season", record.season)
                put("harvest_date", record.harvestDate.toString())
                // Decimal strings keep canonical Double text, including invalid legacy values.
                put("quantity", record.quantity.toString())
                put("quantity_status", if (record.quantity.isFinite() && record.quantity > 0) "recorded" else "invalid")
                put("quantity_unit", record.units)
                put("unit_status", if (record.units.isBlank()) "unknown" else "recorded")
                put("brix", record.brixSugarContent?.let { JsonPrimitive(it.toString()) } ?: JsonNull)
                put("brix_status", brixStatus(record.brixSugarContent))
            }
        }
        val root = buildJsonObject {
            put("format", "vineshield.harvest")
            put("format_version", FORMAT_VERSION)
            put("generated_at_epoch_ms", snapshot.generatedAtMillis)
            put("vineyard_id", snapshot.scope.vineyardId)
            put("vineyard_name", snapshot.vineyardName)
            put("block_id", snapshot.scope.blockId)
            put("block_name", snapshot.blockName)
            put("season_filter", snapshot.season?.let(::JsonPrimitive) ?: JsonNull)
            put("record_count", rows.size)
            put("records", JsonArray(rows))
        }
        return jsonFormat.encodeToString(JsonElement.serializer(), root)
    }

    private suspend fun csv(snapshot: HarvestExportSnapshot): String = buildString {
        append('\uFEFF') // UTF-8 BOM for spreadsheet Unicode detection; JSON has no BOM.
        fun row(values: List<String>) {
            append(values.joinToString(",", transform = ::csvQuoted)).append("\r\n")
            // Bound growth while building, not only after allocating the final UTF-8 byte array.
            if (length > MAX_UTF8_BYTES) throw HarvestExportLimitException()
        }
        row(listOf("format_version", "generated_at_epoch_ms", "vineyard_id", "vineyard_name", "block_id", "block_name", "season_filter", "harvest_id", "season", "harvest_date", "quantity", "quantity_status", "quantity_unit", "unit_status", "brix", "brix_status"))
        for (record in snapshot.records) {
            currentCoroutineContext().ensureActive()
            row(listOf(FORMAT_VERSION.toString(), snapshot.generatedAtMillis.toString(), snapshot.scope.vineyardId.toString(),
                spreadsheetText(snapshot.vineyardName), snapshot.scope.blockId.toString(), spreadsheetText(snapshot.blockName),
                snapshot.season?.let(::spreadsheetText).orEmpty(), record.harvestId.toString(), spreadsheetText(record.season),
                record.harvestDate.toString(), record.quantity.toString(), if (record.quantity.isFinite() && record.quantity > 0) "recorded" else "invalid",
                spreadsheetText(record.units), if (record.units.isBlank()) "unknown" else "recorded",
                record.brixSugarContent?.toString().orEmpty(), brixStatus(record.brixSugarContent)))
        }
    }

    private fun brixStatus(value: Double?): String = when {
        value == null -> "unknown"
        !value.isFinite() || value !in 0.0..100.0 -> "invalid"
        else -> "recorded"
    }

    /** Spreadsheet-view CSV is not a lossless text interchange. Use JSON for exact strings. */
    private fun spreadsheetText(value: String): String = spreadsheetTextValue(value)
}
