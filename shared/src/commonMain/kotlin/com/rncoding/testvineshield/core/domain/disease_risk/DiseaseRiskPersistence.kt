package com.rncoding.testvineshield.core.domain.disease_risk

interface DiseaseRiskPersistence {

    sealed interface StateLoadResult {

        data object NotFound : StateLoadResult

        data class Loaded(
            val state: DiseaseRiskModelState
        ) : StateLoadResult

        data class IncompatibleVersion(
            val storedVersion: String,
            val expectedVersion: String
        ) : StateLoadResult

        data class InvalidState(
            val reason: String
        ) : StateLoadResult
    }

    suspend fun loadState(
        vineyardId: Long,
        blockId: Long,
        diseaseId: Long,
        model: DiseaseRiskModel
    ): StateLoadResult

    suspend fun saveEvaluation(
        evaluation: DiseaseRiskModelEvaluation
    )
}