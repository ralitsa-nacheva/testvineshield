package com.rncoding.testvineshield.core.domain.disease_risk

data class DiseaseRiskWeatherPoint(
    val timestamp: Long,
    val temperatureCelsius: Double,
    val relativeHumidityPercent: Double?,
    val dewPointCelsius: Double?,
    val precipitationMm: Double?,
    val soilMoisture0To1Cm: Double?,
    val soilMoisture1To3Cm: Double?,
    val leafWetness: EnvironmentalMeasurement<Boolean>?,
    val isDay: Boolean?
)