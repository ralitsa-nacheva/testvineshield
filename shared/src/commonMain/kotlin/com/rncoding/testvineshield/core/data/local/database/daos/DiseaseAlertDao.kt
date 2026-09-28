package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseAlertEntity


@Dao
interface DiseaseAlertDao {
    @Upsert
    @Transaction
    suspend fun upsertDiseaseAlert(alert: DiseaseAlertEntity)

    @Query("Select * From disease_alert")
    fun getAllAlerts(): Flow<DiseaseAlertEntity>

    @Query("Select * From disease_alert Where vineyard_id = :vineyardId")
    suspend fun getAllAlertsForVineyard(vineyardId: Long): List<DiseaseAlertEntity>

    @Query("Delete From disease_alert")
    @Transaction
    suspend fun clearAllAlerts()

    @Query("Delete From disease_alert Where vineyard_id = :vineyardId")
    suspend fun deleteAllAlertsForVineyard(vineyardId: Long)

    @Query("Delete From disease_alert Where alert_id = :alertId")
    suspend fun deleteAlertById(alertId: Long)

    // use this for vineyard summary
    @Query("Select Count(alert_id) From disease_alert Where vineyard_id = :vineyardId And alert_status = 'Open'")
    suspend fun getOpenAlertsCountByVineyard(vineyardId: Long): Int

    // use this for vineyard summary aggregate
    @Query("Select alert From disease_alert Where vineyard_id = :vineyardId And alert_status = 'Open'" +
            "And created_at = (Select Max(created_at) From disease_alert Where vineyard_id = :vineyardId)")
    suspend fun getLastOpenAlertByVineyard(vineyardId: Long): String
}