
package com.rncoding.testvineshield.core.domain.disease_risk

interface VineyardDiseaseRiskEvaluator {

    suspend fun evaluateVineyard(
        vineyardId: Long,
        calculatedAt: Long
    ): VineyardDiseaseRiskEvaluationSummary
}
