package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import com.rncoding.testvineshield.core.data.local.database.relations.VineyardWithActivities
import com.rncoding.testvineshield.core.data.local.database.relations.VineyardWithBlocks
import com.rncoding.testvineshield.core.data.local.database.relations.VineyardWithCalculations
import com.rncoding.testvineshield.core.domain.datamodels.VineyardWithSummaryAggregate


@Dao
interface VineyardDao {
    @Upsert
    @Transaction
    suspend fun upsertVineyard(vineyard: VineyardEntity)

    @Query("Select * From vineyard")
    suspend fun getAllVineyards(): List<VineyardEntity>

    //@Query("Select * From vineyard Where vineyard_id = :vineyardId And user_id = :userId")
    //suspend fun getVineyardForUser(vineyardId: Long, userId: Long): Flow<VineyardEntity>

    @Query("Select * From vineyard Where user_id = :userId")
    suspend fun getAllVineyardsForUser(userId: Long): Flow<List<VineyardEntity>>

    @Query("SELECT * FROM vineyard WHERE vineyard_id = :vineyardId")
    suspend fun getVineyardById(vineyardId: Long): Flow<VineyardEntity>

    @Query("Select vineyard_id from vineyard")
    suspend fun getAllVineyardIds(): List<VineyardEntity>

   // @Query("Select vineyard_id from vineyard Where user_id = :userId")
   // suspend fun getAllVineyardIdsByUser(userId: Long): List<VineyardEntity>

    @Query("Select * from vineyard Where city = :city")
    suspend fun getVineyardByCity(city: String): List<VineyardEntity>

    @Query("Select * From vineyard Where name = :name")
    suspend fun getVineyardByName(name: String): List<VineyardEntity>

    @Query("Delete From vineyard")
    @Transaction
    suspend fun deleteAllVineyards()

    //@Query("Delete From vineyard Where user_id = :userId")
    //@Transaction
    //suspend fun deleteAllVineyardsForUser(userId: Long)

    //@Query("Delete From vineyard Where user_id = :userId and vineyard_id = :vineyardId")
    //@Transaction
    //suspend fun deleteVineyardForUser(userId: Long, vineyardId: Long)

    @Query("Delete From vineyard Where vineyard_id = :vineyardId")
    suspend fun deleteVineyardById(vineyardId: Long)

    @Transaction
    @Query("SELECT * FROM vineyard WHERE vineyard_id = :vineyardId")
    suspend fun getVineyardWithBlocks(vineyardId: Long): VineyardWithBlocks

    @Transaction
    @Query("Select * From vineyard Where vineyard_id = :vineyardId")
    suspend fun  getVineyardWithCalculations(vineyardId: Long): VineyardWithCalculations

    @Transaction
    @Query("Select * From vineyard Where vineyard_id = :vineyardId")
    suspend fun getVineyardWithActivities(vineyardId: Long): VineyardWithActivities

    @Transaction
    @Query("""
        SELECT 
            v.name,
            v.country,
            v.city,    
                    
            (SELECT COUNT(*) 
             FROM block b 
             WHERE b.vineyard_id = v.vineyard_id) AS totalBlocks,

            (SELECT a.priority 
             FROM activity a
             WHERE a.vineyard_id = v.vineyard_id
             ORDER BY a.updated_at DESC
             LIMIT 1) AS lastActivityPriority,

            (SELECT a.status 
             FROM activity a
             WHERE a.vineyard_id = v.vineyard_id
             ORDER BY a.updated_at DESC
             LIMIT 1) AS lastActivityStatus,

            
            (SELECT w.temperature_2m 
             FROM weather w
             WHERE w.vineyard_id = v.vineyard_id
             ORDER BY w.timestamp DESC
             LIMIT 1) AS latestTemp,
            
            (SELECT da.alert 
             FROM disease_alert da
             WHERE da.vineyard_id = v.vineyard_id
             ORDER BY da.created_at DESC
             LIMIT 1) AS latestAlert,
           
            (SELECT COUNT(*) 
             FROM disease_occurrence o
             WHERE o.vineyard_id = v.vineyard_id
               AND o.status = "Open") AS totalOccurrences

        FROM vineyard v
        WHERE v.vineyard_id IN (:vineyardIds)
    """)
    suspend fun getVineyardListWithSummary(vineyardIds: List<Long>): List<VineyardWithSummaryAggregate>

    suspend fun getVineyardDetailedSummary()


}