package com.rncoding.testvineshield.core.domain.export

import com.rncoding.testvineshield.core.domain.datamodels.HarvestDomainModel
import com.rncoding.testvineshield.core.domain.usecases.harvest.HarvestScope

enum class HarvestExportFormat(val extension: String, val mimeType: String) {
    JSON("json", "application/json"), CSV("csv", "text/csv")
}

data class HarvestExportSnapshot(
    val scope: HarvestScope,
    val vineyardName: String,
    val blockName: String,
    val season: String?, // null explicitly means all seasons
    val generatedAtMillis: Long,
    val records: List<HarvestDomainModel>
)

data class PreparedHarvestExport(
    val fileName: String,
    val mimeType: String,
    val content: String,
    val recordCount: Int
)

interface HarvestExportEncoder {
    suspend fun encode(snapshot: HarvestExportSnapshot, format: HarvestExportFormat): PreparedHarvestExport
}

class HarvestExportLimitException : IllegalArgumentException("Export exceeds the supported in-memory limit.")
