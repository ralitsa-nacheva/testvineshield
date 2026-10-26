
package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import kotlinx.coroutines.flow.Flow

interface BlockRepository {

    fun observeBlocks(
        vineyardId: Long
    ): Flow<List<BlockDomainModel>>

    suspend fun getBlock(
        vineyardId: Long,
        blockId: Long
    ): BlockDomainModel?

    suspend fun createBlock(
        block: BlockDomainModel
    ): Long?

    suspend fun updateBlock(
        block: BlockDomainModel
    ): Boolean

    suspend fun deleteBlock(
        vineyardId: Long,
        blockId: Long
    ): Boolean
}
