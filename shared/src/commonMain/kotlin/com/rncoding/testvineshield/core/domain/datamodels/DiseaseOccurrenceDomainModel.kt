package com.rncoding.testvineshield.core.domain.datamodels

import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceActivityCrossRef
import com.rncoding.testvineshield.core.data.local.database.entities.OccurrenceSymptomCrossRef
import kotlinx.datetime.LocalDate

data class DiseaseOccurrenceDomainModel(
    val occurrenceId: Long,
    val diseaseId: Long,
    val vineyardId: Long,
    val blockId: Long,
    val observedAt: LocalDate,
    val severity: Int,
    val status: String,
    val curedAt: LocalDate,
    val createdAt: LocalDate,
    val updatedAt: LocalDate,
    val notes: String,
    val observedSymptoms: List<OccurrenceSymptomCrossRef>,
    val treatments: List<OccurrenceActivityCrossRef>
)
