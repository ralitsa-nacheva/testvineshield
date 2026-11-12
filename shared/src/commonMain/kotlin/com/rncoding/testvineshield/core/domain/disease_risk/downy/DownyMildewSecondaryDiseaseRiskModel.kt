package com.rncoding.testvineshield.core.domain.disease_risk.downy

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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class DownyMildewSecondaryDiseaseRiskModel(
    private val calculator:
    DownyMildewSecondaryInfectionCalculator =
        DownyMildewSecondaryInfectionCalculator()
) : DiseaseRiskModel {

    override val modelType =
        DiseaseRiskModelType.DOWNY_MILDEW_SECONDARY

    override val modelVersion =
        MODEL_VERSION

    override fun evaluate(
        input: DiseaseRiskInput,
        previousState: DiseaseRiskModelState?,
        calculatedAt: Long
    ): DiseaseRiskModelEvaluation {

        /*
         * The secondary model is stateless.
         */
        if (previousState != null) {
            return emptyEvaluation(
                input = input,
                calculatedAt = calculatedAt,
                status =
                    DiseaseRiskEvaluationStatus
                        .MODEL_STATE_ERROR
            )
        }

        val oilSpots = input.hostContext.observedSymptoms.filter {
            it.code == DOWNY_MILDEW_OIL_SPOT_CODE
        }
        if (oilSpots.isEmpty()) {
            return emptyEvaluation(input, calculatedAt, DiseaseRiskEvaluationStatus.NOT_APPLICABLE)
        }

        val firstHour = input.hourlyWeather.minOfOrNull { it.timestamp }
            ?: return emptyEvaluation(input, calculatedAt, DiseaseRiskEvaluationStatus.INSUFFICIENT_DATA)
        val zone = try {
            TimeZone.of(input.timeZone)
        } catch (_: IllegalArgumentException) {
            return emptyEvaluation(input, calculatedAt, DiseaseRiskEvaluationStatus.INSUFFICIENT_DATA)
        }
        val firstLocalDate = Instant.fromEpochMilliseconds(firstHour).toLocalDateTime(zone).date
        // A date-only observation on the first day cannot establish that it preceded the window.
        val observedBeforeWindow = oilSpots.any { it.observedAt?.let { date -> date < firstLocalDate } == true }
        if (!observedBeforeWindow) {
            return emptyEvaluation(input, calculatedAt, DiseaseRiskEvaluationStatus.INSUFFICIENT_DATA)
        }

        val calculation =
            calculator.evaluate(
                DownyMildewSecondaryInput(
                    weather =
                        input.hourlyWeather,
                    activeOilSpotsObserved =
                        true
                )
            )

        val status =
            determineStatus(
                calculation
            )

        val riskLevel =
            determineRiskLevel(
                calculation
            )

        val event =
            determineEvent(
                calculation
            )

        val confidence =
            determineConfidence(
                input = input,
                calculation = calculation
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
                        status,

                    riskLevel =
                        riskLevel,

                    confidence =
                        confidence,

                    event =
                        event,

                    nativeRiskValue = null,

                    factors =
                        buildFactors(
                            calculation
                        )
                ),
            nextState = null
        )
    }

    private fun determineStatus(
        result:
        DownyMildewSecondaryInfectionCalculator.Result
    ): DiseaseRiskEvaluationStatus {

        if (result.infectionConditionsMet == null) {
            return DiseaseRiskEvaluationStatus
                .INSUFFICIENT_DATA
        }

        return DiseaseRiskEvaluationStatus
            .CALCULATED
    }

    private fun determineRiskLevel(
        result:
        DownyMildewSecondaryInfectionCalculator.Result
    ): DiseaseRiskLevel {

        return when {

            result.infectionConditionsMet == true ->
                DiseaseRiskLevel.HIGH

            result.sporulationConditionsMet ->
                DiseaseRiskLevel.MODERATE

            else ->
                DiseaseRiskLevel.NONE
        }
    }

    private fun determineEvent(
        result:
        DownyMildewSecondaryInfectionCalculator.Result
    ): DiseaseRiskEvent? {

        return when {

            result.infectionConditionsMet == true ->
                DiseaseRiskEvent
                    .SECONDARY_INFECTION_CONDITIONS

            result.sporulationConditionsMet ->
                DiseaseRiskEvent
                    .SECONDARY_SPORULATION_CONDITIONS

            else ->
                null
        }
    }

    private fun determineConfidence(
        input: DiseaseRiskInput,
        calculation:
        DownyMildewSecondaryInfectionCalculator.Result
    ): DiseaseRiskConfidence {

        if (calculation.infectionConditionsMet == null) {
            return DiseaseRiskConfidence.LOW
        }

        if (
            calculation.infectionConditionsMet == true
        ) {
            return DiseaseRiskConfidenceEvaluator
                .fromLeafWetness(
                    input.hourlyWeather
                )
        }

        /*
         * Sporulation uses modeled hourly
         * temperature, RH and day/night evidence.
         *
         * Those fields currently do not carry
         * source-confidence metadata.
         */
        return DiseaseRiskConfidence.MODERATE
    }

    private fun buildFactors(
        result:
        DownyMildewSecondaryInfectionCalculator.Result
    ): List<DiseaseRiskFactor> {

        return listOf(
            DiseaseRiskFactor(
                name =
                    "observed_oil_spot",
                value = null,
                unit = null,
                satisfied =
                    result.activeOilSpotsObserved
            ),
            DiseaseRiskFactor(
                name =
                    "longest_sporulation_period",
                value =
                    result
                        .longestSporulationPeriodHours
                        .toDouble(),
                unit = "hours",
                satisfied =
                    result.sporulationConditionsMet
            ),
            DiseaseRiskFactor(
                name =
                    "sporulation_conditions",
                value = null,
                unit = null,
                satisfied =
                    result.sporulationConditionsMet
            ),
            DiseaseRiskFactor(
                name =
                    "maximum_post_sporulation_wet_degree_hours",
                value =
                    result.maximumWetDegreeHours,
                unit = "°C·h",
                satisfied =
                    result.wetnessThresholdMet
            ),
            DiseaseRiskFactor(
                name =
                    "secondary_infection_conditions",
                value = null,
                unit = null,
                satisfied =
                    result.infectionConditionsMet
            )
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
            "DOWNY_SECONDARY_GROWCARE_V3"

        const val DOWNY_MILDEW_OIL_SPOT_CODE =
            "downy_mildew_oil_spot"
    }
}