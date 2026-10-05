package com.rncoding.testvineshield.core.domain.datamodels

import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceActivityCrossRef
import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceSymptomCrossRef
import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import kotlinx.datetime.LocalDate

data class DiseaseOccurrenceDomainModel(
    val occurrenceId: Long,
    val diseaseId: Long,
    val vineyardId: Long,
    val blockId: Long?,
    val observedAt: LocalDate,
    val severity: Int,
    val status: DiseaseOccurrenceStatus,
    val curedAt: LocalDate?,
    val createdAt: LocalDate,
    val updatedAt: LocalDate,
    val notes: String?
)
