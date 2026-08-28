package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.entities.HarvestEntity

@Dao
interface HarvestDao {
    @Upsert
    @Transaction
    suspend fun upsertHarvest(harvest: HarvestEntity)

    @Query("SELECT * FROM harvest")
    suspend fun getAllHarvests(): Flow<HarvestEntity>

    @Query("DELETE FROM harvest")
    suspend fun clearAllHarvest()


}