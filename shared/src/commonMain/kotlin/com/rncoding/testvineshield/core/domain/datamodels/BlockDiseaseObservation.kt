package com.rncoding.testvineshield.core.domain.datamodels

import com.rncoding.testvineshield.core.domain.datamodels.enums.DiseaseOccurrenceStatus
import kotlinx.datetime.LocalDate

data class BlockDiseaseObservation(
    val id: Long,
    val diseaseName: String,
    val observedOn: LocalDate,
    val severity: Int,
    val status: DiseaseOccurrenceStatus,
    val notes: String?
)
