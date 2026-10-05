package com.rncoding.testvineshield.core.data.remote

import com.rncoding.testvineshield.core.data.datasource.WeatherRemoteDataSource
import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto

class WeatherRemoteDataSourceImpl(
    private val weatherApi: OpenMeteoApi
) : WeatherRemoteDataSource {

    override suspend fun fetchWeather(
        latitude: Double,
        longitude: Double
    ): WeatherDto {
        return weatherApi.getHourlyWeather(
            latitude = latitude,
            longitude = longitude
        )
    }

    override suspend fun fetchWeatherForecast(
        latitude: Double,
        longitude: Double
    ): WeatherDto {
        return weatherApi.getForecastWeather(
            latitude = latitude,
            longitude = longitude
        )
    }
}