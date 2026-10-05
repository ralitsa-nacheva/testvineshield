package com.rncoding.testvineshield.core.data.remote

import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class OpenMeteoApi(
    private val client: HttpClient
) {

    suspend fun getHourlyWeather(
        latitude: Double,
        longitude: Double
    ): WeatherDto {
        return client.get(
            "https://api.open-meteo.com/v1/forecast"
        ) {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("hourly", HOURLY_VARIABLES)
            parameter("timezone", "auto")
            parameter("past_days", 7)
            parameter("forecast_days", 1)
        }.body()
    }

    suspend fun getForecastWeather(
        latitude: Double,
        longitude: Double
    ): WeatherDto {
        return client.get(
            "https://api.open-meteo.com/v1/forecast"
        ) {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("hourly", HOURLY_VARIABLES)
            parameter("timezone", "auto")
            parameter("forecast_days", 7)
        }.body()
    }

    private companion object {

        const val HOURLY_VARIABLES =
            "temperature_2m," +
                    "relative_humidity_2m," +
                    "dew_point_2m," +
                    "cloud_cover," +
                    "wind_speed_10m," +
                    "precipitation," +
                    "rain," +
                    "showers," +
                    "snowfall," +
                    "snow_depth," +
                    "weather_code," +
                    "freezing_level_height," +
                    "visibility," +
                    "soil_temperature_0cm," +
                    "soil_temperature_6cm," +
                    "soil_moisture_0_to_1cm," +
                    "soil_moisture_1_to_3cm," +
                    "is_day"
    }
}