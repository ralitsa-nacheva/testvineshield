package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.data.local.database.entities.DiseaseRiskEvaluationEntity
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskResult

object DiseaseRiskEvaluationMapper {

    fun toEntity(
        result: DiseaseRiskResult
    ): DiseaseRiskEvaluationEntity {

        return DiseaseRiskEvaluationEntity(
            evaluationId = 0L,
            diseaseId = result.diseaseId,
            vineyardId = result.vineyardId,
            blockId = result.blockId,
            calculatedAt = result.calculatedAt,
            modelType = result.modelType.name,
            modelVersion = result.modelVersion,
            evaluationStatus = result.evaluationStatus.name,
            riskLevel = result.riskLevel.name,
            confidence = result.confidence.name,
            event = result.event?.name,
            nativeRiskValue = result.nativeRiskValue
        )
    }
}