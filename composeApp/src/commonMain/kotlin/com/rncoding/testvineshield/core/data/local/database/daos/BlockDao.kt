package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.entities.BlockEntity
import com.rncoding.testvineshield.core.data.local.database.relations.BlockWithActivities
import com.rncoding.testvineshield.core.data.local.database.relations.BlockWithHarvest

@Dao
interface BlockDao {
    @Upsert
    @Transaction
    suspend fun upsertBlock(block: BlockEntity)

    @Query("Select * From block")
    suspend fun observeBlocks(): Flow<BlockEntity>

    @Query("Delete From block")
    @Transaction
    suspend fun deleteAllBlocks()

    @Query("Delete From block Where vineyard_id = :vineyardId")
    suspend fun deleteBlocksForVineyard(vineyardId: Long)

    @Transaction
    @Query("SELECT * FROM block WHERE block_id = :blockId")
    suspend fun getBlockWithActivities(blockId: Int): List<BlockWithActivities>

    @Transaction
    @Query("SELECT * FROM block WHERE block_id = :blockId")
    suspend fun getBlockWithHarvest(blockId: Int): List<BlockWithHarvest>
    // get blocks by phenological state, get blocks by vine variety, get blocks by activity

   // use this for vineyard summary
    @Query("Select Count(block_id) From block Where vineyard_id = :vineyardId")
    suspend fun getBlockCountByVineyard(vineyardId: Long): Int

    @Query("Select * From block Where vineyard_id = :vineyardId")
    suspend fun getBlocksForVineyard(vineyardId: Long): List<BlockEntity>
}