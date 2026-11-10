package com.rncoding.testvineshield.core.domain.export

import com.rncoding.testvineshield.core.domain.datamodels.SprayRecord

data class SprayExportSnapshot(
    val vineyardId: Long, val vineyardName: String, val blockId: Long?, val blockName: String?,
    val activityType: String, val season: String, val generatedAtMillis: Long,
    val legacyTreatmentPresent: Boolean, val record: SprayRecord
)

interface SprayExportEncoder {
    suspend fun encode(snapshot: SprayExportSnapshot, format: DocumentExportFormat): PreparedDocument
}
class SprayExportLimitException : IllegalArgumentException("Spray export exceeds its in-memory size limit.")
