package com.rncoding.testvineshield.core.domain.disease_risk

data class DiseaseRiskResult(
    val diseaseId: Long,
    val vineyardId: Long,
    val blockId: Long,
    val calculatedAt: Long,

    val modelType: DiseaseRiskModelType,
    val modelVersion: String,

    val evaluationStatus: DiseaseRiskEvaluationStatus,
    val riskLevel: DiseaseRiskLevel,
    val confidence: DiseaseRiskConfidence,

    val event: DiseaseRiskEvent?,

    val nativeRiskValue: Double?,

    val factors: List<DiseaseRiskFactor>
)