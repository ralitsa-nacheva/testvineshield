package com.rncoding.testvineshield.core.domain.datamodels

data class VineyardWeatherSummary(
    val timestamp: Long,
    val temperature: Double,
    val relativeHumidity: Double,
    val precipitation: Double,
    val windSpeed: Double,
    val weatherCode: Int
)