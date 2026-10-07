package com.rncoding.testvineshield.core.domain.disease_risk.botrytis

data class BotrytisRiskResult(
    val hostTissue: BotrytisHostTissue,
    val longestWetPeriodHours: Int?,
    val wetPeriodMeanTemperatureCelsius: Double?,
    val predictedInfectionPercent: Double?,
    val riskLevel: RiskLevel,
    val evaluationAvailable: Boolean
) {

    enum class RiskLevel {
        NONE,
        LOW,
        MODERATE,
        HIGH
    }
}