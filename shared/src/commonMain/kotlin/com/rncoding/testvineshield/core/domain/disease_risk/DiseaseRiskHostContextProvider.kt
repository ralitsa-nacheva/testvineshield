package com.rncoding.testvineshield.core.domain.disease_risk

interface DiseaseRiskHostContextProvider {

    suspend fun getHostContext(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long
    ): DiseaseRiskHostContext
}