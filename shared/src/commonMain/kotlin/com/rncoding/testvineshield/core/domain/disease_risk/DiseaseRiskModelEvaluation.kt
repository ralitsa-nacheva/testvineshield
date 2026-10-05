package com.rncoding.testvineshield.core.domain.disease_risk

data class DiseaseRiskModelEvaluation(
    val result: DiseaseRiskResult,
    val nextState: DiseaseRiskModelState?
)