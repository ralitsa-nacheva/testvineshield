package com.rncoding.testvineshield.core.domain.disease_risk

interface DiseaseRiskWeatherProvider {

    suspend fun getHistoricalWeather(
        vineyardId: Long,
        startTimestamp: Long,
        endTimestamp: Long
    ): List<DiseaseRiskWeatherPoint>
}