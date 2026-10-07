package com.rncoding.testvineshield.core.domain.disease_risk.botrytis

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskConfidence
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskConfidenceEvaluator
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEvaluationStatus
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskFactor
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskInput
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskLevel
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModel
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelEvaluation
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelState
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelType
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskResult

class BotrytisDiseaseRiskModel(
    private val calculator: BotrytisRiskCalculator =
        BotrytisRiskCalculator()
) : DiseaseRiskModel {

    override val modelType =
        DiseaseRiskModelType.BOTRYTIS

    override val modelVersion =
        MODEL_VERSION

    override fun evaluate(
        input: DiseaseRiskInput,
        previousState: DiseaseRiskModelState?,
        calculatedAt: Long
    ): DiseaseRiskModelEvaluation {

        val hostTissue =
            BotrytisHostTissueMapper.map(
                input.hostContext
                    .phenologicalStage
            )

        if (
            hostTissue ==
            BotrytisHostTissue.UNSUPPORTED
        ) {
            return notApplicable(
                input = input,
                calculatedAt = calculatedAt
            )
        }

        if (input.hourlyWeather.isEmpty()) {
            return insufficientData(
                input = input,
                calculatedAt = calculatedAt
            )
        }

        val calculation =
            calculator.evaluate(
                BotrytisRiskInput(
                    weather =
                        input.hourlyWeather,
                    hostTissue =
                        hostTissue
                )
            )

        if (!calculation.evaluationAvailable) {
            return insufficientData(
                input = input,
                calculatedAt = calculatedAt
            )
        }

        val confidence =
            DiseaseRiskConfidenceEvaluator
                .fromLeafWetness(
                    input.hourlyWeather
                )

        val riskLevel =
            mapRiskLevel(
                calculation.riskLevel
            )

        return DiseaseRiskModelEvaluation(
            result =
                DiseaseRiskResult(
                    diseaseId =
                        input.diseaseId,
                    vineyardId =
                        input.vineyardId,
                    blockId =
                        input.blockId,
                    calculatedAt =
                        calculatedAt,

                    modelType =
                        modelType,
                    modelVersion =
                        modelVersion,

                    evaluationStatus =
                        DiseaseRiskEvaluationStatus
                            .CALCULATED,

                    riskLevel =
                        riskLevel,

                    confidence =
                        confidence,

                    event = null,

                    nativeRiskValue =
                        calculation
                            .predictedInfectionPercent,

                    factors =
                        buildFactors(
                            calculation
                        )
                ),
            nextState = null
        )
    }

    private fun buildFactors(
        result: BotrytisRiskResult
    ): List<DiseaseRiskFactor> {

        return listOf(
            DiseaseRiskFactor(
                name = "wet_period_hours",
                value =
                    result
                        .longestWetPeriodHours
                        ?.toDouble(),
                unit = "hours",
                satisfied =
                    result.longestWetPeriodHours
                        ?.let {
                            it > 0
                        }
            ),
            DiseaseRiskFactor(
                name =
                    "wet_period_mean_temperature",
                value =
                    result
                        .wetPeriodMeanTemperatureCelsius,
                unit = "°C",
                satisfied = null
            ),
            DiseaseRiskFactor(
                name =
                    "predicted_infection",
                value =
                    result
                        .predictedInfectionPercent,
                unit = "%",
                satisfied =
                    result
                        .predictedInfectionPercent
                        ?.let {
                            it > 0.0
                        }
            )
        )
    }

    private fun mapRiskLevel(
        riskLevel:
        BotrytisRiskResult.RiskLevel
    ): DiseaseRiskLevel {

        return when (riskLevel) {

            BotrytisRiskResult.RiskLevel.NONE ->
                DiseaseRiskLevel.NONE

            BotrytisRiskResult.RiskLevel.LOW ->
                DiseaseRiskLevel.LOW

            BotrytisRiskResult.RiskLevel.MODERATE ->
                DiseaseRiskLevel.MODERATE

            BotrytisRiskResult.RiskLevel.HIGH ->
                DiseaseRiskLevel.HIGH
        }
    }

    private fun notApplicable(
        input: DiseaseRiskInput,
        calculatedAt: Long
    ): DiseaseRiskModelEvaluation {

        return emptyEvaluation(
            input = input,
            calculatedAt = calculatedAt,
            status =
                DiseaseRiskEvaluationStatus
                    .NOT_APPLICABLE
        )
    }

    private fun insufficientData(
        input: DiseaseRiskInput,
        calculatedAt: Long
    ): DiseaseRiskModelEvaluation {

        return emptyEvaluation(
            input = input,
            calculatedAt = calculatedAt,
            status =
                DiseaseRiskEvaluationStatus
                    .INSUFFICIENT_DATA
        )
    }

    private fun emptyEvaluation(
        input: DiseaseRiskInput,
        calculatedAt: Long,
        status: DiseaseRiskEvaluationStatus
    ): DiseaseRiskModelEvaluation {

        return DiseaseRiskModelEvaluation(
            result =
                DiseaseRiskResult(
                    diseaseId =
                        input.diseaseId,
                    vineyardId =
                        input.vineyardId,
                    blockId =
                        input.blockId,
                    calculatedAt =
                        calculatedAt,

                    modelType =
                        modelType,
                    modelVersion =
                        modelVersion,

                    evaluationStatus =
                        status,

                    riskLevel =
                        DiseaseRiskLevel.NONE,

                    confidence =
                        DiseaseRiskConfidence.LOW,

                    event = null,
                    nativeRiskValue = null,
                    factors = emptyList()
                ),
            nextState = null
        )
    }

    companion object {
        const val MODEL_VERSION =
            "NAIR_ALLEN_FLOWER_BERRY_V1"
    }
}