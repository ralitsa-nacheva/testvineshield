package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rncoding.testvineshield.core.data.local.database.entities.VineyardEntity
import com.rncoding.testvineshield.core.data.local.database.projections.VineyardSummaryProjection
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface VineyardDao {

    @Insert(
        onConflict = OnConflictStrategy.ABORT
    )
    suspend fun insertVineyard(
        vineyard: VineyardEntity
    ): Long

    @Query(
        """
    UPDATE vineyard
    SET
        name = :name,
        size = :size,
        country = :country,
        city = :city,
        latitude = :latitude,
        longitude = :longitude,
        time_zone = :timeZone,
        elevation = :elevation,
        updated_at = :updatedAt
    WHERE vineyard_id = :vineyardId
      AND user_id = :userId
    """
    )
    suspend fun updateVineyard(
        vineyardId: Long,
        userId: Long,
        name: String,
        size: Double,
        country: String,
        city: String,
        latitude: Double,
        longitude: Double,
        timeZone: String,
        elevation: Int,
        updatedAt: LocalDate
    ): Int
    @Query(
        """
        SELECT *
        FROM vineyard
        WHERE user_id = :userId
        ORDER BY sort_order ASC, vineyard_id ASC
        """
    )
    fun observeVineyardsForUser(
        userId: Long
    ): Flow<List<VineyardEntity>>

    @Query(
        """
        SELECT *
        FROM vineyard
        WHERE vineyard_id = :vineyardId
          AND user_id = :userId
        LIMIT 1
        """
    )
    fun observeVineyard(
        userId: Long,
        vineyardId: Long
    ): Flow<VineyardEntity?>

    @Query(
        """
        SELECT *
        FROM vineyard
        WHERE vineyard_id = :vineyardId
          AND user_id = :userId
        LIMIT 1
        """
    )
    suspend fun getVineyard(
        userId: Long,
        vineyardId: Long
    ): VineyardEntity

    @Query(
        """
        DELETE FROM vineyard
        WHERE vineyard_id = :vineyardId
          AND user_id = :userId
        """
    )
    suspend fun deleteVineyard(
        userId: Long,
        vineyardId: Long
    ): Int

    @Query(
        """
        SELECT COALESCE(MAX(sort_order), -1) + 1
        FROM vineyard
        WHERE user_id = :userId
        """
    )
    suspend fun getNextSortOrder(
        userId: Long
    ): Int

    @Query(
        """
        UPDATE vineyard
        SET sort_order = :sortOrder
        WHERE vineyard_id = :vineyardId
          AND user_id = :userId
        """
    )
    suspend fun updateSortOrder(
        userId: Long,
        vineyardId: Long,
        sortOrder: Int
    ): Int

    @Query(
        """
        SELECT
            v.vineyard_id AS vineyardId,
            v.name AS name,
            v.country AS country,
            v.city AS city,
            v.sort_order AS sortOrder,

            (
                SELECT COUNT(*)
                FROM block b
                WHERE b.vineyard_id = v.vineyard_id
            ) AS blockCount,

            (
                SELECT a.priority
                FROM activity a
                WHERE a.vineyard_id = v.vineyard_id
                ORDER BY a.updated_at DESC
                LIMIT 1
            ) AS lastActivityPriority,

            (
                SELECT a.status
                FROM activity a
                WHERE a.vineyard_id = v.vineyard_id
                ORDER BY a.updated_at DESC
                LIMIT 1
            ) AS lastActivityStatus,

            (
                SELECT w.temperature_2m
                FROM weather_history w
                WHERE w.vineyard_id = v.vineyard_id
                ORDER BY w.timestamp DESC
                LIMIT 1
            ) AS latestTemperature,

            (
                SELECT da.alert
                FROM disease_alert da
                WHERE da.vineyard_id = v.vineyard_id
                  AND da.alert_status = 'active'
                ORDER BY da.created_at DESC
                LIMIT 1
            ) AS activeAlert,

            (
                SELECT COUNT(*)
                FROM disease_occurrence o
                WHERE o.vineyard_id = v.vineyard_id
                  AND o.status = 'active'
            ) AS currentDiseaseCount

        FROM vineyard v

        WHERE v.user_id = :userId

        ORDER BY
            v.sort_order ASC,
            v.vineyard_id ASC
        """
    )
    fun observeVineyardSummaries(
        userId: Long
    ): Flow<List<VineyardSummaryProjection>>
}