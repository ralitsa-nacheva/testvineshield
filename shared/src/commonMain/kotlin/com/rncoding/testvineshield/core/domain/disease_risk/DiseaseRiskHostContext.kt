package com.rncoding.testvineshield.core.domain.disease_risk

import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage

import kotlinx.datetime.LocalDate

data class DiseaseRiskHostContext(
    val phenologicalStage: PhenologicalStage?,
    val activeOccurrence: DiseaseRiskOccurrenceEvidence?,
    val observedSymptoms: List<DiseaseRiskSymptomEvidence>,
    val stageObservedOn: LocalDate? = null
)