
package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.VineyardWeatherLocation

interface VineyardWeatherLocationRepository {

    suspend fun getLocation(
        userId: Long,
        vineyardId: Long
    ): VineyardWeatherLocation?
}
