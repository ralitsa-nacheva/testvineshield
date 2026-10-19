package com.rncoding.testvineshield.core.domain.disease_risk

interface DiseaseRiskEngine {

    suspend fun evaluate(
        modelType: DiseaseRiskModelType,
        diseaseId: Long,
        vineyardId: Long,
        blockId: Long,
        calculatedAt: Long
    ): DiseaseRiskEngineResult
}