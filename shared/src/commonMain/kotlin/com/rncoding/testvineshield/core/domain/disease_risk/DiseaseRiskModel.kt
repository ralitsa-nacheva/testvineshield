package com.rncoding.testvineshield.core.domain.disease_risk

interface DiseaseRiskModel {

    val modelType: DiseaseRiskModelType

    val modelVersion: String

    fun evaluate(
        input: DiseaseRiskInput,
        previousState: DiseaseRiskModelState?,
        calculatedAt: Long
    ): DiseaseRiskModelEvaluation
}