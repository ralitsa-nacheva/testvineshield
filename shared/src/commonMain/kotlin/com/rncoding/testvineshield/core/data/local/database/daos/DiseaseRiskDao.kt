package com.rncoding.testvineshield.core.data.local.database.daos

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import androidx.room.Transaction
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseRiskEvaluationEntity
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseRiskModelStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiseaseRiskDao {

    @Upsert
    suspend fun upsertEvaluation(
        evaluation: DiseaseRiskEvaluationEntity
    )

    @Upsert
    suspend fun upsertModelState(
        state: DiseaseRiskModelStateEntity
    )

    @Query(
        """
        SELECT *
        FROM disease_risk_model_state
        WHERE vineyard_id = :vineyardId
          AND block_id = :blockId
          AND disease_id = :diseaseId
          AND model_type = :modelType
        LIMIT 1
        """
    )
    suspend fun getModelState(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long,
        modelType: String
    ): DiseaseRiskModelStateEntity?

    @Query(
        """
        DELETE FROM disease_risk_model_state
        WHERE vineyard_id = :vineyardId
          AND block_id = :blockId
          AND disease_id = :diseaseId
          AND model_type = :modelType
        """
    )
    suspend fun deleteModelState(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long,
        modelType: String
    )

    @Query(
        """
        SELECT *
        FROM disease_risk_evaluation
        WHERE vineyard_id = :vineyardId
          AND block_id = :blockId
          AND disease_id = :diseaseId
        ORDER BY calculated_at DESC, evaluation_id DESC
        """
    )
    fun observeEvaluations(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long
    ): Flow<List<DiseaseRiskEvaluationEntity>>

    @Query(
        """
        SELECT *
        FROM disease_risk_evaluation
        WHERE vineyard_id = :vineyardId
          AND block_id = :blockId
          AND disease_id = :diseaseId
          AND model_type = :modelType
        ORDER BY calculated_at DESC, evaluation_id DESC
        LIMIT 1
        """
    )
    suspend fun getLatestEvaluation(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long,
        modelType: String
    ): DiseaseRiskEvaluationEntity?

    @Query(
        """
        DELETE FROM disease_risk_evaluation
        WHERE vineyard_id = :vineyardId
        """
    )
    suspend fun deleteEvaluationsForVineyard(
        vineyardId: Long
    )

    @Transaction
    suspend fun saveEvaluationAndState(
        evaluation: DiseaseRiskEvaluationEntity,
        state: DiseaseRiskModelStateEntity?
    ) {
        upsertEvaluation(evaluation)

        if (state != null) {
            upsertModelState(state)
        }
    }
}