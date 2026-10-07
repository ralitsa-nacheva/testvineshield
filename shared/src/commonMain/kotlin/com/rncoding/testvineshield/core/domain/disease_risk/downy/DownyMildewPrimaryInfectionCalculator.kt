package com.rncoding.testvineshield.core.domain.disease_risk.downy

import com.rncoding.testvineshield.core.domain.disease_risk.ContinuousWetPeriodExtractor

class DownyMildewPrimaryInfectionCalculator {

    data class Evaluation(
        val state: DownyMildewPrimaryState,
        val result: Result
    )

    data class Result(
        val rainfallMm: Double,
        val rainfallThresholdMm: Double?,
        val rainfallThresholdMet: Boolean?,
        val germinationTemperatureMet: Boolean,
        val soilWetHours: Int,
        val germinationCompleted: Boolean,
        val subsequentRainDetected: Boolean,
        val maximumWetDegreeHours: Double?,
        val wetnessThresholdMet: Boolean?,
        val infectionConditionsMet: Boolean?
    )

    fun evaluate(
        input: DownyMildewPrimaryInput
    ): Evaluation {

        if (input.weather.isEmpty()) {
            return emptyEvaluation(
                previousState = input.previousState
            )
        }

        val weather =
            input.weather.sortedBy {
                it.timestamp
            }

        val rainfallMm =
            weather.sumOf {
                it.precipitationMm ?: 0.0
            }

        val rainfallThreshold =
            rainfallThreshold(
                input.soilWetnessEvidence
            )

        val rainfallThresholdMet =
            rainfallThreshold?.let {
                rainfallMm >= it
            }

        val germinationTemperatureMet =
            weather.any {
                it.temperatureCelsius >
                        MIN_GERMINATION_START_TEMP_C
            }

        var state =
            input.previousState

        if (
            state.phase ==
            DownyMildewPrimaryState.Phase.WAITING_FOR_TRIGGER &&
            rainfallThresholdMet == true &&
            germinationTemperatureMet
        ) {
            state =
                state.copy(
                    phase =
                        DownyMildewPrimaryState.Phase
                            .GERMINATION_IN_PROGRESS,
                    triggerStartedAt =
                        weather.first().timestamp,
                    soilWetHours = 0,
                    consecutiveDryHours = 0,
                    germinationCompletedAt = null
                )
        }

        if (
            state.phase ==
            DownyMildewPrimaryState.Phase
                .GERMINATION_IN_PROGRESS
        ) {
            state =
                evaluateGermination(
                    weather = weather,
                    soilWetnessEvidence =
                        input.soilWetnessEvidence,
                    state = state
                )
        }

        val germinationCompleted =
            state.phase ==
                    DownyMildewPrimaryState.Phase
                        .GERMINATION_COMPLETE

        val subsequentRainDetected =
            if (
                germinationCompleted &&
                state.germinationCompletedAt != null
            ) {
                weather.any { point ->
                    point.timestamp >
                            state.germinationCompletedAt &&
                            (point.precipitationMm ?: 0.0) > 0.0
                }
            } else {
                false
            }

        val wetPeriods =
            if (subsequentRainDetected) {
                ContinuousWetPeriodExtractor.extract(
                    weather.filter { point ->
                        val completedAt =
                            state.germinationCompletedAt

                        completedAt != null &&
                                point.timestamp >
                                completedAt
                    }
                )
            } else {
                emptyList()
            }

        val maximumWetDegreeHours =
            if (subsequentRainDetected) {
                wetPeriods
                    .mapNotNull { wetPeriod ->
                        WetPeriodDegreeHourCalculator
                            .calculate(wetPeriod)
                    }
                    .maxOrNull()
            } else {
                null
            }

        val wetnessThresholdMet =
            maximumWetDegreeHours?.let {
                it >= REQUIRED_WET_DEGREE_HOURS
            }

        val infectionConditionsMet =
            when {
                rainfallThresholdMet == null ->
                    null

                !germinationCompleted ->
                    false

                !subsequentRainDetected ->
                    false

                wetnessThresholdMet == null ->
                    null

                else ->
                    wetnessThresholdMet
            }

        return Evaluation(
            state = state,
            result =
                Result(
                    rainfallMm = rainfallMm,
                    rainfallThresholdMm =
                        rainfallThreshold,
                    rainfallThresholdMet =
                        rainfallThresholdMet,
                    germinationTemperatureMet =
                        germinationTemperatureMet,
                    soilWetHours =
                        state.soilWetHours,
                    germinationCompleted =
                        germinationCompleted,
                    subsequentRainDetected =
                        subsequentRainDetected,
                    maximumWetDegreeHours =
                        maximumWetDegreeHours,
                    wetnessThresholdMet =
                        wetnessThresholdMet,
                    infectionConditionsMet =
                        infectionConditionsMet
                )
        )
    }

