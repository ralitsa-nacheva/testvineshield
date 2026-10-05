package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseAlertEntity
import com.rncoding.testvineshield.core.data.local.database.projections.DiseaseAlertSummaryProjection


@Dao
interface DiseaseAlertDao {
    @Upsert
    @Transaction
    suspend fun upsertDiseaseAlert(alert: DiseaseAlertEntity)

    @Query(
        """
    SELECT *
    FROM disease_alert
    ORDER BY created_at DESC, alert_id DESC
    """
    )
    fun observeAllAlerts(): Flow<List<DiseaseAlertEntity>>

    @Query("Select * From disease_alert Where vineyard_id = :vineyardId")
    suspend fun getAllAlertsForVineyard(vineyardId: Long): List<DiseaseAlertEntity>

    @Query("Delete From disease_alert")
    @Transaction
    suspend fun clearAllAlerts()

    @Query("Delete From disease_alert Where vineyard_id = :vineyardId")
    suspend fun deleteAllAlertsForVineyard(vineyardId: Long)

    @Query("Delete From disease_alert Where alert_id = :alertId")
    suspend fun deleteAlertById(alertId: Long)

    @Query(
        """
    SELECT *
    FROM disease_alert
    WHERE vineyard_id = :vineyardId
      AND alert_status = 'active'
    ORDER BY created_at DESC, alert_id DESC
    """
    )
    fun observeActiveAlerts(
        vineyardId: Long
    ): Flow<List<DiseaseAlertEntity>>

    // use this for vineyard summary
    @Query(
        """
    SELECT COUNT(alert_id)
    FROM disease_alert
    WHERE vineyard_id = :vineyardId
      AND alert_status = 'active'
    """
    )
    suspend fun getActiveAlertsCountByVineyard(
        vineyardId: Long
    ): Int

    // use this for vineyard summary aggregate
    @Query(
        """
    SELECT alert
    FROM disease_alert
    WHERE vineyard_id = :vineyardId
      AND alert_status = 'active'
    ORDER BY created_at DESC, alert_id DESC
    LIMIT 1
    """
    )
    suspend fun getLastActiveAlertByVineyard(
        vineyardId: Long
    ): String?

    @Query(
        """
    SELECT
        d_a.alert_id AS alert_id,
        d_a.disease_id AS disease_id,
        d.name AS disease_name,
        d_a.block_id AS block_id,
        b.name AS block_name,
        d_a.created_at AS created_at,
        d_a.alert AS alert,
        d_a.alert_status AS alert_status,
        d_a.alert_severity AS alert_severity,
        d_a.phenological_stage AS phenological_stage
    FROM disease_alert d_a
    INNER JOIN disease d
        ON d.disease_id = d_a.disease_id
    LEFT JOIN block b
        ON b.block_id = d_a.block_id
    WHERE d_a.vineyard_id = :vineyardId
      AND d_a.alert_status = 'active'
    ORDER BY d_a.created_at DESC,
             d_a.alert_id DESC
    """
    )
    fun observeActiveAlertsForVineyard(
        vineyardId: Long
    ): Flow<List<DiseaseAlertSummaryProjection>>
}