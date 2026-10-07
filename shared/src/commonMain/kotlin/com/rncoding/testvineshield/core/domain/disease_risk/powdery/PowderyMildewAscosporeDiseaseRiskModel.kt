package com.rncoding.testvineshield.core.domain.disease_risk.powdery

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskConfidence
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskConfidenceEvaluator
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEvaluationStatus
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskEvent
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskFactor
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskInput
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskLevel
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModel
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelEvaluation
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelState
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskModelType
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskResult

class PowderyMildewAscosporeDiseaseRiskModel(
    private val calculator:
    PowderyMildewAscosporeCalculator =
        PowderyMildewAscosporeCalculator()
) : DiseaseRiskModel {

    override val modelType =
        DiseaseRiskModelType.POWDERY_MILDEW_ASCOSPORE

    override val modelVersion =
        MODEL_VERSION

    override fun evaluate(
        input: DiseaseRiskInput,
        previousState: DiseaseRiskModelState?,
        calculatedAt: Long
    ): DiseaseRiskModelEvaluation {

        /*
         * This model is stateless.
         *
         * Receiving state intended for another
         * model indicates an orchestration error.
         */
        if (previousState != null) {
            return stateError(
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
                PowderyMildewAscosporeInput(
                    weather =
                        input.hourlyWeather
                )
            )

        val infectionConditions =
            calculation.infectionConditionsMet

        val releaseConditions =
            calculation.releaseConditionsMet

        /*
         * null means that release conditions were
         * reached but the available wetness evidence
         * was insufficient to determine infection.
         */
        if (
            releaseConditions &&
            infectionConditions == null
        ) {
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
                                .INSUFFICIENT_DATA,

                        riskLevel =
                            DiseaseRiskLevel.NONE,

                        confidence =
                            DiseaseRiskConfidence.LOW,

                        event =
                            DiseaseRiskEvent
                                .ASCOSPORE_RELEASE_CONDITIONS,

                        nativeRiskValue = null,

                        factors =
                            buildFactors(
                                calculation
                            )
                    ),
                nextState = null
            )
        }

        val riskLevel =
            when {

                infectionConditions == true ->
                    DiseaseRiskLevel.HIGH

                releaseConditions ->
                    DiseaseRiskLevel.MODERATE

                else ->
                    DiseaseRiskLevel.NONE
            }

        val event =
            when {

                infectionConditions == true ->
                    DiseaseRiskEvent
                        .ASCOSPORE_INFECTION_CONDITIONS

                releaseConditions ->
                    DiseaseRiskEvent
                        .ASCOSPORE_RELEASE_CONDITIONS

                else ->
                    null
            }

        val confidence =
            when {

                /*
                 * Infection depends on continuous
                 * leaf-wetness evidence.
                 */
                infectionConditions == true ->
                    DiseaseRiskConfidenceEvaluator
                        .fromLeafWetness(
                            input.hourlyWeather
                        )

                /*
                 * Release is based on rainfall and
                 * temperature from the weather
                 * source. We currently do not carry
                 * provenance/confidence metadata
                 * for those fields.
                 */
                releaseConditions ->
                    DiseaseRiskConfidence.MODERATE

                else ->
                    DiseaseRiskConfidence.MODERATE
            }

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

                    event =
                        event,

                    /*
                     * This model does not expose
                     * a validated continuous
                     * numerical risk index.
                     */
                    nativeRiskValue = null,

                    factors =
                        buildFactors(
                            calculation
                        )
                ),
            nextState = null
        )
    }

    private fun buildFactors(
        result: PowderyMildewAscosporeResult
    ): List<DiseaseRiskFactor> {

        return listOf(
            DiseaseRiskFactor(
                name =
                    "event_precipitation",
                value =
                    result.precipitationMm,
                unit = "mm",
                satisfied =
                    result.releaseConditionsMet
            ),
            DiseaseRiskFactor(
                name =
                    "ascospore_release_conditions",
                value = null,
                unit = null,
                satisfied =
                    result.releaseConditionsMet
            ),
            DiseaseRiskFactor(
                name =
                    "longest_wet_period",
                value =
                    result
                        .longestWetPeriodHours
                        ?.toDouble(),
                unit = "hours",
                satisfied =
                    result
                        .longestWetPeriodHours
                        ?.let {
                            it >=
                                    PowderyMildewAscosporeCalculator
                                        .REQUIRED_INFECTION_WET_HOURS
                        }
            ),
            DiseaseRiskFactor(
                name =
                    "wet_period_mean_temperature",
                value =
                    result
                        .wetPeriodMeanTemperatureCelsius,
                unit = "°C",
                satisfied =
                    result
                        .wetPeriodMeanTemperatureCelsius
                        ?.let {
                            it >=
                                    PowderyMildewAscosporeCalculator
                                        .MIN_INFECTION_TEMPERATURE_C &&
                                    it <=
                                    PowderyMildewAscosporeCalculator
                                        .MAX_INFECTION_TEMPERATURE_C
                        }
            ),
            DiseaseRiskFactor(
                name =
                    "ascospore_infection_conditions",
                value = null,
                unit = null,
                satisfied =
                    result.infectionConditionsMet
            )
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

    private fun stateError(
        input: DiseaseRiskInput,
        calculatedAt: Long
    ): DiseaseRiskModelEvaluation {

        return emptyEvaluation(
            input = input,
            calculatedAt = calculatedAt,
            status =
                DiseaseRiskEvaluationStatus
                    .MODEL_STATE_ERROR
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
            "POWDERY_ASCOSPORE_EVENT_V1"
    }
}