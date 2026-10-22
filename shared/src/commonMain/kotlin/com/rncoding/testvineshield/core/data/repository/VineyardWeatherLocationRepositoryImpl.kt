
package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.VineyardDao
import com.rncoding.testvineshield.core.domain.datamodels.VineyardWeatherLocation
import com.rncoding.testvineshield.core.domain.repository.VineyardWeatherLocationRepository

class VineyardWeatherLocationRepositoryImpl(
    private val vineyardDao: VineyardDao
) : VineyardWeatherLocationRepository {


    override suspend fun getLocation(
        userId: Long,
        vineyardId: Long
    ): VineyardWeatherLocation? {
        require(userId > 0L) {
            "User ID must be positive."
        }
        require(vineyardId > 0L) {
            "Vineyard ID must be positive."
        }

        val vineyard = vineyardDao.getVineyardForBackgroundWork(
            userId = userId,
            vineyardId = vineyardId
        ) ?: return null

        return VineyardWeatherLocation(
            vineyardId = vineyard.vineyardId,
            latitude = vineyard.latitude,
            longitude = vineyard.longitude
        )
    }

}
