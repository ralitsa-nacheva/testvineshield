package com.rncoding.testvineshield.core.data.local

import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherDao
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherEntity

class WeatherLocalDataSource(private val weatherDao: WeatherDao) {
    suspend fun observeLatestVineyardWeather(vineyardId: Long): Flow<WeatherEntity> =
        weatherDao.observeLatestVineyardWeather(vineyardId)

    suspend fun upsertWeather(weather: WeatherEntity) =
        weatherDao.upsertWeather(weather)
}