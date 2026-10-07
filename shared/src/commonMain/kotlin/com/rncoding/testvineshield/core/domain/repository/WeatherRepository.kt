package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.WeatherDomainModel
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {

    suspend fun refreshWeather(
        vineyardId: Long,
        latitude: Double,
        longitude: Double
    )

    fun observeLatestVineyardWeather(
        vineyardId: Long
    ): Flow<WeatherDomainModel?>

    fun observeHistoricalVineyardWeather(
        vineyardId: Long,
        startTimestamp: Long
    ): Flow<List<WeatherDomainModel>>
}