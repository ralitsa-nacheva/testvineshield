package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.WeatherDomainModel
import com.rncoding.testvineshield.vineyard_details.domain.Vineyard
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    suspend fun refreshWeather(vineyardId: Long,
                       latitude: Double,
                       longitude: Double)
    suspend fun observeLatestVineyardWeather(vineyardId: Long): Flow<WeatherDomainModel>

    suspend fun observeHistoricalVineyardWeather(vineyardId: Long, startDay: Long): Flow<List<WeatherDomainModel>>


}