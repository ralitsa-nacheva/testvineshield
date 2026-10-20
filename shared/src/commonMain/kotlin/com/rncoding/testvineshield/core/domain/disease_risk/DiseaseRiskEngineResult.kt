
package com.rncoding.testvineshield.core.domain.disease_risk

sealed interface DiseaseRiskEngineResult {

    data class Success(
        val evaluation: DiseaseRiskModelEvaluation
    ) : DiseaseRiskEngineResult

    data class Skipped(
        val reason: SkipReason
    ) : DiseaseRiskEngineResult {

        enum class SkipReason {
            MONITORING_DISABLED,
            NO_NEW_WEATHER
        }
    }

    data class StateIncompatible(
        val storedVersion: String,
        val expectedVersion: String
    ) : DiseaseRiskEngineResult

    data class StateInvalid(
        val reason: String
    ) : DiseaseRiskEngineResult

    data class Failure(
        val reason: String
    ) : DiseaseRiskEngineResult
}
