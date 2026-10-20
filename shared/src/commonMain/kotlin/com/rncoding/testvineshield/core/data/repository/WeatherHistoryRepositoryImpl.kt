package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.WeatherHistoryDao
import com.rncoding.testvineshield.core.data.local.mappers.WeatherHistoryMapper
import com.rncoding.testvineshield.core.data.local.database.daos.VineyardDao
import com.rncoding.testvineshield.core.data.datasource.WeatherRemoteDataSource
import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryDomainModel
import com.rncoding.testvineshield.core.domain.repository.WeatherHistoryRepository
import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryRefreshResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

class WeatherHistoryRepositoryImpl(
    private val remoteDataSource: WeatherRemoteDataSource,
    private val vineyardDao: VineyardDao,
    private val weatherHistoryDao: WeatherHistoryDao,
    private val weatherHistoryMapper: WeatherHistoryMapper
) : WeatherHistoryRepository {


    override suspend fun refreshWeatherHistory(
        vineyardId: Long,
        latitude: Double,
        longitude: Double
    ): WeatherHistoryRefreshResult {
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

        val entities = weatherHistoryMapper.weatherDtoToEntity(
            weatherDto = response,
            vineyardId = vineyardId,
            latestCompletedHourStart = latestCompletedHourStart
        )

        val insertedHours = if (entities.isEmpty()) {
            0
        } else {
            weatherHistoryDao.insertMissingWeather(entities)
                .count { rowId -> rowId != -1L }
        }

        return WeatherHistoryRefreshResult(
            vineyardId = vineyardId,
            fetchedAt = fetchedAt,
            receivedCompletedHours = entities.size,
            insertedHours = insertedHours
        )
    }


    override fun observeLatestVineyardWeather(
        vineyardId: Long
    ): Flow<WeatherHistoryDomainModel?> {
        return weatherHistoryDao
            .observeLatestVineyardWeather(vineyardId)
            .map { entity ->
                entity?.let(
                    weatherHistoryMapper::entityToDomain
                )
            }
    }

    override fun observeHistoricalVineyardWeather(
        vineyardId: Long,
        startTimestamp: Long
    ): Flow<List<WeatherHistoryDomainModel>> {
        return weatherHistoryDao
            .observeHistoricalVineyardWeather(
                vineyardId = vineyardId,
                startTimestamp = startTimestamp
            )
            .map { entities ->
                entities.map(
                    weatherHistoryMapper::entityToDomain
                )
            }
    }
}