package com.rncoding.testvineshield.core.data.remote.dto

import kotlinx.serialization.Serializable

// Open-meteo returns json response with nested lists for hourly weather forecast values
@Serializable
data class WeatherDto(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val timezone: String,
    val hourlyWeather: HourlyWeatherDto
)
