package com.rncoding.testvineshield.core.domain.repositories

import com.rncoding.testvineshield.core.domain.datamodels.WeatherDomainModel
import kotlinx.coroutines.flow.Flow

interface WeatherForecastRepository {

    /**
     * Observe the stored forecast for a vineyard.
     *
     * Observations are ordered by timestamp ascending.
     */
    fun observeForecast(
        vineyardId: Long
    ): Flow<List<WeatherDomainModel>>

    /**
     * Observe the next forecast observation at or after
     * the specified epoch-millisecond timestamp.
     */
    fun observeNextForecast(
        vineyardId: Long,
        fromTimestamp: Long
    ): Flow<WeatherDomainModel?>

    /**
     * Fetch a fresh forecast and atomically replace
     * the stored forecast for this vineyard.
     *
     * The implementation must validate the response
     * before replacing existing records.
     *
     * @param fetchedAt Epoch milliseconds representing
     *                  the refresh time.
     */
    suspend fun refreshForecast(
        vineyardId: Long,
        latitude: Double,
        longitude: Double,
        fetchedAt: Long
    )
}