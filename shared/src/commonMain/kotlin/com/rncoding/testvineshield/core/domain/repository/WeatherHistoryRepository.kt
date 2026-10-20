package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryRefreshResult
import kotlinx.coroutines.flow.Flow

interface WeatherHistoryRepository {


    suspend fun refreshWeatherHistory(
        vineyardId: Long,
        latitude: Double,
        longitude: Double
    ): WeatherHistoryRefreshResult


    fun observeLatestVineyardWeather(
        vineyardId: Long
    ): Flow<WeatherHistoryDomainModel?>

    fun observeHistoricalVineyardWeather(
        vineyardId: Long,
        startTimestamp: Long
    ): Flow<List<WeatherHistoryDomainModel>>
}