package com.rncoding.testvineshield.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WeatherDto(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val timezone: String,

    @SerialName("hourly")
    val hourlyWeather: HourlyWeatherDto
)