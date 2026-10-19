package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseRiskMonitoringConfigurationEntity

@Dao
interface DiseaseRiskMonitoringConfigurationDao {

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
}