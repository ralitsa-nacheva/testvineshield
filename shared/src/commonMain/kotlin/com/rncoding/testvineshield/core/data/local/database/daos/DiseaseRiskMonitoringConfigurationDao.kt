package com.rncoding.testvineshield.core.data.local.database.daos

import kotlinx.coroutines.flow.Flow
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rncoding.testvineshield.core.domain.disease_risk.MonitoringDraftSaveOutcome
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseRiskMonitoringConfigurationEntity

@Dao
interface DiseaseRiskMonitoringConfigurationDao {
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM block b JOIN vineyard v ON v.vineyard_id = b.vineyard_id
            WHERE b.block_id = :blockId AND v.vineyard_id = :vineyardId AND v.user_id = :ownerId
        )
    """)
    suspend fun ownsBlock(ownerId: Long, vineyardId: Long, blockId: Long): Boolean

    @Query("SELECT disease_id FROM disease WHERE code = :code LIMIT 1")
    suspend fun catalogueDiseaseId(code: String): Long?

    @Transaction
    suspend fun saveDisabledDraft(
        ownerId: Long,
        vineyardId: Long,
        blockId: Long,
        diseaseCode: String,
        modelType: String,
        startDate: String
    ): MonitoringDraftSaveOutcome {
        if (!ownsBlock(ownerId, vineyardId, blockId)) return MonitoringDraftSaveOutcome.BLOCK_UNAVAILABLE
        val diseaseId = catalogueDiseaseId(diseaseCode) ?: return MonitoringDraftSaveOutcome.DISEASE_UNAVAILABLE
        val existing = getConfiguration(vineyardId, blockId, diseaseId, modelType)
        if (existing?.enabled == true) return MonitoringDraftSaveOutcome.ALREADY_ENABLED
        if (existing?.monitoringStartDate == startDate) return MonitoringDraftSaveOutcome.SAVED

        saveConfigurationAndResetState(
            DiseaseRiskMonitoringConfigurationEntity(vineyardId, blockId, diseaseId, modelType, startDate, false)
        )
        return MonitoringDraftSaveOutcome.SAVED
    }

    @Query("SELECT * FROM disease_risk_monitoring_configuration WHERE vineyard_id = :vineyardId ORDER BY block_id, disease_id, model_type")
    fun observeConfigurationsForVineyard(vineyardId: Long): Flow<List<DiseaseRiskMonitoringConfigurationEntity>>


    @Query(
        """
        SELECT *
        FROM disease_risk_monitoring_configuration
        WHERE vineyard_id = :vineyardId
          AND block_id = :blockId
          AND disease_id = :diseaseId
          AND model_type = :modelType
        LIMIT 1
        """
    )
    suspend fun getConfiguration(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long,
        modelType: String
    ): DiseaseRiskMonitoringConfigurationEntity?

    @Upsert
    suspend fun upsertConfiguration(
        configuration: DiseaseRiskMonitoringConfigurationEntity
    )

    @Query(
        """
        DELETE FROM disease_risk_monitoring_configuration
        WHERE vineyard_id = :vineyardId
          AND block_id = :blockId
          AND disease_id = :diseaseId
          AND model_type = :modelType
        """
    )
    suspend fun deleteConfiguration(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long,
        modelType: String
    ): Int

    @Query(
        """
    DELETE FROM disease_risk_model_state
    WHERE vineyard_id = :vineyardId
      AND block_id = :blockId
      AND disease_id = :diseaseId
      AND model_type = :modelType
    """
    )
    suspend fun deleteAssociatedModelState(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long,
        modelType: String
    )

    @Transaction
    suspend fun saveConfigurationAndResetState(
        configuration: DiseaseRiskMonitoringConfigurationEntity
    ) {
        upsertConfiguration(configuration)

        deleteAssociatedModelState(
            vineyardId = configuration.vineyardId,
            blockId = configuration.blockId,
            diseaseId = configuration.diseaseId,
            modelType = configuration.modelType
        )
    }

    @Transaction
    suspend fun deleteConfigurationAndState(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long,
        modelType: String
    ) {
        deleteAssociatedModelState(
            vineyardId = vineyardId,
            blockId = blockId,
            diseaseId = diseaseId,
            modelType = modelType
        )

        deleteConfiguration(
            vineyardId = vineyardId,
            blockId = blockId,
            diseaseId = diseaseId,
            modelType = modelType
        )
    }


    @Query(
        """
    SELECT *
    FROM disease_risk_monitoring_configuration
    WHERE vineyard_id = :vineyardId
      AND enabled = 1
    ORDER BY block_id, disease_id, model_type
    """
    )
    suspend fun getEnabledConfigurationsForVineyard(
        vineyardId: Long
    ): List<DiseaseRiskMonitoringConfigurationEntity>

}
