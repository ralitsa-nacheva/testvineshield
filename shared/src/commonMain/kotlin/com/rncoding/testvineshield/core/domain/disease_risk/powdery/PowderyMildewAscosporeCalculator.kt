package com.rncoding.testvineshield.core.domain.disease_risk.powdery

import com.rncoding.testvineshield.core.domain.disease_risk.ContinuousWetPeriodExtractor
import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint

class PowderyMildewAscosporeCalculator {

    fun evaluate(
        input: PowderyMildewAscosporeInput
    ): PowderyMildewAscosporeResult {

        if (input.weather.isEmpty()) {
            return emptyResult()
        }

        val weather =
            input.weather.sortedBy {
                it.timestamp
            }

        val precipitationMm =
            weather.sumOf {
                it.precipitationMm ?: 0.0
            }

        val releaseConditionsMet =
            precipitationMm >=
                    MIN_RELEASE_PRECIPITATION_MM &&
                    weather.any {
                        it.temperatureCelsius >
                                MIN_RELEASE_TEMPERATURE_C
                    }

        if (!releaseConditionsMet) {
            return PowderyMildewAscosporeResult(
                precipitationMm = precipitationMm,
                releaseConditionsMet = false,
                longestWetPeriodHours = null,
                wetPeriodMeanTemperatureCelsius = null,
                infectionConditionsMet = false
            )
        }

        if (
            weather.any {
                it.leafWetness?.value == null
            }
        ) {
            return PowderyMildewAscosporeResult(
                precipitationMm = precipitationMm,
                releaseConditionsMet = true,
                longestWetPeriodHours = null,
                wetPeriodMeanTemperatureCelsius = null,
                infectionConditionsMet = null
            )
        }

        val wetPeriods =
            ContinuousWetPeriodExtractor.extract(
                weather
            )

        if (wetPeriods.isEmpty()) {
            return PowderyMildewAscosporeResult(
                precipitationMm = precipitationMm,
                releaseConditionsMet = true,
                longestWetPeriodHours = 0,
                wetPeriodMeanTemperatureCelsius = null,
                infectionConditionsMet = false
            )
        }

        val qualifyingWetPeriod =
            wetPeriods
                .map { wetPeriod ->
                    WetPeriodEvaluation(
                        hours = wetPeriod.size,
                        meanTemperatureCelsius =
                            wetPeriod
                                .map {
                                    it.temperatureCelsius
                                }
                                .average()
                    )
                }
                .filter { evaluation ->
                    evaluation
                        .meanTemperatureCelsius >=
                            MIN_INFECTION_TEMPERATURE_C &&
                            evaluation
                                .meanTemperatureCelsius <=
                            MAX_INFECTION_TEMPERATURE_C
                }
                .maxByOrNull {
                    it.hours
                }

        val longestWetPeriod =
            wetPeriods.maxByOrNull {
                it.size
            }

        val longestWetPeriodMeanTemperature =
            longestWetPeriod
                ?.map {
                    it.temperatureCelsius
                }
                ?.average()

        val infectionConditionsMet =
            qualifyingWetPeriod?.hours
                ?.let {
                    it >=
                            REQUIRED_INFECTION_WET_HOURS
                }
                ?: false

        return PowderyMildewAscosporeResult(
            precipitationMm = precipitationMm,
            releaseConditionsMet = true,
            longestWetPeriodHours =
                longestWetPeriod?.size,
            wetPeriodMeanTemperatureCelsius =
                longestWetPeriodMeanTemperature,
            infectionConditionsMet =
                infectionConditionsMet
        )
    }

    private fun emptyResult():
            PowderyMildewAscosporeResult {

        return PowderyMildewAscosporeResult(
            precipitationMm = 0.0,
            releaseConditionsMet = false,
            longestWetPeriodHours = null,
            wetPeriodMeanTemperatureCelsius = null,
            infectionConditionsMet = null
        )
    }

    private data class WetPeriodEvaluation(
        val hours: Int,
        val meanTemperatureCelsius: Double
    )

    companion object {

        const val MIN_RELEASE_PRECIPITATION_MM =
            2.5

        const val MIN_RELEASE_TEMPERATURE_C =
            10.0

        const val MIN_INFECTION_TEMPERATURE_C =
            10.0

        const val MAX_INFECTION_TEMPERATURE_C =
            15.0

        const val REQUIRED_INFECTION_WET_HOURS =
            12
    }
}