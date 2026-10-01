package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.entities.WeatherHistoryEntity
import com.rncoding.testvineshield.core.data.remote.dto.WeatherDto
import com.rncoding.testvineshield.core.domain.datamodels.WeatherDomainModel
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.collections.List

class WeatherMapper {

    fun weatherDtoToEntity(
        weatherDto: WeatherDto,
        vineyardId: Long,
        lastTimestamp: Long?
    ):List<WeatherHistoryEntity> {

        val weatherValues = mutableListOf<WeatherHistoryEntity>()
        val timezone = weatherDto.timezone

        for (i in weatherDto.hourlyWeather.timestamp.indices) {
            val timestamp = parseIsoTime(
                weatherDto.hourlyWeather.timestamp[i],
                TimeZone.of(timezone)
            )
            if (lastTimestamp != null && timestamp <= lastTimestamp) {
                continue
            }
            weatherValues.add(
                WeatherHistoryEntity(
                     weatherId = 0L,
                     vineyardId = vineyardId,
                     timestamp = timestamp,
                     hourlyTemp2m = weatherDto.hourlyWeather.hourlyTemp2m[i],
                     relativeHumidity2m = weatherDto.hourlyWeather.relativeHumidity2m[i],
                     dewPoint2m = weatherDto.hourlyWeather.dewPoint2m[i],
                     cloudCover = weatherDto.hourlyWeather.cloudCover[i],
                     windSpeed10m = weatherDto.hourlyWeather.windSpeed10m[i],
                     precipitation = weatherDto.hourlyWeather.precipitation[i],
                     rain = weatherDto.hourlyWeather.rain[i],
                     showers = weatherDto.hourlyWeather.showers[i],
                     snowfall = weatherDto.hourlyWeather.snowfall[i],
                     snowDepth = weatherDto.hourlyWeather.snowDepth[i],
                     weatherCode = weatherDto.hourlyWeather.weatherCode[i],
                     freezingLevelHeight = weatherDto.hourlyWeather.freezingLevelHeight[i],
                     visibility = weatherDto.hourlyWeather.visibility[i],
                     soilTemperature0cm = weatherDto.hourlyWeather.soilTemperature0cm[i],
                     soilTemperature6cm = weatherDto.hourlyWeather.soilTemperature6cm[i],
                     soilMoisture0to1cm = weatherDto.hourlyWeather.soilMoisture0to1cm[i],
                     soilMoisture1to3cm = weatherDto.hourlyWeather.soilMoisture1to3cm[i],
                     isDay = weatherDto.hourlyWeather.isDay[i]
                )
            )

        }
        return weatherValues

    }

    private fun parseIsoTime(time: String, zone: TimeZone): Long {
        return LocalDateTime
            .parse(time)
            .toInstant(zone)
            .toEpochMilliseconds()
    }


    fun entityToDomain(entity: WeatherHistoryEntity): WeatherDomainModel {
        return WeatherDomainModel(
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