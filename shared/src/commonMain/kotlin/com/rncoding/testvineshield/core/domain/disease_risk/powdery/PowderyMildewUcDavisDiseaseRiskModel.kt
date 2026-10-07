package com.rncoding.testvineshield.core.domain.disease_risk.powdery

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskConfidence
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

class PowderyMildewUcDavisDiseaseRiskModel(
    private val calculator:
    PowderyMildewUcDavisCalculator =
        PowderyMildewUcDavisCalculator()
) : DiseaseRiskModel {

    override val modelType =
        DiseaseRiskModelType.POWDERY_MILDEW_UC_DAVIS

    override val modelVersion =
        PowderyMildewUcDavisCalculator.MODEL_VERSION

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

        val initialState =
            when (previousState) {

                null ->
                    PowderyMildewRiskState()

                is PowderyMildewRiskState ->
                    previousState

                else ->
                    return stateError(
                        input = input,
                        calculatedAt = calculatedAt
                    )
            }

        val dailyWeather =
            try {
                PowderyMildewDailyWeatherAdapter.map(
                    weather = input.hourlyWeather,
                    timeZoneId = input.timeZone
                )
            } catch (_: IllegalArgumentException) {
                return insufficientData(
                    input = input,
                    calculatedAt = calculatedAt,
                    previousState = initialState
                )
            }

        if (dailyWeather.isEmpty()) {
            return insufficientData(
                input = input,
                calculatedAt = calculatedAt,
                previousState = initialState
            )
        }

        var currentState =
            initialState

        var latestResult:
                PowderyMildewDailyRiskResult? =
            null

        for (day in dailyWeather) {

            val lastEvaluatedDate =
                currentState.lastEvaluatedDate

            if (
                lastEvaluatedDate != null &&
                day.date <= lastEvaluatedDate
            ) {
                continue
            }

            val evaluation =
                calculator.evaluateDay(
                    day = day,
                    previousState = currentState
                )

            currentState =
                evaluation.state

            latestResult =
                evaluation.dailyResult
        }

        if (latestResult == null) {
            return noNewData(
                input = input,
                calculatedAt = calculatedAt,
                state = currentState
            )
        }

        val result =
            latestResult

        val riskLevel =
            PowderyMildewRiskLevelMapper.map(
                index = currentState.currentIndex,
                initiated = currentState.initiated
            )

        val event =
            when {

                !initialState.initiated &&
                        currentState.initiated ->

                    DiseaseRiskEvent
                        .CONIDIAL_RISK_INITIATED

                currentState.initiated ->

                    DiseaseRiskEvent
                        .CONIDIAL_RISK_UPDATED

                else ->
                    null
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
                        DiseaseRiskConfidence.HIGH,

                    event =
                        event,

                    nativeRiskValue =
                        currentState
                            .currentIndex
                            .toDouble(),

                    factors =
                        buildFactors(
                            result = result,
                            state = currentState
                        )
                ),
            nextState =
                currentState
        )
    }

    private fun buildFactors(
        result: PowderyMildewDailyRiskResult,
        state: PowderyMildewRiskState
    ): List<DiseaseRiskFactor> {

        return listOf(
            DiseaseRiskFactor(
                name =
                    "qualifying_temperature_hours",
                value =
                    result
                        .qualifyingHours
                        .toDouble(),
                unit = "hours",
                satisfied =
                    result.qualifyingDay
            ),
            DiseaseRiskFactor(
                name =
                    "high_temperature_penalty",
                value = null,
                unit = null,
                satisfied =
                    result.highTemperaturePenalty
            ),
            DiseaseRiskFactor(
                name =
                    "consecutive_qualifying_days",
                value =
                    state
                        .consecutiveQualifyingDays
                        .toDouble(),
                unit = "days",
                satisfied = null
            ),
            DiseaseRiskFactor(
                name =
                    "uc_davis_risk_index",
                value =
                    result
                        .riskIndex
                        .toDouble(),
                unit = "index",
                satisfied =
                    result.initiated
            )
        )
    }

    private fun noNewData(
        input: DiseaseRiskInput,
        calculatedAt: Long,
        state: PowderyMildewRiskState
    ): DiseaseRiskModelEvaluation {

        val riskLevel =
            PowderyMildewRiskLevelMapper.map(
                index = state.currentIndex,
                initiated = state.initiated
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
                        DiseaseRiskConfidence.HIGH,

                    event = null,

                    nativeRiskValue =
                        state
                            .currentIndex
                            .toDouble(),

                    factors =
                        emptyList()
                ),
            nextState =
                state
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
}