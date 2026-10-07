package com.rncoding.testvineshield.core.data.local

import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherHistoryDao
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherHistoryEntity

class WeatherLocalDataSource(private val weatherHistoryDao: WeatherHistoryDao) {
    suspend fun observeLatestVineyardWeather(vineyardId: Long): Flow<WeatherHistoryEntity?> =
        weatherHistoryDao.observeLatestVineyardWeather(vineyardId)

    suspend fun upsertWeather(weather: List<WeatherHistoryEntity>) =
        weatherHistoryDao.upsertWeather(weather)
}