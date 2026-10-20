package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.WeatherForecastDao
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherForecastEntity
import com.rncoding.testvineshield.core.data.local.mappers.WeatherForecastMapper
import com.rncoding.testvineshield.core.data.datasource.WeatherRemoteDataSource
import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryDomainModel
import com.rncoding.testvineshield.core.domain.repositories.WeatherForecastRepository
import com.rncoding.testvineshield.core.data.local.database.daos.VineyardDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WeatherForecastRepositoryImpl(
    private val remoteDataSource: WeatherRemoteDataSource,
    private val vineyardDao: VineyardDao,
    private val forecastDao: WeatherForecastDao,
    private val forecastMapper: WeatherForecastMapper
) : WeatherForecastRepository {

    override fun observeForecast(
        vineyardId: Long
    ): Flow<List<WeatherHistoryDomainModel>> {
        return forecastDao
            .observeForecastWeather(vineyardId)
            .map { entities ->
                entities.map(::entityToDomain)
            }
    }

    override fun observeNextForecast(
        vineyardId: Long,
        fromTimestamp: Long
    ): Flow<WeatherHistoryDomainModel?> {
        return forecastDao
            .observeNextForecastWeather(
                vineyardId = vineyardId,
                fromTimestamp = fromTimestamp
            )
            .map { entity ->
                entity?.let(::entityToDomain)
            }
    }

    override suspend fun refreshForecast(
        vineyardId: Long,
        latitude: Double,
        longitude: Double,
        fetchedAt: Long
    ) {
        require(vineyardId > 0L) {
            "Vineyard ID must be positive."
        }

        require(latitude in -90.0..90.0) {
            "Invalid vineyard latitude."
        }

        require(longitude in -180.0..180.0) {
            "Invalid vineyard longitude."
        }

        val timeZone = requireNotNull(
            vineyardDao.getVineyardTimeZone(vineyardId)
        ) {
            "Time zone not found for vineyard $vineyardId."
        }

        val response = remoteDataSource.fetchWeatherForecast(
            latitude = latitude,
            longitude = longitude,
            timeZone = timeZone
        )

        require(response.timezone == timeZone) {
            "Open-Meteo returned time zone '${response.timezone}' " +
                    "instead of '$timeZone'."
        }

        val entities = forecastMapper.weatherDtoToEntities(
            weatherDto = response,
            vineyardId = vineyardId,
            forecastCreatedAt = fetchedAt,
            fromTimestamp = fetchedAt
        )

        require(entities.size >= MINIMUM_FORECAST_HOURS) {
            "Insufficient forecast coverage. " +
                    "Expected at least $MINIMUM_FORECAST_HOURS hourly observations, " +
                    "but received ${entities.size}."
        }

        require(
            entities.zipWithNext().all { (previous, current) ->
                current.timestamp - previous.timestamp == ONE_HOUR_MILLIS
            }
        ) {
            "Forecast observations contain missing, duplicate, or unordered hours."
        }

        require(
            entities.first().timestamp >= fetchedAt
        ) {
            "Forecast contains observations before the requested cutoff."
        }

        forecastDao.replaceForecast(
            vineyardId = vineyardId,
            forecast = entities
        )
    }

    private fun entityToDomain(
        entity: WeatherForecastEntity
    ): WeatherHistoryDomainModel {
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

    private companion object {
        const val ONE_HOUR_MILLIS = 3_600_000L
        const val MINIMUM_FORECAST_HOURS = 24
    }
}

