package com.rncoding.testvineshield.core.domain.disease_risk

import kotlinx.datetime.LocalDate

data class DiseaseRiskOccurrenceEvidence(
    val occurrenceId: Long,
    val diseaseId: Long,
    val observedAt: LocalDate,
    val severityPercent: Int
)