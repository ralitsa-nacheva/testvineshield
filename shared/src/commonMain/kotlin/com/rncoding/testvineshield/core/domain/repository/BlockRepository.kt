
package com.rncoding.testvineshield.core.domain.repository

import com.rncoding.testvineshield.core.domain.datamodels.BlockDomainModel
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import com.rncoding.testvineshield.core.domain.datamodels.enums.PhenologicalStage
import com.rncoding.testvineshield.core.domain.datamodels.BlockStageObservation

interface BlockRepository {
    fun observeStagesOn(
        ownerId: Long, vineyardId: Long, date: LocalDate
    ): Flow<Map<Long, PhenologicalStage>>

    suspend fun getRecentStageObservations(ownerId: Long, vineyardId: Long, blockId: Long): List<BlockStageObservation>


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
