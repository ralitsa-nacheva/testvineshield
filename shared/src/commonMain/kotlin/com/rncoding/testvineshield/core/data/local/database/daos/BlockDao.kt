package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.entities.BlockEntity
import com.rncoding.testvineshield.core.data.local.database.projections.BlockSummaryProjection
import com.rncoding.testvineshield.core.data.local.database.relations.BlockWithActivities
import com.rncoding.testvineshield.core.data.local.database.relations.BlockWithHarvest

@Dao
interface BlockDao {
    @Upsert
    @Transaction
    suspend fun upsertBlock(block: BlockEntity)

    @Query(
        """
    SELECT *
    FROM block
    ORDER BY vineyard_id, block_id
    """
    )
    fun observeBlocks(): Flow<List<BlockEntity>>

    @Query(
        """
    SELECT *
    FROM block
    WHERE vineyard_id = :vineyardId
    ORDER BY name ASC, block_id ASC
    """
    )
    fun observeBlocksForVineyard(
        vineyardId: Long
    ): Flow<List<BlockEntity>>

    @Query("Delete From block")
    @Transaction
    suspend fun deleteAllBlocks()

    @Query("Delete From block Where vineyard_id = :vineyardId")
    suspend fun deleteBlocksForVineyard(vineyardId: Long)

    @Transaction
    @Query("SELECT * FROM block WHERE block_id = :blockId")
    suspend fun getBlockWithActivities(blockId: Long): List<BlockWithActivities>

    @Transaction
    @Query("SELECT * FROM block WHERE block_id = :blockId")
    suspend fun getBlockWithHarvest(blockId: Long): List<BlockWithHarvest>
    // get blocks by phenological state, get blocks by vine variety, get blocks by activity

   // use this for vineyard summary
    @Query("Select Count(block_id) From block Where vineyard_id = :vineyardId")
    suspend fun getBlockCountByVineyard(vineyardId: Long): Int

    @Query("Select * From block Where vineyard_id = :vineyardId")
    suspend fun getBlocksForVineyard(vineyardId: Long): List<BlockEntity>

    @Query(
        """
    SELECT
        b.block_id AS block_id,
        b.name AS name,
        b.area AS area,
        b.vine_variety AS vine_variety,

        (
            SELECT a.phenological_stage
            FROM activity a
            WHERE a.block_id = b.block_id
            ORDER BY a.updated_at DESC, a.activity_id DESC
            LIMIT 1
        ) AS phenological_stage,

        (
            SELECT a.activity_type
            FROM activity a
            WHERE a.block_id = b.block_id
            ORDER BY a.updated_at DESC, a.activity_id DESC
            LIMIT 1
        ) AS latest_activity_type,

        (
            SELECT a.priority
            FROM activity a
            WHERE a.block_id = b.block_id
            ORDER BY a.updated_at DESC, a.activity_id DESC
            LIMIT 1
        ) AS latest_activity_priority,

        (
            SELECT a.status
            FROM activity a
            WHERE a.block_id = b.block_id
            ORDER BY a.updated_at DESC, a.activity_id DESC
            LIMIT 1
        ) AS latest_activity_status,

        (
            SELECT COUNT(*)
            FROM disease_occurrence d_o
            WHERE d_o.block_id = b.block_id
              AND d_o.status = 'active'
        ) AS active_disease_count,

        (
            SELECT COUNT(*)
            FROM disease_alert d_a
            WHERE d_a.block_id = b.block_id
              AND d_a.alert_status = 'active'
        ) AS active_alert_count

    FROM block b
    WHERE b.vineyard_id = :vineyardId
    ORDER BY b.name ASC, b.block_id ASC
    """
    )
    fun observeBlockSummariesForVineyard(
        vineyardId: Long
    ): Flow<List<BlockSummaryProjection>>

}