package com.rncoding.testvineshield.core.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto

class OpenMeteoApi(
    private val client: HttpClient
) {

    suspend fun getHourlyWeather(
        latitude: Double,
        longitude: Double
    ): WeatherDto {

        return client.get("https://api.open-meteo.com/v1/forecast") {
            parameter("latitude", latitude)
            parameter("longitude", longitude)
            parameter("hourly", "temperature_2m,relativehumidity_2m,precipitation")
            parameter("timezone", "auto")
        }.body()
    }
}