package com.rncoding.testvineshield.core.domain.disease_risk.downy

import com.rncoding.testvineshield.core.domain.disease_risk.ContinuousWetPeriodExtractor
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint

class DownyMildewSecondaryInfectionCalculator {

    data class Result(
        val activeOilSpotsObserved: Boolean,
        val longestSporulationPeriodHours: Int,
        val sporulationConditionsMet: Boolean,
        val maximumWetDegreeHours: Double?,
        val wetnessThresholdMet: Boolean?,
        val infectionConditionsMet: Boolean?
    )

    fun evaluate(
        input: DownyMildewSecondaryInput
    ): Result {

        if (!input.activeOilSpotsObserved) {
            return Result(
                activeOilSpotsObserved = false,
                longestSporulationPeriodHours = 0,
                sporulationConditionsMet = false,
                maximumWetDegreeHours = null,
                wetnessThresholdMet = null,
                infectionConditionsMet = false
            )
        }

        val longestSporulationPeriodHours =
            longestContinuousSporulationPeriod(
                input.weather
            )

        val sporulationConditionsMet =
            longestSporulationPeriodHours >=
                    REQUIRED_SPORULATION_HOURS

        val wetPeriods =
            ContinuousWetPeriodExtractor.extract(
                input.weather
            )

        val maximumWetDegreeHours =
            wetPeriods
                .mapNotNull { wetPeriod ->
                    LeafWetnessDegreeHourCalculator
                        .calculate(wetPeriod)
                }
                .maxOrNull()

        val wetnessThresholdMet =
            maximumWetDegreeHours?.let {
                it >= REQUIRED_WET_DEGREE_HOURS
            }

        val infectionConditionsMet =
            wetnessThresholdMet?.let {
                sporulationConditionsMet && it
            }

        return Result(
            activeOilSpotsObserved = true,
            longestSporulationPeriodHours =
                longestSporulationPeriodHours,
            sporulationConditionsMet =
                sporulationConditionsMet,
            maximumWetDegreeHours =
                maximumWetDegreeHours,
            wetnessThresholdMet =
                wetnessThresholdMet,
            infectionConditionsMet =
                infectionConditionsMet
        )
    }

    private fun longestContinuousSporulationPeriod(
        weather: List<DiseaseRiskWeatherPoint>
    ): Int {

        val sorted =
            weather.sortedBy {
                it.timestamp
            }

        var longest = 0
        var current = 0
        var previousTimestamp: Long? = null

        for (point in sorted) {

            val humidity =
                point.relativeHumidityPercent

            val qualifies =
                point.isDay == false &&
                        humidity != null &&
                        point.temperatureCelsius >=
                        MIN_SPORULATION_TEMP_C &&
                        humidity >=
                        MIN_SPORULATION_RH_PERCENT

            val previous =
                previousTimestamp

            val continuous =
                previous == null ||
                        point.timestamp - previous <=
                        ONE_HOUR_MILLIS

            if (qualifies && continuous) {
                current += 1
                longest = maxOf(
                    longest,
                    current
                )
            } else if (qualifies) {
                current = 1
                longest = maxOf(
                    longest,
                    current
                )
            } else {
                current = 0
            }

            previousTimestamp =
                point.timestamp
        }

        return longest
    }

    companion object {
        const val MIN_SPORULATION_TEMP_C =
            13.0

        const val MIN_SPORULATION_RH_PERCENT =
            98.0

        const val REQUIRED_SPORULATION_HOURS =
            4

        const val REQUIRED_WET_DEGREE_HOURS =
            45.0

        private const val ONE_HOUR_MILLIS =
            60L * 60L * 1000L
    }
}