package com.rncoding.testvineshield.core.data.repository
import com.rncoding.testvineshield.core.data.local.database.daos.WeatherCacheDao
import com.rncoding.testvineshield.core.domain.datamodels.WeatherCacheStatus
import com.rncoding.testvineshield.core.domain.repository.WeatherCacheStatusRepository
import kotlinx.coroutines.flow.map
class WeatherCacheStatusRepositoryImpl(private val dao: WeatherCacheDao): WeatherCacheStatusRepository {
    override fun observe(vineyardId: Long)=dao.observeStatus(vineyardId).map { rows -> rows.map {
        WeatherCacheStatus(it.kind,it.state,it.attemptedAt,it.succeededAt,it.coverageStart,it.coverageEnd,it.hours,it.error)
    } }
}
