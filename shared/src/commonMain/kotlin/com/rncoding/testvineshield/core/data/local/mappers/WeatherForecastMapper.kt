package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.entities.WeatherForecastEntity
import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto

class WeatherForecastMapper {

    fun weatherDtoToEntities(
        weatherDto: WeatherDto,
        vineyardId: Long,
        forecastCreatedAt: Long,
        fromTimestamp: Long
    ): List<WeatherForecastEntity> {

        val hourly = weatherDto.hourlyWeather
        val expectedSize = hourly.timestamp.size

        require(expectedSize > 0) {
            "Open-Meteo returned an empty forecast."
        }

        val fieldSizes = listOf(
            hourly.hourlyTemp2m.size,
            hourly.relativeHumidity2m.size,
            hourly.dewPoint2m.size,
            hourly.cloudCover.size,
            hourly.windSpeed10m.size,
            hourly.precipitation.size,
            hourly.rain.size,
            hourly.showers.size,
            hourly.snowfall.size,
            hourly.snowDepth.size,
            hourly.weatherCode.size,
            hourly.freezingLevelHeight.size,
            hourly.visibility.size,
            hourly.soilTemperature0cm.size,
            hourly.soilTemperature6cm.size,
            hourly.soilMoisture0to1cm.size,
            hourly.soilMoisture1to3cm.size,
            hourly.isDay.size
        )

        require(fieldSizes.all { it == expectedSize }) {
            "Open-Meteo forecast contains inconsistent hourly array lengths."
        }

        return hourly.timestamp.indices.mapNotNull { index ->

            // Open-Meteo returns Unix seconds.
            // Room stores epoch milliseconds.
            val timestamp = hourly.timestamp[index] * 1_000L

            if (timestamp < fromTimestamp) {
                return@mapNotNull null
            }

            WeatherForecastEntity(
                weatherId = 0L,
                vineyardId = vineyardId,
                timestamp = timestamp,
                hourlyTemp2m = hourly.hourlyTemp2m[index],
                relativeHumidity2m = hourly.relativeHumidity2m[index],
                dewPoint2m = hourly.dewPoint2m[index],
                cloudCover = hourly.cloudCover[index],
                windSpeed10m = hourly.windSpeed10m[index],
                precipitation = hourly.precipitation[index],
                rain = hourly.rain[index],
                showers = hourly.showers[index],
                snowfall = hourly.snowfall[index],
                snowDepth = hourly.snowDepth[index],
                weatherCode = hourly.weatherCode[index],
                freezingLevelHeight = hourly.freezingLevelHeight[index],
                visibility = hourly.visibility[index],
                soilTemperature0cm = hourly.soilTemperature0cm[index],
                soilTemperature6cm = hourly.soilTemperature6cm[index],
                soilMoisture0to1cm = hourly.soilMoisture0to1cm[index],
                soilMoisture1to3cm = hourly.soilMoisture1to3cm[index],
                isDay = hourly.isDay[index],
                forecastCreatedAt = forecastCreatedAt
            )
        }
    }
}