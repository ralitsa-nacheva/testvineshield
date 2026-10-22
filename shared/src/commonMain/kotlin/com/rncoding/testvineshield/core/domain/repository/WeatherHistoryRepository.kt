
package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryDomainModel
import com.rncoding.testvineshield.core.domain.datamodels.WeatherHistoryRefreshResult
import com.rncoding.testvineshield.core.domain.error.AppError
import com.rncoding.testvineshield.core.domain.error.Result
import kotlinx.coroutines.flow.Flow

interface WeatherHistoryRepository {

    suspend fun refreshWeatherHistory(
        vineyardId: Long,
        latitude: Double,
        longitude: Double
    ): Result<WeatherHistoryRefreshResult, AppError>

    fun observeLatestVineyardWeather(
        vineyardId: Long
    ): Flow<WeatherHistoryDomainModel?>

    fun observeHistoricalVineyardWeather(
        vineyardId: Long,
        startTimestamp: Long
    ): Flow<List<WeatherHistoryDomainModel>>
}
