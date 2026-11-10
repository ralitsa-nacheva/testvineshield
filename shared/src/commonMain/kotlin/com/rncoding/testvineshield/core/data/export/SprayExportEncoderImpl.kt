package com.rncoding.testvineshield.core.data.export

import com.rncoding.testvineshield.core.domain.datamodels.SprayRate
import com.rncoding.testvineshield.core.domain.export.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

class SprayExportEncoderImpl : SprayExportEncoder {
    companion object { const val MAX_INPUT_CHARACTERS = 64 * 1024; const val MAX_UTF8_BYTES = 1024 * 1024 }
    private val jsonFormat = Json { prettyPrint = true }
    override suspend fun encode(snapshot: SprayExportSnapshot, format: DocumentExportFormat): PreparedDocument = withContext(Dispatchers.Default) {
        val record = snapshot.record
        if (record.products.size !in 1..20) throw SprayExportLimitException()
        val inputSize = snapshot.vineyardName.length.toLong() + (snapshot.blockName?.length ?: 0) + snapshot.activityType.length + snapshot.season.length +
            (record.notes?.length ?: 0) + record.timeZone.length + record.products.sumOf { it.productName.length.toLong() + (it.labelReference?.length ?: 0) }
        if (inputSize > MAX_INPUT_CHARACTERS) throw SprayExportLimitException()
        val content = when (format) { DocumentExportFormat.JSON -> json(snapshot); DocumentExportFormat.CSV -> csv(snapshot) }
        currentCoroutineContext().ensureActive()
        if (content.encodeToByteArray().size > MAX_UTF8_BYTES) throw SprayExportLimitException()
        PreparedDocument("vineshield-spray-activity-${record.activityId}-${snapshot.generatedAtMillis}.${format.extension}", format.mimeType, content, record.products.size)
    }
    private fun number(value: Double?): JsonElement = value?.let { JsonPrimitive(it.toString()) } ?: JsonNull
    private fun text(value: String?): JsonElement = value?.let(::JsonPrimitive) ?: JsonNull
    private fun status(value: Double?): String = when { value == null -> "unknown"; !value.isFinite() || value <= 0 -> "invalid"; else -> "recorded" }
    private fun rateStatus(rate: SprayRate?): String = when { rate == null -> "unknown"; status(rate.quantity) == "invalid" || status(rate.basisQuantity) == "invalid" -> "invalid"; else -> "recorded" }
    private suspend fun json(s: SprayExportSnapshot): String {
        val app = s.record
        val products = app.products.map { line ->
            currentCoroutineContext().ensureActive()
            buildJsonObject {
                put("product_line_id", line.lineId); put("product_name", line.productName)
                put("quantity", number(line.quantity)); put("quantity_status", status(line.quantity)); put("quantity_unit", text(line.unit?.code))
                put("label_reference", text(line.labelReference)); put("rate_status", rateStatus(line.rate))
                put("rate", line.rate?.let { rate -> buildJsonObject {
                    put("product_amount", number(rate.quantity)); put("product_unit", rate.productUnit.code)
                    put("basis_amount", number(rate.basisQuantity)); put("basis_unit", rate.basis.code); put("basis_dimension", rate.basis.dimension)
                } } ?: JsonNull)
            }
        }
        return jsonFormat.encodeToString(JsonElement.serializer(), buildJsonObject {
            put("format", "vineshield.spray"); put("format_version", 1); put("generated_at_epoch_ms", s.generatedAtMillis)
            put("vineyard_id", s.vineyardId); put("vineyard_name", s.vineyardName); put("block_id", s.blockId?.let { JsonPrimitive(it) } ?: JsonNull); put("block_name", text(s.blockName))
            put("scope_type", if (s.blockId == null) "whole_vineyard" else "block"); put("activity_id", app.activityId); put("activity_type", s.activityType); put("season", s.season)
            put("legacy_activity_treatment_present", s.legacyTreatmentPresent); put("legacy_activity_fields_exported", false)
            put("application_date", app.applicationDate.toString()); put("local_time", text(app.localTime)); put("time_zone", app.timeZone)
            put("treated_area", number(app.treatedArea)); put("area_unit", text(app.areaUnit?.code)); put("area_status", status(app.treatedArea))
            put("carrier_water", number(app.waterQuantity)); put("water_unit", text(app.waterUnit?.code)); put("water_status", status(app.waterQuantity))
            put("notes", text(app.notes)); put("saved_revision", app.revision); put("product_count", products.size); put("products", JsonArray(products))
        })
    }
    private suspend fun csv(s: SprayExportSnapshot): String = buildString {
        append('\uFEFF')
        fun row(values: List<String>) {
            append(values.joinToString(",", transform = ::csvQuoted)).append("\r\n")
            if (length > MAX_UTF8_BYTES) throw SprayExportLimitException()
        }
        row(listOf("format_version", "generated_at_epoch_ms", "vineyard_id", "vineyard_name", "block_id", "block_name", "scope_type", "activity_id", "activity_type", "season",
            "legacy_activity_treatment_present", "legacy_activity_fields_exported", "application_date", "local_time", "time_zone", "app_treated_area", "app_area_unit", "app_area_status",
            "app_carrier_water", "app_water_unit", "app_water_status", "app_notes", "saved_revision", "product_line_id", "product_name", "product_quantity", "quantity_status", "product_unit", "label_reference",
            "rate_product_amount", "rate_product_unit", "rate_basis_amount", "rate_basis_unit", "rate_basis_dimension", "rate_status"))
        val app = s.record
        for (line in app.products) {
            currentCoroutineContext().ensureActive()
            row(listOf("1", s.generatedAtMillis.toString(), s.vineyardId.toString(), spreadsheetTextValue(s.vineyardName), s.blockId?.toString().orEmpty(), spreadsheetTextValue(s.blockName.orEmpty()),
                if (s.blockId == null) "whole_vineyard" else "block", app.activityId.toString(), spreadsheetTextValue(s.activityType), spreadsheetTextValue(s.season), s.legacyTreatmentPresent.toString(), "false",
                app.applicationDate.toString(), app.localTime.orEmpty(), spreadsheetTextValue(app.timeZone), app.treatedArea?.toString().orEmpty(), app.areaUnit?.code.orEmpty(), status(app.treatedArea),
                app.waterQuantity?.toString().orEmpty(), app.waterUnit?.code.orEmpty(), status(app.waterQuantity), spreadsheetTextValue(app.notes.orEmpty()), app.revision.toString(), line.lineId.toString(),
                spreadsheetTextValue(line.productName), line.quantity?.toString().orEmpty(), status(line.quantity), line.unit?.code.orEmpty(), spreadsheetTextValue(line.labelReference.orEmpty()),
                line.rate?.quantity?.toString().orEmpty(), line.rate?.productUnit?.code.orEmpty(), line.rate?.basisQuantity?.toString().orEmpty(), line.rate?.basis?.code.orEmpty(), line.rate?.basis?.dimension.orEmpty(), rateStatus(line.rate)))
        }
    }
}
