package com.rncoding.testvineshield.core.data.local.database.daos

import com.rncoding.testvineshield.core.domain.datamodels.units.AreaUnit
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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
        updated_at = :updatedAt,
        size_unit = :sizeUnit
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
        updatedAt: LocalDate,
        sizeUnit: AreaUnit? = null
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
    ): VineyardEntity?

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

    @Query("SELECT vineyard_id FROM vineyard WHERE user_id = :userId ORDER BY sort_order, vineyard_id")
    suspend fun getVineyardIdsForUser(userId: Long): List<Long>

    /** A reorder replaces the complete owned order. Validation and writes share one transaction.
     * A stale/filtered/cross-owner list is rejected rather than partially persisted.
     */
    @Transaction
    suspend fun reorderVineyards(userId: Long, vineyardIds: List<Long>) {
        val requested = vineyardIds.toList()
        if (userId <= 0 || requested.any { it <= 0 } || requested.distinct().size != requested.size) {
            throw InvalidVineyardOrderException()
        }
        val owned = getVineyardIdsForUser(userId)
        if (owned.size != requested.size || owned.toSet() != requested.toSet()) {
            throw InvalidVineyardOrderException()
        }
        requested.forEachIndexed { index, vineyardId ->
            if (updateSortOrder(userId, vineyardId, index) != 1) {
                // Throwing is essential: returning an error here would commit earlier updates.
                throw InvalidVineyardOrderException()
            }
        }
    }

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

    @Query(
        """
    SELECT time_zone
    FROM vineyard
    WHERE vineyard_id = :vineyardId
    LIMIT 1
    """
    )
    suspend fun getVineyardTimeZone(
        vineyardId: Long
    ): String?



    @Query(
        """
    SELECT *
    FROM vineyard
    WHERE vineyard_id = :vineyardId
      AND user_id = :userId
    LIMIT 1
    """
    )
    suspend fun getVineyardForBackgroundWork(
        userId: Long,
        vineyardId: Long
    ): VineyardEntity?

}
/** Internal persistence validation signal, mapped to a domain error by the repository. */
internal class InvalidVineyardOrderException : IllegalArgumentException("Invalid or stale owned vineyard order.")
