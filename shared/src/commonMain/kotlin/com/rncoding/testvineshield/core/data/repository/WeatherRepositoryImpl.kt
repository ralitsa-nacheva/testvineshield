package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.WeatherHistoryDao
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherForecastDao
import com.rncoding.testvineshield.core.data.local.mappers.WeatherMapper
import com.rncoding.testvineshield.core.data.remote.OpenMeteoApi
import com.rncoding.testvineshield.core.data.remote.WeatherRemoteDataSourceImpl
import com.rncoding.testvineshield.core.domain.datamodels.WeatherDomainModel
import com.rncoding.testvineshield.core.domain.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WeatherRepositoryImpl(
    private val remoteDataSource: WeatherRemoteDataSourceImpl,
    private val weatherHistoryDao: WeatherHistoryDao,
    private val forecastDao: WeatherForecastDao,
    private val weatherApi: OpenMeteoApi,
    private val weatherMapper: WeatherMapper
) : WeatherRepository {

    override suspend fun refreshWeather(
        vineyardId: Long,
        latitude: Double,
        longitude: Double
    ) {
        val response = remoteDataSource.fetchWeather(latitude, longitude)

        val lastTimestamp = weatherHistoryDao.getLastWeatherTimestamp(vineyardId)

        val entities = weatherMapper.weatherDtoToEntity(response, vineyardId, lastTimestamp )

        for (i in entities.indices){
            val entity = entities[i]
            weatherHistoryDao.upsertWeather(entity)
        }

    }

    override suspend fun observeLatestVineyardWeather(vineyardId: Long): Flow<WeatherDomainModel> {
        return weatherHistoryDao.observeLatestVineyardWeather(vineyardId)
            .map{entity -> weatherMapper.entityToDomain(entity)}

    }

    override suspend fun observeHistoricalVineyardWeather(
        vineyardId: Long,
        startDay: Long
    ): Flow<List<WeatherDomainModel>> {
        return weatherHistoryDao.observeHistoricalVineyardWeather(vineyardId, startDay)
            .map {entities ->
                entities.map {entity -> weatherMapper.entityToDomain(entity)}
            }
    }


}