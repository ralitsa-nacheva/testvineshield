package com.rncoding.testvineshield.core.domain.disease_risk

interface DiseaseRiskInputFactory {

    suspend fun createInput(
        modelType: DiseaseRiskModelType,
        diseaseId: Long,
        vineyardId: Long,
        blockId: Long,
        calculatedAt: Long,
        previousState: DiseaseRiskModelState?
    ): DiseaseRiskInputResult
}