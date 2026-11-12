package com.rncoding.testvineshield.core.domain.disease_risk.downy

import com.rncoding.testvineshield.core.domain.disease_risk.ContinuousWetPeriodExtractor
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint
import com.rncoding.testvineshield.core.domain.disease_risk.HourlyWeatherContinuityValidator

class DownyMildewSecondaryInfectionCalculator {

    data class Result(
        val activeOilSpotsObserved: Boolean,

        val longestSporulationPeriodHours: Int,
        val sporulationConditionsMet: Boolean,
        val sporulationCompletedAt: Long?,

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
                sporulationCompletedAt = null,
                maximumWetDegreeHours = null,
                wetnessThresholdMet = null,
                infectionConditionsMet = false
            )
        }

        val weather = input.weather.sortedBy { it.timestamp }
        if (!HourlyWeatherContinuityValidator.isStrictlyContinuous(weather)) {
            return Result(
                activeOilSpotsObserved = true,
                longestSporulationPeriodHours = 0,
                sporulationConditionsMet = false,
                sporulationCompletedAt = null,
                maximumWetDegreeHours = null,
                wetnessThresholdMet = null,
                infectionConditionsMet = null
            )
        }

        val sporulation =
            evaluateSporulation(
                weather = weather
            )

        if (!sporulation.conditionsMet) {
            val missingInputsCouldQualify = evaluateSporulation(weather, allowUnknown = true).conditionsMet
            return Result(
                activeOilSpotsObserved = true,
                longestSporulationPeriodHours =
                    sporulation.longestPeriodHours,
                sporulationConditionsMet = false,
                sporulationCompletedAt = null,
                maximumWetDegreeHours = null,
                wetnessThresholdMet = null,
                infectionConditionsMet = if (missingInputsCouldQualify) null else false
            )
        }

        val sporulationCompletedAt =
            sporulation.completedAt

        if (sporulationCompletedAt == null) {
            return Result(
                activeOilSpotsObserved = true,
                longestSporulationPeriodHours =
                    sporulation.longestPeriodHours,
                sporulationConditionsMet = true,
                sporulationCompletedAt = null,
                maximumWetDegreeHours = null,
                wetnessThresholdMet = null,
                infectionConditionsMet = null
            )
        }

        val postSporulationWeather =
            weather.filter { point ->
                point.timestamp >
                        sporulationCompletedAt
            }

        if (postSporulationWeather.isEmpty()) {
            return Result(
                activeOilSpotsObserved = true,
                longestSporulationPeriodHours =
                    sporulation.longestPeriodHours,
                sporulationConditionsMet = true,
                sporulationCompletedAt =
                    sporulationCompletedAt,
                maximumWetDegreeHours = null,
                wetnessThresholdMet = null,
                infectionConditionsMet = null
            )
        }

        val wetnessEvidenceAvailable =
            postSporulationWeather.all { point ->
                point.leafWetness?.value != null
            }

        if (!wetnessEvidenceAvailable) {
            return Result(
                activeOilSpotsObserved = true,
                longestSporulationPeriodHours =
                    sporulation.longestPeriodHours,
                sporulationConditionsMet = true,
                sporulationCompletedAt =
                    sporulationCompletedAt,
                maximumWetDegreeHours = null,
                wetnessThresholdMet = null,
                infectionConditionsMet = null
            )
        }

        val wetPeriods =
            ContinuousWetPeriodExtractor.extract(
                postSporulationWeather
            )

        val maximumWetDegreeHours =
            wetPeriods
                .mapNotNull { wetPeriod ->
                    WetPeriodDegreeHourCalculator
                        .calculate(wetPeriod)
                }
                .maxOrNull()

        val wetnessThresholdMet =
            maximumWetDegreeHours?.let {
                it >= REQUIRED_WET_DEGREE_HOURS
            } ?: false

        return Result(
            activeOilSpotsObserved = true,
            longestSporulationPeriodHours =
                sporulation.longestPeriodHours,
            sporulationConditionsMet = true,
            sporulationCompletedAt =
                sporulationCompletedAt,
            maximumWetDegreeHours =
                maximumWetDegreeHours,
            wetnessThresholdMet =
                wetnessThresholdMet,
            infectionConditionsMet =
                wetnessThresholdMet
        )
    }

    private fun evaluateSporulation(
        weather: List<DiseaseRiskWeatherPoint>,
        allowUnknown: Boolean = false
    ): SporulationEvaluation {

        var currentHours = 0
        var longestHours = 0

        var previousQualifyingTimestamp: Long? =
            null

        var firstCompletionTimestamp: Long? =
            null

        for (point in weather) {

            val qualifies =
                qualifiesForSporulation(
                    point = point,
                    allowUnknown = allowUnknown
                )

            if (!qualifies) {
                currentHours = 0
                previousQualifyingTimestamp = null
                continue
            }

            val previousTimestamp =
                previousQualifyingTimestamp

            val continuous =
                previousTimestamp == null ||
                        point.timestamp -
                        previousTimestamp ==
                        ONE_HOUR_MILLIS

            if (continuous) {
                currentHours += 1
            } else {
                currentHours = 1
            }

            longestHours =
                maxOf(
                    longestHours,
                    currentHours
                )

            if (
                currentHours >=
                REQUIRED_SPORULATION_HOURS &&
                firstCompletionTimestamp == null
            ) {
                firstCompletionTimestamp =
                    point.timestamp
            }

            previousQualifyingTimestamp =
                point.timestamp
        }

        return SporulationEvaluation(
            longestPeriodHours =
                longestHours,
            conditionsMet =
                firstCompletionTimestamp != null,
            completedAt =
                firstCompletionTimestamp
        )
    }

    private fun qualifiesForSporulation(
        point: DiseaseRiskWeatherPoint,
        allowUnknown: Boolean
    ): Boolean {
        val temperature = point.temperatureCelsius.takeIf { it.isFinite() }
        val humidity = point.relativeHumidityPercent?.takeIf { it in 0.0..100.0 }
        if (point.isDay == true || (temperature != null && temperature <= MIN_SPORULATION_TEMP_C)) return false
        if (humidity != null && humidity <= MIN_SPORULATION_RH_PERCENT) return false

        // The second scan asks whether missing evidence could change a negative result.
        return allowUnknown || (point.isDay == false && temperature != null && humidity != null)
    }

    private data class SporulationEvaluation(
        val longestPeriodHours: Int,
        val conditionsMet: Boolean,
        val completedAt: Long?
    )

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