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
import com.rncoding.testvineshield.core.domain.disease_risk.IncrementalWeatherSelector

class DownyMildewPrimaryDiseaseRiskModel(
    private val calculator:
    DownyMildewPrimaryInfectionCalculator =
        DownyMildewPrimaryInfectionCalculator()
) : DiseaseRiskModel {

    override val modelType =
        DiseaseRiskModelType.DOWNY_MILDEW_PRIMARY

    override val modelVersion =
        MODEL_VERSION

    override fun evaluate(
        input: DiseaseRiskInput,
        previousState: DiseaseRiskModelState?,
        calculatedAt: Long
    ): DiseaseRiskModelEvaluation {

        if (input.hourlyWeather.isEmpty()) {
            return insufficientData(
                input = input,
                calculatedAt = calculatedAt,
                previousState = previousState
            )
        }

        val state =
            when (previousState) {

                null ->
                    DownyMildewPrimaryState()

                is DownyMildewPrimaryState ->
                    previousState

                else ->
                    return stateError(
                        input = input,
                        calculatedAt = calculatedAt
                    )
            }

        val selection = IncrementalWeatherSelector.select(
            weather = input.hourlyWeather,
            lastProcessedTimestamp =
                state.lastProcessedWeatherTimestamp
        )

        val newWeather = when (selection) {

            is IncrementalWeatherSelector.Selection.Ready ->
                selection.weather

            IncrementalWeatherSelector.Selection.NoNewWeather ->
                return insufficientData(
                    input = input,
                    calculatedAt = calculatedAt,
                    previousState = state
                )

            IncrementalWeatherSelector.Selection.InvalidTimeline ->
                return insufficientData(
                    input = input,
                    calculatedAt = calculatedAt,
                    previousState = state
                )
        }

        /*
         * IMPORTANT:
         *
         * Our current weather evidence does not yet
         * provide a defensible threshold mapping from
         * Open-Meteo volumetric soil moisture to the
         * biological WET/DRY condition required by
         * this model.
         *
         * Therefore we explicitly use UNKNOWN rather
         * than inventing such a threshold.
         */
        val soilWetnessEvidence =
            SoilWetnessEvidence.UNKNOWN

        val evaluation =
            calculator.evaluate(
                DownyMildewPrimaryInput(
                    weather = newWeather,
                    soilWetnessEvidence =
                        soilWetnessEvidence,
                    previousState =
                        state
                )
            )

        val result =
            evaluation.result

        val nextState =
            if (soilWetnessEvidence == SoilWetnessEvidence.UNKNOWN) {
                state
            } else {
                evaluation.state.copy(
                    lastProcessedWeatherTimestamp =
                        newWeather.last().timestamp
                )
            }

        val infectionConditions =
            result.infectionConditionsMet

        val riskLevel =
            when {

                infectionConditions == true ->
                    DiseaseRiskLevel.HIGH

                result.germinationCompleted ->
                    DiseaseRiskLevel.MODERATE

                result.rainfallThresholdMet == true &&
                        result.germinationTemperatureMet ->
                    DiseaseRiskLevel.LOW

                else ->
                    DiseaseRiskLevel.NONE
            }

        val event =
            if (infectionConditions == true) {
                DiseaseRiskEvent
                    .PRIMARY_INFECTION_CONDITIONS
            } else {
                null
            }

        val status =
            determineStatus(
                result = result
            )

        val confidence =
            determineConfidence(
                input = input,
                result = result
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
                            result
                        )
                ),
            nextState =
                nextState
        )
    }

    private fun determineStatus(
        result:
        DownyMildewPrimaryInfectionCalculator.Result
    ): DiseaseRiskEvaluationStatus {

        /*
         * rainfallThresholdMet == null means the
         * calculator could not select the required
         * rainfall threshold because initial soil
         * wetness was unknown.
         */
        if (result.rainfallThresholdMet == null) {
            return DiseaseRiskEvaluationStatus
                .INSUFFICIENT_DATA
        }

        /*
         * Once germination has completed and rain
         * occurs afterwards, infection evaluation
         * depends on leaf-wetness evidence.
         */
        if (
            result.germinationCompleted &&
            result.subsequentRainDetected &&
            result.infectionConditionsMet == null
        ) {
            return DiseaseRiskEvaluationStatus
                .INSUFFICIENT_DATA
        }

        return DiseaseRiskEvaluationStatus
            .CALCULATED
    }

    private fun determineConfidence(
        input: DiseaseRiskInput,
        result:
        DownyMildewPrimaryInfectionCalculator.Result
    ): DiseaseRiskConfidence {

        if (
            result.rainfallThresholdMet == null
        ) {
            return DiseaseRiskConfidence.LOW
        }

        if (
            result.infectionConditionsMet == true
        ) {
            return DiseaseRiskConfidenceEvaluator
                .fromLeafWetness(
                    input.hourlyWeather
                )
        }

        return DiseaseRiskConfidence.MODERATE
    }

    private fun buildFactors(
        result:
        DownyMildewPrimaryInfectionCalculator.Result
    ): List<DiseaseRiskFactor> {

        return listOf(
            DiseaseRiskFactor(
                name = "rainfall",
                value =
                    result.rainfallMm,
                unit = "mm",
                satisfied =
                    result.rainfallThresholdMet
            ),
            DiseaseRiskFactor(
                name =
                    "required_rainfall_threshold",
                value =
                    result.rainfallThresholdMm,
                unit = "mm",
                satisfied =
                    result.rainfallThresholdMet
            ),
            DiseaseRiskFactor(
                name =
                    "germination_temperature",
                value = null,
                unit = null,
                satisfied =
                    result.germinationTemperatureMet
            ),
            DiseaseRiskFactor(
                name =
                    "soil_wet_hours",
                value =
                    result
                        .soilWetHours
                        .toDouble(),
                unit = "hours",
                satisfied =
                    if (
                        result.rainfallThresholdMet ==
                        null
                    ) {
                        null
                    } else {
                        result.soilWetHours >=
                                DownyMildewPrimaryInfectionCalculator
                                    .REQUIRED_SOIL_WET_HOURS
                    }
            ),
            DiseaseRiskFactor(
                name =
                    "germination_completed",
                value = null,
                unit = null,
                satisfied =
                    result.germinationCompleted
            ),
            DiseaseRiskFactor(
                name =
                    "subsequent_rain",
                value = null,
                unit = null,
                satisfied =
                    result.subsequentRainDetected
            ),
            DiseaseRiskFactor(
                name =
                    "maximum_wet_degree_hours",
                value =
                    result.maximumWetDegreeHours,
                unit = "°C·h",
                satisfied =
                    result.wetnessThresholdMet
            ),
            DiseaseRiskFactor(
                name =
                    "primary_infection_conditions",
                value = null,
                unit = null,
                satisfied =
                    result.infectionConditionsMet
            )
        )
    }

    private fun insufficientData(
        input: DiseaseRiskInput,
        calculatedAt: Long,
        previousState: DiseaseRiskModelState?
    ): DiseaseRiskModelEvaluation {

        return emptyEvaluation(
            input = input,
            calculatedAt = calculatedAt,
            status =
                DiseaseRiskEvaluationStatus
                    .INSUFFICIENT_DATA,
            nextState =
                previousState
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
                    .MODEL_STATE_ERROR,
            nextState = null
        )
    }

    private fun emptyEvaluation(
        input: DiseaseRiskInput,
        calculatedAt: Long,
        status: DiseaseRiskEvaluationStatus,
        nextState: DiseaseRiskModelState?
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
            nextState =
                nextState
        )
    }

    companion object {
        const val MODEL_VERSION =
            "DOWNY_PRIMARY_GROWCARE_V1"
    }
}