
package com.rncoding.testvineshield.core.data.disease_risk

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEngine
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEngineResult
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskMonitoringConfigurationRepository
import com.rncoding.testvineshield.core.domain.disease_risk.VineyardDiseaseRiskEvaluationItem
import com.rncoding.testvineshield.core.domain.disease_risk.VineyardDiseaseRiskEvaluationSummary
import com.rncoding.testvineshield.core.domain.disease_risk.VineyardDiseaseRiskEvaluator
import kotlinx.coroutines.CancellationException

class VineyardDiseaseRiskEvaluatorImpl(
    private val monitoringRepository:
    DiseaseRiskMonitoringConfigurationRepository,
    private val diseaseRiskEngine: DiseaseRiskEngine
) : VineyardDiseaseRiskEvaluator {

    override suspend fun evaluateVineyard(
        vineyardId: Long,
        calculatedAt: Long
    ): VineyardDiseaseRiskEvaluationSummary {

        require(vineyardId > 0L) {
            "vineyardId must be positive."
        }

        require(calculatedAt > 0L) {
            "calculatedAt must be positive."
        }

        // Repository failures propagate to the caller.
        // An unavailable configuration list is not an empty list.
        val configurations =
            monitoringRepository.getEnabledConfigurationsForVineyard(
                vineyardId
            )

        val items = mutableListOf<VineyardDiseaseRiskEvaluationItem>()

        for (configuration in configurations) {

            val result = try {
                diseaseRiskEngine.evaluate(
                    modelType = configuration.modelType,
                    diseaseId = configuration.diseaseId,
                    vineyardId = configuration.vineyardId,
                    blockId = configuration.blockId,
                    calculatedAt = calculatedAt
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                DiseaseRiskEngineResult.Failure(
                    reason = exception.message
                        ?: "Unexpected disease-risk evaluation failure."
                )
            }

            items += VineyardDiseaseRiskEvaluationItem(
                configuration = configuration,
                result = result
            )
        }

        return VineyardDiseaseRiskEvaluationSummary(
            vineyardId = vineyardId,
            calculatedAt = calculatedAt,
            items = items
        )
    }
}
