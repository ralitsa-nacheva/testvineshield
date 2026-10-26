
package com.rncoding.testvineshield.core.data.repository

import com.rncoding.testvineshield.core.data.local.database.daos.BlockDao
import com.rncoding.testvineshield.core.data.local.mappers.BlockMapper
import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import com.rncoding.testvineshield.core.domain.repository.BlockRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BlockRepositoryImpl(
    private val blockDao: BlockDao,
    private val blockMapper: BlockMapper
) : BlockRepository {

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
            phenologicalStage = block.phenologicalStage
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
