package com.rncoding.testvineshield.core.domain.disease_risk

data class DiseaseRiskWeatherWindow(
    val startTimestamp: Long,
    val endTimestamp: Long
) {
    init {
        require(startTimestamp <= endTimestamp) {
            "Weather window start must not exceed end"
        }
    }
}