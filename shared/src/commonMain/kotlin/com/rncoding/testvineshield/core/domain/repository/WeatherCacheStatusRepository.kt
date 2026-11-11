package com.rncoding.testvineshield.core.domain.repository
import com.rncoding.testvineshield.core.domain.datamodels.WeatherCacheStatus
import kotlinx.coroutines.flow.Flow
interface WeatherCacheStatusRepository { fun observe(vineyardId: Long): Flow<List<WeatherCacheStatus>> }
