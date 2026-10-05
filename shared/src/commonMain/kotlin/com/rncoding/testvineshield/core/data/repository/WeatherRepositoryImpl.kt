package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.WeatherHistoryDao
import com.rncoding.testvineshield.core.data.local.mappers.WeatherMapper
import com.rncoding.testvineshield.core.data.remote.WeatherRemoteDataSourceImpl
import com.rncoding.testvineshield.core.domain.datamodels.WeatherDomainModel
import com.rncoding.testvineshield.core.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WeatherRepositoryImpl(
    private val remoteDataSource: WeatherRemoteDataSourceImpl,
    private val weatherHistoryDao: WeatherHistoryDao,
    private val weatherMapper: WeatherMapper
) : WeatherRepository {

    override suspend fun refreshWeather(
        vineyardId: Long,
        latitude: Double,
        longitude: Double
    ) {
        val response =
            remoteDataSource.fetchWeather(
                latitude = latitude,
                longitude = longitude
            )

        val lastTimestamp =
            weatherHistoryDao
                .getLastWeatherTimestamp(vineyardId)

        val entities =
            weatherMapper.weatherDtoToEntity(
                weatherDto = response,
                vineyardId = vineyardId,
                lastTimestamp = lastTimestamp
            )

        for (entity in entities) {
            weatherHistoryDao.upsertWeather(entity)
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
        startDay: Long
    ): Flow<List<WeatherDomainModel>> {
        return weatherHistoryDao
            .observeHistoricalVineyardWeather(
                vineyardId = vineyardId,
                startTimestamp = startDay
            )
            .map { entities ->
                entities.map(
                    weatherMapper::entityToDomain
                )
            }
    }
}