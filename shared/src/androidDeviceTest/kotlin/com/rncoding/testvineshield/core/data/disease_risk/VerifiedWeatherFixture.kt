package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.data.local.database.VineshieldDatabase
import com.rncoding.testvineshield.core.data.local.database.entities.WeatherHistoryEntity
import kotlin.test.assertNotNull

internal suspend fun VineshieldDatabase.insertVerifiedWeatherFixture(rows: List<WeatherHistoryEntity>): Int {
    if(rows.isEmpty()) return 0
    val dao=weatherCacheDao(); val id=rows.first().vineyardId
    val parent=assertNotNull(dao.vineyard(id))
    val request=assertNotNull(dao.begin(id,parent.latitude,parent.longitude,parent.timeZone,"history",1L))
    return assertNotNull(dao.completeHistory(request,rows,1L))
}
