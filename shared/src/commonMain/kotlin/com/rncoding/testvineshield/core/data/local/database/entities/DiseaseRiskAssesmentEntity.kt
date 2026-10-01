package com.rncoding.testvineshield.core.data.local.database.entities

data class DiseaseRiskAssessmentEntity(
    val riskAssessmentId: Long,
    val vineyardId: Long,
    val diseaseId: Long,
    val blockId: Long?,
    val modelType: DiseaseRiskModelType,
    val calculatedAt: Long,
    val riskScore: Int,
    val riskLevel: DiseaseRiskLevel,
    val modelVersion: String,
    val isInfectionEvent: Boolean,
    val details: String?
)