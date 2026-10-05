package com.rncoding.testvineshield.core.domain.disease_risk.downy

import com.rncoding.testvineshield.core.domain.disease_risk.ContinuousWetPeriodExtractor

class DownyMildewPrimaryInfectionCalculator {

    data class Result(
        val rainfallMm: Double,
        val rainfallThresholdMm: Double?,
        val rainfallThresholdMet: Boolean?,
        val maximumWetDegreeHours: Double?,
        val wetnessThresholdMet: Boolean?,
        val infectionConditionsMet: Boolean?
    )

    fun evaluate(
        input: DownyMildewPrimaryInput
    ): Result {

        val rainfall =
            input.weather.sumOf {
                it.precipitationMm ?: 0.0
            }

        val rainfallThreshold =
            when (input.soilWetnessEvidence) {
                SoilWetnessEvidence.WET ->
                    WET_SOIL_RAINFALL_MM

                SoilWetnessEvidence.DRY ->
                    DRY_SOIL_RAINFALL_MM

                SoilWetnessEvidence.UNKNOWN ->
                    null
            }

        val rainfallThresholdMet =
            rainfallThreshold?.let {
                rainfall >= it
            }

        val wetPeriods =
            ContinuousWetPeriodExtractor.extract(
                input.weather
            )

        val maximumDegreeHours =
            wetPeriods
                .mapNotNull {
                    LeafWetnessDegreeHourCalculator
                        .calculate(it)
                }
                .maxOrNull()

        val wetnessThresholdMet =
            maximumDegreeHours?.let {
                it >= REQUIRED_WET_DEGREE_HOURS
            }

        val conditionsMet =
            if (
                rainfallThresholdMet == null ||
                wetnessThresholdMet == null
            ) {
                null
            } else {
                rainfallThresholdMet &&
                        wetnessThresholdMet
            }

        return Result(
            rainfallMm = rainfall,
            rainfallThresholdMm =
                rainfallThreshold,
            rainfallThresholdMet =
                rainfallThresholdMet,
            maximumWetDegreeHours =
                maximumDegreeHours,
            wetnessThresholdMet =
                wetnessThresholdMet,
            infectionConditionsMet =
                conditionsMet
        )
    }

    companion object {
        const val WET_SOIL_RAINFALL_MM = 3.0
        const val DRY_SOIL_RAINFALL_MM = 5.0

        const val REQUIRED_WET_DEGREE_HOURS =
            45.0
    }
}