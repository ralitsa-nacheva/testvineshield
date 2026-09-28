package com.rncoding.testvineshield.core.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
// Open-meteo returns json response with nested lists for hourly weather forecast values


@Serializable
data class HourlyWeatherDto(
    @SerialName("time")
    val timestamp: List<String>,
    @SerialName("temperature_2m")
    val hourlyTemp2m: List<Double>,
    @SerialName("relative_humidity_2m")
    val relativeHumidity2m: List<Double>,
    @SerialName("dew_point_2m")
    val dewPoint2m: List<Double>,
    @SerialName("cloud_cover")
    val cloudCover: List<Double>,
    @SerialName("wind_speed_10m")
    val windSpeed10m: List<Double>,
    val precipitation: List<Double>,
    val rain: List<Double>,
    val showers: List<Double>,
    val snowfall: List<Double>,
    @SerialName("snow_depth")
    val snowDepth: List<Double>,
    @SerialName("weather_code")
    val weatherCode: List<Int>,
    @SerialName("freezing_level_height")
    val freezingLevelHeight: List<Double>,
    val visibility: List<Double>,
    @SerialName("soil_temperature_0cm")
    val soilTemperature0cm: List<Double>,
    @SerialName("soil_temperature_6cm")
    val soilTemperature6cm: List<Double>,
    @SerialName("soil_moisture_0_to_1cm")
    val soilMoisture0to1cm: List<Double>,
    @SerialName("soil_moisture_1_to_3cm")
    val soilMoisture1to3cm: List<Double>,
    @SerialName("is_day")
    val isDay: List<Boolean>
)

//Do I need these:
/*
 @SerialName("")
val hourlyRainfall: List<Double>,
val fog: List<Double>,
val dew: List<Double>,
val hail: List<Double>,
val cloudy: List<Double>,
val sunlight: List<Double>,
*/
