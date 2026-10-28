
package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.HarvestDao
import com.rncoding.testvineshield.core.data.local.mappers.HarvestMapper
import com.rncoding.testvineshield.core.domain.datamodels.HarvestDomainModel
import com.rncoding.testvineshield.core.domain.repository.HarvestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HarvestRepositoryImpl(
    private val harvestDao: HarvestDao,
    private val harvestMapper: HarvestMapper
) : HarvestRepository {

    override fun observeHarvestsForBlock(
        blockId: Long
    ): Flow<List<HarvestDomainModel>> {
        require(blockId > 0L)

        return harvestDao.observeHarvestsForBlock(blockId)
            .map { entities ->
                entities.map(harvestMapper::toDomain)
            }
    }

    override suspend fun getHarvest(
        blockId: Long,
        harvestId: Long
    ): HarvestDomainModel? {
        require(blockId > 0L)
        require(harvestId > 0L)

        return harvestDao.getHarvestForBlock(
            blockId = blockId,
            harvestId = harvestId
        )?.let(harvestMapper::toDomain)
    }

    override suspend fun createHarvest(
        harvest: HarvestDomainModel
    ): Long? {
        require(harvest.harvestId == 0L)
        require(harvest.blockId > 0L)

        return harvestDao.insertHarvest(
            harvestMapper.toEntity(harvest)
        ).takeIf { it > 0L }
    }


    override suspend fun updateHarvest(
        harvest: HarvestDomainModel
    ): Boolean {
        require(harvest.harvestId > 0L)
        require(harvest.blockId > 0L)

        val updatedRows = harvestDao.updateHarvest(
            harvestId = harvest.harvestId,
            blockId = harvest.blockId,
            season = harvest.season,
            harvestDate = harvest.harvestDate,
            units = harvest.units,
            quantity = harvest.quantity,
            brixSugarContent = harvest.brixSugarContent
        )

        return updatedRows == 1
    }

    override suspend fun deleteHarvest(
        blockId: Long,
        harvestId: Long
    ): Boolean {
        require(blockId > 0L)
        require(harvestId > 0L)

        return harvestDao.deleteHarvest(
            blockId = blockId,
            harvestId = harvestId
        ) > 0
    }
}
