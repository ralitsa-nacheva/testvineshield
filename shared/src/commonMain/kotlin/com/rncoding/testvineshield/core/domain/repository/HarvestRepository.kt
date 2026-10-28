
package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.HarvestDomainModel
import kotlinx.coroutines.flow.Flow

interface HarvestRepository {

    fun observeHarvestsForBlock(
        blockId: Long
    ): Flow<List<HarvestDomainModel>>

    suspend fun getHarvest(
        blockId: Long,
        harvestId: Long
    ): HarvestDomainModel?

    suspend fun createHarvest(
        harvest: HarvestDomainModel
    ): Long?

    suspend fun updateHarvest(
        harvest: HarvestDomainModel
    ): Boolean

    suspend fun deleteHarvest(
        blockId: Long,
        harvestId: Long
    ): Boolean
}
