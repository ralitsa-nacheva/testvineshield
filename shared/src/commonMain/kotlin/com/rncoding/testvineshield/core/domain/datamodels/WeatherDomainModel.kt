package com.rncoding.testvineshield.core.domain.datamodels


data class WeatherDomainModel(
    val vineyardId: Long,
    val timestamp: Long,
    val hourlyTemp2m: Double,
    val relativeHumidity2m: Double,
    val dewPoint2m: Double,
    val cloudCover: Double,
    val windSpeed10m: Double,
    val precipitation: Double,
    val rain: Double,
    val showers: Double,
    val snowfall: Double,
    val snowDepth: Double,
    val weatherCode: Int,
    val freezingLevelHeight: Double,
    val visibility: Double,
    val soilTemperature0cm: Double,
    val soilTemperature6cm: Double,
    val soilMoisture0to1cm: Double,
    val soilMoisture1to3cm: Double,
    val isDay: Boolean
)
