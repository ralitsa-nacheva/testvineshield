package com.rncoding.testvineshield.core.domain.disease_risk.powdery

data class PowderyMildewAscosporeResult(
    val precipitationMm: Double,
    val releaseConditionsMet: Boolean,
    val longestWetPeriodHours: Int?,
    val wetPeriodMeanTemperatureCelsius: Double?,
    val infectionConditionsMet: Boolean?
)