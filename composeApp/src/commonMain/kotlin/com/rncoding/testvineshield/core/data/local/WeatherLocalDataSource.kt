package com.rncoding.testvineshield.core.data.local

import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherDao
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherEntity

class WeatherLocalDataSource(private val weatherDao: WeatherDao) {
    suspend fun observeWeather(): Flow<WeatherEntity> =
        weatherDao.observeVineyardWeather()

    suspend fun upsertWeather(weather: WeatherEntity) =
        weatherDao.upsertWeather(weather)
}