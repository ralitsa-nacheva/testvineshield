package com.rncoding.testvineshield.core.data.local.mappers

import com.rncoding.testvineshield.core.data.local.database.entities.WeatherHistoryEntity
import com.rncoding.testvineshield.core.domain.datamodels.VineyardWeatherSummary

class VineyardWeatherSummaryMapper {

    fun toDomain(
        entity: WeatherHistoryEntity
    ): VineyardWeatherSummary {
        return VineyardWeatherSummary(
            timestamp = entity.timestamp,
            temperature = entity.hourlyTemp2m,
            relativeHumidity = entity.relativeHumidity2m,
            precipitation = entity.precipitation,
            windSpeed = entity.windSpeed10m,
            weatherCode = entity.weatherCode
        )
    }
}