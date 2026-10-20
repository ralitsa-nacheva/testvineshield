
package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.domain.disease_risk.*
import kotlinx.coroutines.CancellationException

class DiseaseRiskEngineImpl(
    private val modelRegistry: DiseaseRiskModelRegistry,
    private val inputFactory: DiseaseRiskInputFactory,
    private val persistence: DiseaseRiskPersistence,
    private val coordinator: DiseaseRiskEvaluationCoordinator
) : DiseaseRiskEngine {

    override suspend fun evaluate(
        modelType: DiseaseRiskModelType,
        diseaseId: Long,
        vineyardId: Long,
        blockId: Long,
        calculatedAt: Long
    ): DiseaseRiskEngineResult {
        return coordinator.withExclusiveEvaluation {
            evaluateExclusively(
                modelType = modelType,
                diseaseId = diseaseId,
                vineyardId = vineyardId,
                blockId = blockId,
                calculatedAt = calculatedAt
            )
        }
    }

    private suspend fun evaluateExclusively(
        modelType: DiseaseRiskModelType,
        diseaseId: Long,
        vineyardId: Long,
        blockId: Long,
        calculatedAt: Long
    ): DiseaseRiskEngineResult {
        try {
            // 1. Resolve the registered disease-risk model.
            val model = modelRegistry.getModel(modelType)
                ?: return DiseaseRiskEngineResult.Failure(
                    reason = "No disease-risk model registered for $modelType."
                )

            // 2. Restore the model's previously persisted state.
            val previousState = when (
                val loaded = persistence.loadState(
                    vineyardId = vineyardId,
                    blockId = blockId,
                    diseaseId = diseaseId,
                    model = model
                )
            ) {
                DiseaseRiskPersistence.StateLoadResult.NotFound -> null

                is DiseaseRiskPersistence.StateLoadResult.Loaded ->
                    loaded.state

                is DiseaseRiskPersistence.StateLoadResult.IncompatibleVersion ->
                    return DiseaseRiskEngineResult.StateIncompatible(
                        storedVersion = loaded.storedVersion,
                        expectedVersion = loaded.expectedVersion
                    )

                is DiseaseRiskPersistence.StateLoadResult.InvalidState ->
                    return DiseaseRiskEngineResult.StateInvalid(
                        reason = loaded.reason
                    )
            }

            // 3. Build and validate the input.
            val input = when (
                val inputResult = inputFactory.createInput(
                    modelType = modelType,
                    diseaseId = diseaseId,
                    vineyardId = vineyardId,
                    blockId = blockId,
                    calculatedAt = calculatedAt,
                    previousState = previousState
                )
            ) {
                is DiseaseRiskInputResult.Success ->
                    inputResult.input

                is DiseaseRiskInputResult.MissingVineyard ->
                    return DiseaseRiskEngineResult.Failure(
                        reason = "Vineyard ${inputResult.vineyardId} not found."
                    )

                is DiseaseRiskInputResult.InvalidTimeZone ->
                    return DiseaseRiskEngineResult.Failure(
                        reason = "Invalid vineyard time zone: ${inputResult.timeZone}."
                    )

                is DiseaseRiskInputResult.InsufficientWeather ->
                    return DiseaseRiskEngineResult.Failure(
                        reason = inputResult.reason
                    )

                is DiseaseRiskInputResult.Failure ->
                    return DiseaseRiskEngineResult.Failure(
                        reason = inputResult.reason
                    )

                is DiseaseRiskInputResult.MissingMonitoringConfiguration ->
                    return DiseaseRiskEngineResult.Failure(
                        reason = "Monitoring configuration is missing for $modelType."
                    )

                is DiseaseRiskInputResult.MonitoringDisabled ->
                    return DiseaseRiskEngineResult.Skipped(
                        reason = DiseaseRiskEngineResult.Skipped
                            .SkipReason.MONITORING_DISABLED
                    )

                DiseaseRiskInputResult.NoNewWeather ->
                    return DiseaseRiskEngineResult.Skipped(
                        reason = DiseaseRiskEngineResult.Skipped
                            .SkipReason.NO_NEW_WEATHER
                    )
            }

            // 4. Execute the selected scientific model.
            val evaluation = model.evaluate(
                input = input,
                previousState = previousState,
                calculatedAt = calculatedAt
            )

            // 5. Persist the evaluation and next state together.
            persistence.saveEvaluation(evaluation)

            return DiseaseRiskEngineResult.Success(
                evaluation = evaluation
            )

        } catch (exception: CancellationException) {
            // Never swallow coroutine cancellation.
            throw exception
        } catch (exception: Exception) {
            return DiseaseRiskEngineResult.Failure(
                reason = exception.message
                    ?: "Disease-risk evaluation failed."
            )
        }
    }
}
