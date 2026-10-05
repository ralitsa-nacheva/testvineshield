package com.rncoding.testvineshield.core.domain.disease_risk

import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage

data class DiseaseRiskHostContext(
    val phenologicalStage: PhenologicalStage?,
    val activeOccurrence: DiseaseRiskOccurrenceEvidence?,
    val observedSymptoms: List<DiseaseRiskSymptomEvidence>
)