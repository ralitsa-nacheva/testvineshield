package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.data.local.database.daos.DiseaseRiskDao
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModel
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskPersistence
import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseRiskModelStateEntity
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEvaluationStatus
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelEvaluation

class DefaultDiseaseRiskPersistence(
    private val diseaseRiskDao: DiseaseRiskDao,
    private val stateCodec: DiseaseRiskStateCodec
) : DiseaseRiskPersistence  {

    override suspend fun loadState(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long,
        model: DiseaseRiskModel
    ): DiseaseRiskPersistence.StateLoadResult {

        val stored = diseaseRiskDao.getModelState(
            vineyardId = vineyardId,
            blockId = blockId,
            diseaseId = diseaseId,
            modelType = model.modelType.name
        ) ?: return DiseaseRiskPersistence.StateLoadResult.NotFound

        if (stored.modelVersion != model.modelVersion) {
            return DiseaseRiskPersistence.StateLoadResult
                .IncompatibleVersion(
                    storedVersion = stored.modelVersion,
                    expectedVersion = model.modelVersion
                )
        }

        return when (
            val decoded = stateCodec.decode(
                modelType = model.modelType,
                stateType = stored.stateType,
                statePayload = stored.statePayload
            )
        ) {
            is DiseaseRiskStateCodec.DecodeResult.Success ->
                DiseaseRiskPersistence.StateLoadResult.Loaded(
                    state = decoded.state
                )

            is DiseaseRiskStateCodec.DecodeResult.Failure ->
                DiseaseRiskPersistence.StateLoadResult.InvalidState(
                    reason = decoded.reason
                )
        }
    }

    override suspend fun saveEvaluation(
        evaluation: DiseaseRiskModelEvaluation
    ) {
        val result = evaluation.result

        require(
            result.evaluationStatus !=
                    DiseaseRiskEvaluationStatus.MODEL_STATE_ERROR
        ) {
            "Cannot persist an evaluation with MODEL_STATE_ERROR"
        }

        val evaluationEntity =
            DiseaseRiskEvaluationMapper.toEntity(result)

        val stateEntity =
            evaluation.nextState?.let { state ->

                val encoded = stateCodec.encode(
                    modelType = result.modelType,
                    state = state
                )

                DiseaseRiskModelStateEntity(
                    vineyardId = result.vineyardId,
                    blockId = result.blockId,
                    diseaseId = result.diseaseId,
                    modelType = result.modelType.name,
                    modelVersion = result.modelVersion,
                    stateType = encoded.stateType,
                    statePayload = encoded.statePayload,
                    updatedAt = result.calculatedAt
                )
            }

        diseaseRiskDao.saveEvaluationAndState(
            evaluation = evaluationEntity,
            state = stateEntity
        )
    }
}