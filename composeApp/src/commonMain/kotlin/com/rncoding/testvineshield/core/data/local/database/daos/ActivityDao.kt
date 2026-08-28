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
    suspend fun getAllActivities(): Flow<List<ActivityEntity>>

    @Query("Select * From activity Where activity_id = :activityId")
    suspend fun getActivityById(activityId: Long): Flow<ActivityEntity>

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
    @Query("Select activity From activity Where vineyard_id = :vineyardId And status = 'Open' " +
            "And created_at = (Select Max(created_at) From activity Where vineyard_id = :vineyardId)")
    suspend fun getLastOpenActivityByVineyard(vineyardId: Long): String

    @Query("Select * From activity Where vineyard_id = :vineyardId")
    suspend fun getAllActivitiesForVineyard(vineyardId: Long): List<ActivityEntity>

    @Query("Select Count(activity_id) From activity Where vineyard_id = :vineyardId")
    suspend fun countAllActivitiesForVineyard(vineyardId: Long): Int

    @Query("Select * From activity Where block_id = :blockId And status = 'Open' " +
            "And created_at =(Select Max(created_at) From activity Where block_id = :blockId)")
    suspend fun getLastActivityByBlock(blockId: Long): ActivityEntity

    @Query("Select * From activity Where block_id = :blockId")
    suspend fun getAllActivitiesForBlock(blockId: Long): List<ActivityEntity>

    @Query("Select Count(activity_id) From activity Where block_id = :blockId")
    suspend fun countAllActivitiesForBlock(blockId: Long): Int

    // get/add/delete activities by name, by season, by block, by planned date, by phenological state, by status, by priority,
    // by date of completion, by tool, by treatment product, complete activity
}