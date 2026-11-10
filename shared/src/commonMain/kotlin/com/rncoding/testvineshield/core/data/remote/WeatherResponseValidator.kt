package com.rncoding.testvineshield.core.data.remote

import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto
import kotlinx.datetime.TimeZone

/** Validates the whole batch before filtering or any database write. No interpolation/default zeros. */
object WeatherResponseValidator {
    const val MAX_RESPONSE_HOURS = 24 * 32
    // Explicit network contract. Extra provider fields are ignored, required fields are not guessed.
    val expectedUnits: Map<String, String> = mapOf(
        "time" to "unixtime", "temperature_2m" to "°C", "relative_humidity_2m" to "%",
        "dew_point_2m" to "°C", "cloud_cover" to "%", "wind_speed_10m" to "km/h",
        "precipitation" to "mm", "rain" to "mm", "showers" to "mm", "snowfall" to "cm",
        "snow_depth" to "m", "weather_code" to "wmo code", "freezing_level_height" to "m",
        "visibility" to "m", "soil_temperature_0cm" to "°C", "soil_temperature_6cm" to "°C",
        "soil_moisture_0_to_1cm" to "m³/m³", "soil_moisture_1_to_3cm" to "m³/m³", "is_day" to ""
    )
    private val weatherCodes = setOf(0,1,2,3,45,48,51,53,55,56,57,61,63,65,66,67,71,73,75,77,80,81,82,85,86,95,96,99)

    fun validate(response: WeatherDto) {
        require(response.latitude.isFinite() && response.latitude in -90.0..90.0) { "Invalid response latitude" }
        require(response.longitude.isFinite() && response.longitude in -180.0..180.0) { "Invalid response longitude" }
        require(response.elevation.isFinite()) { "Invalid response elevation" }
        TimeZone.of(response.timezone)
        val units = requireNotNull(response.hourlyUnits) { "Missing hourly_units" }
        require(expectedUnits.all { (name, unit) -> units[name] == unit }) { "Missing or unsupported hourly units" }
        val h = response.hourlyWeather
        val count = h.timestamp.size
        require(count in 1..MAX_RESPONSE_HOURS) { "Empty or oversized hourly batch" }
        val numeric = listOf(h.hourlyTemp2m, h.relativeHumidity2m, h.dewPoint2m, h.cloudCover,
            h.windSpeed10m, h.precipitation, h.rain, h.showers, h.snowfall, h.snowDepth,
            h.freezingLevelHeight, h.visibility, h.soilTemperature0cm, h.soilTemperature6cm,
            h.soilMoisture0to1cm, h.soilMoisture1to3cm)
        require(numeric.all { it.size == count } && h.weatherCode.size == count && h.isDay.size == count) { "Hourly arrays have different lengths" }
        require(h.timestamp.all { it > 0 && it <= Long.MAX_VALUE / 1000 }) { "Invalid Unix seconds or millisecond overflow" }
        require(h.timestamp.zipWithNext().all { (a,b) -> b > a && b-a == 3600L }) { "Hourly timestamps must be unique, ascending and continuous" }
        require(numeric.all { values -> values.all { it.isFinite() } }) { "Nonfinite hourly measurement" }
        require(listOf(h.hourlyTemp2m,h.dewPoint2m,h.soilTemperature0cm,h.soilTemperature6cm).all { values -> values.all { it >= -273.15 } }) { "Temperature below absolute zero" }
        require(listOf(h.relativeHumidity2m,h.cloudCover).all { values -> values.all { it in 0.0..100.0 } }) { "Percentage out of range" }
        require(listOf(h.soilMoisture0to1cm,h.soilMoisture1to3cm).all { values -> values.all { it in 0.0..1.0 } }) { "Soil moisture out of range" }
        require(listOf(h.windSpeed10m,h.precipitation,h.rain,h.showers,h.snowfall,h.snowDepth,h.visibility).all { values -> values.all { it >= 0.0 } }) { "Negative nonnegative measurement" }
        require(h.weatherCode.all { it in weatherCodes }) { "Unsupported WMO weather code" }
        // Returned coordinates are grid-cell coordinates, not necessarily the requested coordinates.
        // No arbitrary exact-coordinate equality or fabricated missing-weather values.
    }
}
