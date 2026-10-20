package com.rncoding.testvineshield.core.data.datasource

import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto

interface WeatherRemoteDataSource {

    suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
        timeZone: String
    ): WeatherDto

    suspend fun fetchWeatherForecast(
        latitude: Double,
        longitude: Double,
        timeZone: String
    ): WeatherDto
}