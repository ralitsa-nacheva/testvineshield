
package com.rncoding.testvineshield.core.domain.disease_risk

data class VineyardDiseaseRiskEvaluationItem(
    val configuration: DiseaseRiskMonitoringConfiguration,
    val result: DiseaseRiskEngineResult
)

data class VineyardDiseaseRiskEvaluationSummary(
    val vineyardId: Long,
    val calculatedAt: Long,
    val items: List<VineyardDiseaseRiskEvaluationItem>
) {
    val total: Int
        get() = items.size

    val succeeded: Int
        get() = items.count {
            it.result is DiseaseRiskEngineResult.Success
        }

    val skipped: Int
        get() = items.count {
            it.result is DiseaseRiskEngineResult.Skipped
        }

    val failed: Int
        get() = items.count {
            when (it.result) {
                is DiseaseRiskEngineResult.Failure,
                is DiseaseRiskEngineResult.StateInvalid,
                is DiseaseRiskEngineResult.StateIncompatible -> true

                else -> false
            }
        }

    val hasFailures: Boolean
        get() = failed > 0
}
