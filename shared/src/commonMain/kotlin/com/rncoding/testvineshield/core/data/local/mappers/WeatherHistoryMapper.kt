package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.entities.WeatherHistoryEntity
import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto
import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryDomainModel
import kotlin.collections.List

class WeatherHistoryMapper {

    fun weatherDtoToEntity(
        weatherDto: WeatherDto,
        vineyardId: Long,
        latestCompletedHourStart: Long
    ): List<WeatherHistoryEntity> {

        val hourly = weatherDto.hourlyWeather

        val weatherValues = mutableListOf<WeatherHistoryEntity>()

        for (i in hourly.timestamp.indices) {

            // Open-Meteo returns Unix seconds.
            // Preserve the exact timestamp without UTC-hour rounding.
            val timestamp = hourly.timestamp[i] * 1_000L

            // Exclude the current incomplete hour and future hours.
            if (timestamp > latestCompletedHourStart) {
                continue
            }

            weatherValues.add(
                WeatherHistoryEntity(
                    weatherId = 0L,
                    vineyardId = vineyardId,
                    timestamp = timestamp,
                    hourlyTemp2m = hourly.hourlyTemp2m[i],
                    relativeHumidity2m = hourly.relativeHumidity2m[i],
                    dewPoint2m = hourly.dewPoint2m[i],
                    cloudCover = hourly.cloudCover[i],
                    windSpeed10m = hourly.windSpeed10m[i],
                    precipitation = hourly.precipitation[i],
                    rain = hourly.rain[i],
                    showers = hourly.showers[i],
                    snowfall = hourly.snowfall[i],
                    snowDepth = hourly.snowDepth[i],
                    weatherCode = hourly.weatherCode[i],
                    freezingLevelHeight = hourly.freezingLevelHeight[i],
                    visibility = hourly.visibility[i],
                    soilTemperature0cm = hourly.soilTemperature0cm[i],
                    soilTemperature6cm = hourly.soilTemperature6cm[i],
                    soilMoisture0to1cm = hourly.soilMoisture0to1cm[i],
                    soilMoisture1to3cm = hourly.soilMoisture1to3cm[i],
                    isDay = hourly.isDay[i]
                )
            )
        }

        return weatherValues
    }

    fun entityToDomain(entity: WeatherHistoryEntity): WeatherHistoryDomainModel {
        return WeatherHistoryDomainModel(
            vineyardId = entity.vineyardId,
            timestamp = entity.timestamp,
            hourlyTemp2m = entity.hourlyTemp2m,
            relativeHumidity2m = entity.relativeHumidity2m,
            dewPoint2m = entity.dewPoint2m,
            cloudCover = entity.cloudCover,
            windSpeed10m = entity.windSpeed10m,
            precipitation = entity.precipitation,
            rain = entity.rain,
            showers = entity.showers,
            snowfall = entity.snowfall,
            snowDepth = entity.snowDepth,
            weatherCode = entity.weatherCode,
            freezingLevelHeight = entity.freezingLevelHeight,
            visibility = entity.visibility,
            soilTemperature0cm = entity.soilTemperature0cm,
            soilTemperature6cm = entity.soilTemperature6cm,
            soilMoisture0to1cm = entity.soilMoisture0to1cm,
            soilMoisture1to3cm = entity.soilMoisture1to3cm,
            isDay = entity.isDay
        )
    }
}