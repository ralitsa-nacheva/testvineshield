
package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.BlockDao
import com.rncoding.testvineshield.core.data.local.mappers.BlockMapper
import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.repository.BlockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import com.rncoding.testvineshield.core.domain.datamodels.BlockStageObservation
import kotlinx.coroutines.flow.map

class BlockRepositoryImpl(
    private val blockDao: BlockDao,
    private val blockMapper: BlockMapper
) : BlockRepository {
    override fun observeStagesOn(
        ownerId: Long, vineyardId: Long, date: LocalDate
    ): Flow<Map<Long, PhenologicalStage>> =
        blockDao.observeStageObservations(ownerId, vineyardId, date)
            .map { rows -> rows.associate { it.blockId to it.stage } }

    override suspend fun getRecentStageObservations(
        ownerId: Long,
        vineyardId: Long,
        blockId: Long
    ): List<BlockStageObservation> =
        blockDao.getRecentStageObservations(ownerId, vineyardId, blockId)
            .map { BlockStageObservation(it.observedOn, it.stage) }


    override fun observeBlocks(
        vineyardId: Long
    ): Flow<List<BlockDomainModel>> {
        require(vineyardId > 0L) {
            "Vineyard ID must be positive."
        }

        return blockDao.observeBlocksForVineyard(vineyardId)
            .map { entities ->
                entities.map(blockMapper::toDomain)
            }
    }

    override suspend fun getBlock(
        vineyardId: Long,
        blockId: Long
    ): BlockDomainModel? {
        require(vineyardId > 0L)
        require(blockId > 0L)

        return blockDao.getBlockById(
            vineyardId = vineyardId,
            blockId = blockId
        )?.let(blockMapper::toDomain)
    }

    override suspend fun createBlock(
        block: BlockDomainModel
    ): Long? {
        require(block.blockId == 0L) {
            "A new block must have ID 0."
        }
        require(block.vineyardId > 0L)

        val id = blockDao.insertBlock(
            blockMapper.toEntity(block)
        )

        return id.takeIf { it > 0L }
    }

    override suspend fun updateBlock(
        block: BlockDomainModel
    ): Boolean {
        require(block.blockId > 0L)
        require(block.vineyardId > 0L)

        val affectedRows = blockDao.updateBlockFields(
            vineyardId = block.vineyardId,
            blockId = block.blockId,
            name = block.name,
            area = block.area,
            vineVariety = block.vineVariety,
            color = block.color,
            rootStock = block.rootStock,
            rowCount = block.rowCount,
            rowSpacing = block.rowSpacing,
            plantedAt = block.plantedAt,
            phenologicalStage = block.phenologicalStage,
            areaUnit = block.areaUnit,
            rowSpacingUnit = block.rowSpacingUnit,
            stageObservedOn = block.stageObservedOn
        )

        return affectedRows > 0
    }

    override suspend fun deleteBlock(
        vineyardId: Long,
        blockId: Long
    ): Boolean {
        require(vineyardId > 0L)
        require(blockId > 0L)

        return blockDao.deleteBlockById(
            vineyardId = vineyardId,
            blockId = blockId
        ) > 0
    }
}
