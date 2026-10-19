package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.data.local.database.daos.VineyardDao
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskVineyardContextProvider

class DefaultDiseaseRiskVineyardContextProvider(
    private val vineyardDao: VineyardDao
) : DiseaseRiskVineyardContextProvider {

    override suspend fun getTimeZone(
        vineyardId: Long
    ): String? {
        return vineyardDao.getVineyardTimeZone(
            vineyardId = vineyardId
        )
    }
}