package com.rncoding.testvineshield.core.domain.disease_risk

interface DiseaseRiskVineyardContextProvider {

    suspend fun getTimeZone(
        vineyardId: Long
    ): String?
}