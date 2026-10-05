package com.rncoding.testvineshield.core.domain.disease_risk

import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import kotlinx.datetime.LocalDate

data class DiseaseRiskSymptomEvidence(
    val symptomId: Long,
    val code: String?,
    val name: String,
    val plantPart: String,
    val phenologicalStage: PhenologicalStage,
    val observedAt: LocalDate?,
    val severity: String?,
    val notes: String?
)