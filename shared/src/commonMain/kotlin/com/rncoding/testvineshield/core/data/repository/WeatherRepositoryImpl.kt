package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.WeatherHistoryDao
import com.rncoding.testvineshield.core.data.local.mappers.WeatherMapper
import com.rncoding.testvineshield.core.data.local.database.daos.VineyardDao
import com.rncoding.testvineshield.core.data.datasource.WeatherRemoteDataSource
import com.rncoding.testvineshield.core.domain.datamodels.WeatherDomainModel
import com.rncoding.testvineshield.core.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class WeatherRepositoryImpl(
    private val remoteDataSource: WeatherRemoteDataSource,
    private val vineyardDao: VineyardDao,
    private val weatherHistoryDao: WeatherHistoryDao,
    private val weatherMapper: WeatherMapper
) : WeatherRepository {

    override suspend fun refreshWeather(
        vineyardId: Long,
        latitude: Double,
        longitude: Double
    ) {
        require(vineyardId > 0L) {
            "Vineyard ID must be positive."
        }

        val timeZone = requireNotNull(
            vineyardDao.getVineyardTimeZone(vineyardId)
        ) {
            "Time zone not found for vineyard $vineyardId."
        }

        val response = remoteDataSource.fetchWeather(
            latitude = latitude,
            longitude = longitude,
            timeZone = timeZone
        )

        require(response.timezone == timeZone) {
            "Open-Meteo returned time zone '${response.timezone}' " +
                    "instead of '$timeZone'."
        }

        val fetchedAt = Clock.System.now().toEpochMilliseconds()

        val latestCompletedHourStart =
            fetchedAt - 3_600_000L

        val entities = weatherMapper.weatherDtoToEntity(
            weatherDto = response,
            vineyardId = vineyardId,
            latestCompletedHourStart = latestCompletedHourStart
        )

        if (entities.isNotEmpty()) {
            weatherHistoryDao.insertMissingWeather(entities)
        }
    }

    override fun observeLatestVineyardWeather(
        vineyardId: Long
    ): Flow<WeatherDomainModel?> {
        return weatherHistoryDao
            .observeLatestVineyardWeather(vineyardId)
            .map { entity ->
                entity?.let(
                    weatherMapper::entityToDomain
                )
            }
    }

    override fun observeHistoricalVineyardWeather(
        vineyardId: Long,
        startTimestamp: Long
    ): Flow<List<WeatherDomainModel>> {
        return weatherHistoryDao
            .observeHistoricalVineyardWeather(
                vineyardId = vineyardId,
                startTimestamp = startTimestamp
            )
            .map { entities ->
                entities.map(
                    weatherMapper::entityToDomain
                )
            }
    }
}