    private fun evaluateGermination(
        weather:
        List<com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint>,
        soilWetnessEvidence: SoilWetnessEvidence,
        state: DownyMildewPrimaryState
    ): DownyMildewPrimaryState {

        var soilWetHours =
            state.soilWetHours

        var consecutiveDryHours =
            state.consecutiveDryHours

        var currentState =
            state

        for (point in weather) {

            if (
                point.temperatureCelsius <
                MIN_GERMINATION_CONTINUATION_TEMP_C
            ) {
                continue
            }

            when (soilWetnessEvidence) {

                SoilWetnessEvidence.WET -> {
                    soilWetHours += 1
                    consecutiveDryHours = 0
                }

                SoilWetnessEvidence.DRY -> {
                    consecutiveDryHours += 1

                    if (
                        consecutiveDryHours >
                        MAX_CONSECUTIVE_DRY_HOURS
                    ) {
                        return DownyMildewPrimaryState()
                    }
                }

                SoilWetnessEvidence.UNKNOWN -> {
                    return currentState.copy(
                        soilWetHours = soilWetHours,
                        consecutiveDryHours =
                            consecutiveDryHours
                    )
                }
            }

            if (
                soilWetHours >=
                REQUIRED_SOIL_WET_HOURS
            ) {
                currentState =
                    currentState.copy(
                        phase =
                            DownyMildewPrimaryState.Phase
                                .GERMINATION_COMPLETE,
                        soilWetHours =
                            soilWetHours,
                        consecutiveDryHours =
                            consecutiveDryHours,
                        germinationCompletedAt =
                            point.timestamp
                    )

                return currentState
            }
        }

        return currentState.copy(
            soilWetHours = soilWetHours,
            consecutiveDryHours =
                consecutiveDryHours
        )
    }

    private fun rainfallThreshold(
        evidence: SoilWetnessEvidence
    ): Double? {
        return when (evidence) {
            SoilWetnessEvidence.WET ->
                WET_SOIL_RAINFALL_MM

            SoilWetnessEvidence.DRY ->
                DRY_SOIL_RAINFALL_MM

            SoilWetnessEvidence.UNKNOWN ->
                null
        }
    }

    private fun emptyEvaluation(
        previousState: DownyMildewPrimaryState
    ): Evaluation {

        return Evaluation(
            state = previousState,
            result =
                Result(
                    rainfallMm = 0.0,
                    rainfallThresholdMm = null,
                    rainfallThresholdMet = null,
                    germinationTemperatureMet = false,
                    soilWetHours =
                        previousState.soilWetHours,
                    germinationCompleted =
                        previousState.phase ==
                                DownyMildewPrimaryState.Phase
                                    .GERMINATION_COMPLETE,
                    subsequentRainDetected = false,
                    maximumWetDegreeHours = null,
                    wetnessThresholdMet = null,
                    infectionConditionsMet = null
                )
        )
    }

    companion object {

        const val WET_SOIL_RAINFALL_MM = 3.0
        const val DRY_SOIL_RAINFALL_MM = 5.0

        const val MIN_GERMINATION_START_TEMP_C =
            10.0

        const val MIN_GERMINATION_CONTINUATION_TEMP_C =
            8.0

        const val REQUIRED_SOIL_WET_HOURS =
            16

        const val MAX_CONSECUTIVE_DRY_HOURS =
            3

        const val REQUIRED_WET_DEGREE_HOURS =
            45.0
    }
}