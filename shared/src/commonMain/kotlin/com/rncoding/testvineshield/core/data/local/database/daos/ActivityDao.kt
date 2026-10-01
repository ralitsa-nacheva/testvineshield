package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.entities.ActivityEntity
import com.rncoding.testvineshield.core.data.local.database.relations.ActivityWithOccurrences

@Dao
interface ActivityDao {
    @Upsert
    @Transaction
    suspend fun upsertActivity(activity: ActivityEntity)

    @Query("Select * From activity")
    fun observeActivities(): Flow<List<ActivityEntity>>// should it return just activity_ids instead of complete entities

    @Query("Select * From activity Where activity_id = :activityId")
    fun observeActivityById(activityId: Long): Flow<ActivityEntity>

    @Query("Delete From activity")
    @Transaction
    suspend fun deleteAllActivities()

    @Query("Delete From activity Where activity_id = :activityId")
    suspend fun deleteActivityById(activityId: Long)

    @Query("Delete From activity Where vineyard_id = :vineyardId")
    suspend fun deleteActivitiesForVineyard(vineyardId: Long)

    @Query("Delete From activity Where block_id = :blockId")
    suspend fun deleteActivitiesForBlock(blockId: Long)

    @Transaction
    @Query("SELECT * FROM activity WHERE activity_id = :activityId")
    suspend fun getActivityWithOccurrences(activityId: Long): List<ActivityWithOccurrences>

    // use this in vineyard summary, maybe add another function to count all open activities
    @Query(
        """
    SELECT activity_type
    FROM activity
    WHERE vineyard_id = :vineyardId
      AND status IN ('planned', 'in_progress')
    ORDER BY updated_at DESC, activity_id DESC
    LIMIT 1
    """
    )
    suspend fun getLatestPendingActivityByVineyard(
        vineyardId: Long
    ): String?

    @Query("Select * From activity Where vineyard_id = :vineyardId")
    suspend fun getAllActivitiesForVineyard(vineyardId: Long): List<ActivityEntity>

    @Query("Select Count(activity_id) From activity Where vineyard_id = :vineyardId")
    suspend fun countAllActivitiesForVineyard(vineyardId: Long): Int

    @Query(
        """
    SELECT *
    FROM activity
    WHERE block_id = :blockId
      AND status IN ('planned', 'in_progress')
    ORDER BY updated_at DESC, activity_id DESC
    LIMIT 1
    """
    )
    suspend fun getLatestPendingActivityByBlock(
        blockId: Long
    ): ActivityEntity?

    @Query("Select * From activity Where block_id = :blockId")
    suspend fun getAllActivitiesForBlock(blockId: Long): List<ActivityEntity>

    @Query("Select Count(activity_id) From activity Where block_id = :blockId")
    suspend fun countAllActivitiesForBlock(blockId: Long): Int

    // get/add/delete activities by name, by season, by block, by planned date, by phenological state, by status, by priority,
    // by date of completion, by tool, by treatment product, complete activity
}