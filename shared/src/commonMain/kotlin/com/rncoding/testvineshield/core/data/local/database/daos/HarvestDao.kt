
package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.rncoding.testvineshield.core.data.local.database.entities.HarvestEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface HarvestDao {

    @Upsert
    suspend fun upsertHarvest(harvest: HarvestEntity)

    @Insert
    suspend fun insertHarvest(harvest: HarvestEntity): Long


    @Query(
        """
    UPDATE harvest
    SET
        season = :season,
        harvest_date = :harvestDate,
        units = :units,
        quantity = :quantity,
        brix_sugar_content = :brixSugarContent
    WHERE harvest_id = :harvestId
      AND block_id = :blockId
    """
    )
    suspend fun updateHarvest(
        harvestId: Long,
        blockId: Long,
        season: String,
        harvestDate: LocalDate,
        units: String,
        quantity: Double,
        brixSugarContent: Double
    ): Int


    @Query(
        """
        SELECT * FROM harvest
        ORDER BY harvest_date DESC, harvest_id DESC
        """
    )
    fun observeHarvests(): Flow<List<HarvestEntity>>

    @Query(
        """
        SELECT * FROM harvest
        WHERE block_id = :blockId
        ORDER BY harvest_date DESC, harvest_id DESC
        """
    )
    fun observeHarvestsForBlock(
        blockId: Long
    ): Flow<List<HarvestEntity>>

    @Query(
        """
        SELECT * FROM harvest
        WHERE harvest_id = :harvestId
        LIMIT 1
        """
    )
    suspend fun getHarvestById(
        harvestId: Long
    ): HarvestEntity?

    @Query(
        """
        SELECT * FROM harvest
        WHERE block_id = :blockId
          AND harvest_id = :harvestId
        LIMIT 1
        """
    )
    suspend fun getHarvestForBlock(
        blockId: Long,
        harvestId: Long
    ): HarvestEntity?

    @Query(
        """
        DELETE FROM harvest
        WHERE block_id = :blockId
          AND harvest_id = :harvestId
        """
    )
    suspend fun deleteHarvest(
        blockId: Long,
        harvestId: Long
    ): Int

    @Query(
        """
        SELECT COUNT(*) FROM harvest
        WHERE block_id = :blockId
        """
    )
    suspend fun countHarvestsForBlock(
        blockId: Long
    ): Int

    @Query("DELETE FROM harvest")
    suspend fun clearAllHarvest()
}
