package com.rncoding.testvineshield.core.domain.datamodels

import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import kotlinx.datetime.LocalDate

data class DiseaseOccurrenceSummary(
    val occurrenceId: Long,
    val diseaseId: Long,
    val diseaseName: String,
    val blockId: Long?,
    val blockName: String?,
    val observedAt: LocalDate,
    val severity: Int,
    val status: DiseaseOccurrenceStatus
)