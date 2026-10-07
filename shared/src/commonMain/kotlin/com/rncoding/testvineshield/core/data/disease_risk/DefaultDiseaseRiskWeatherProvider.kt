package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.data.local.database.daos.WeatherHistoryDao
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherHistoryEntity
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherProvider
import com.rncoding.testvineshield.core.domain.disease_risk.wetness.DiseaseRiskWeatherEnricher

class DefaultDiseaseRiskWeatherProvider(
    private val weatherHistoryDao: WeatherHistoryDao,
    private val weatherEnricher: DiseaseRiskWeatherEnricher
) : DiseaseRiskWeatherProvider {

    override suspend fun getHistoricalWeather(
        vineyardId: Long,
        startTimestamp: Long,
        endTimestamp: Long
    ): List<DiseaseRiskWeatherPoint> {

        if (startTimestamp > endTimestamp) {
            return emptyList()
        }

        return weatherHistoryDao
            .getWeatherForRiskWindow(
                vineyardId = vineyardId,
                startTimestamp = startTimestamp,
                endTimestamp = endTimestamp
            )
            .map { entity ->
                entity.toRiskWeatherPoint()
            }
            .let(weatherEnricher::enrich)
    }

    private fun WeatherHistoryEntity.toRiskWeatherPoint():
            DiseaseRiskWeatherPoint {

        return DiseaseRiskWeatherPoint(
            timestamp = timestamp,
            temperatureCelsius = hourlyTemp2m,
            relativeHumidityPercent = relativeHumidity2m,
            dewPointCelsius = dewPoint2m,
            precipitationMm = precipitation,
            soilMoisture0To1Cm = soilMoisture0to1cm,
            soilMoisture1To3Cm = soilMoisture1to3cm,
            leafWetness = null,
            isDay = isDay
        )
    }
}