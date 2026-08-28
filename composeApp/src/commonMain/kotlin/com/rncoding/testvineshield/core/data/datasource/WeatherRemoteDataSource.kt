package com.rncoding.testvineshield.core.data.datasource

import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto

interface WeatherRemoteDataSource {
    suspend fun fetchWeather(latitude: Double, longitude: Double): WeatherDto
    suspend fun fetchForecastWeather(latitude: Double, longitude: Double): WeatherDto
}