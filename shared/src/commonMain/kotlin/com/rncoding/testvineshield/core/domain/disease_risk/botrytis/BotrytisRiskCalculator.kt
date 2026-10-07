package com.rncoding.testvineshield.core.domain.disease_risk.botrytis

import com.rncoding.testvineshield.core.domain.disease_risk.ContinuousWetPeriodExtractor
import kotlin.math.exp
import kotlin.math.pow

class BotrytisRiskCalculator {

    fun evaluate(
        input: BotrytisRiskInput
    ): BotrytisRiskResult {

        if (
            input.hostTissue ==
            BotrytisHostTissue.UNSUPPORTED
        ) {
            return unavailableResult(
                hostTissue =
                    input.hostTissue
            )
        }

        if (input.weather.isEmpty()) {
            return unavailableResult(
                hostTissue =
                    input.hostTissue
            )
        }

        if (
            input.weather.any {
                it.leafWetness?.value == null
            }
        ) {
            return unavailableResult(
                hostTissue =
                    input.hostTissue
            )
        }

        val wetPeriods =
            ContinuousWetPeriodExtractor.extract(
                input.weather
            )

        if (wetPeriods.isEmpty()) {
            return BotrytisRiskResult(
                hostTissue =
                    input.hostTissue,
                longestWetPeriodHours = 0,
                wetPeriodMeanTemperatureCelsius =
                    null,
                predictedInfectionPercent = 0.0,
                riskLevel =
                    BotrytisRiskResult.RiskLevel.NONE,
                evaluationAvailable = true
            )
        }

        val evaluations =
            wetPeriods.map { wetPeriod ->

                val wetnessHours =
                    wetPeriod.size.toDouble()

                val meanTemperature =
                    wetPeriod
                        .map {
                            it.temperatureCelsius
                        }
                        .average()

                WetPeriodEvaluation(
                    hours =
                        wetPeriod.size,
                    meanTemperatureCelsius =
                        meanTemperature,
                    infectionPercent =
                        calculateInfectionPercent(
                            hostTissue =
                                input.hostTissue,
                            temperatureCelsius =
                                meanTemperature,
                            wetnessHours =
                                wetnessHours
                        )
                )
            }

        val highestRiskPeriod =
            evaluations.maxByOrNull {
                it.infectionPercent
            } ?: return unavailableResult(
                hostTissue =
                    input.hostTissue
            )

        return BotrytisRiskResult(
            hostTissue =
                input.hostTissue,
            longestWetPeriodHours =
                highestRiskPeriod.hours,
            wetPeriodMeanTemperatureCelsius =
                highestRiskPeriod
                    .meanTemperatureCelsius,
            predictedInfectionPercent =
                highestRiskPeriod
                    .infectionPercent,
            riskLevel =
                mapRiskLevel(
                    highestRiskPeriod
                        .infectionPercent
                ),
            evaluationAvailable = true
        )
    }

    private fun calculateInfectionPercent(
        hostTissue: BotrytisHostTissue,
        temperatureCelsius: Double,
        wetnessHours: Double
    ): Double {

        val parameters =
            parametersFor(
                hostTissue
            )

        val temperatureEffect =
            exp(
                -(
                        (
                                temperatureCelsius -
                                        parameters.optimalTemperatureCelsius
                                ) /
                                parameters.temperatureRange
                        ).pow(2.0)
            )

        val timeComponent =
            (
                    temperatureEffect *
                            wetnessHours /
                            parameters.timeConstantHours
                    ).pow(2.0)

        val infectionPercent =
            parameters.maximumInfectionPercent *
                    (
                            1.0 -
                                    exp(
                                        -timeComponent
                                    )
                            )

        return infectionPercent.coerceIn(
            minimumValue = 0.0,
            maximumValue =
                parameters.maximumInfectionPercent
        )
    }

    private fun parametersFor(
        hostTissue: BotrytisHostTissue
    ): Parameters {

        return when (hostTissue) {

            BotrytisHostTissue.FLOWERS ->
                Parameters(
                    maximumInfectionPercent =
                        FLOWER_MAX_INFECTION_PERCENT,
                    timeConstantHours =
                        FLOWER_TIME_CONSTANT_HOURS,
                    optimalTemperatureCelsius =
                        FLOWER_OPTIMAL_TEMP_C,
                    temperatureRange =
                        FLOWER_TEMP_RANGE_C
                )

            BotrytisHostTissue.MATURE_BERRIES ->
                Parameters(
                    maximumInfectionPercent =
                        BERRY_MAX_INFECTION_PERCENT,
                    timeConstantHours =
                        BERRY_TIME_CONSTANT_HOURS,
                    optimalTemperatureCelsius =
                        BERRY_OPTIMAL_TEMP_C,
                    temperatureRange =
                        BERRY_TEMP_RANGE_C
                )

            BotrytisHostTissue.UNSUPPORTED ->
                error(
                    "Unsupported Botrytis host tissue"
                )
        }
    }

    private fun mapRiskLevel(
        infectionPercent: Double
    ): BotrytisRiskResult.RiskLevel {

        return when {
            infectionPercent <= 0.0 ->
                BotrytisRiskResult.RiskLevel.NONE

            infectionPercent < LOW_TO_MODERATE_PERCENT ->
                BotrytisRiskResult.RiskLevel.LOW

            infectionPercent < MODERATE_TO_HIGH_PERCENT ->
                BotrytisRiskResult.RiskLevel.MODERATE

            else ->
                BotrytisRiskResult.RiskLevel.HIGH
        }
    }

    private fun unavailableResult(
        hostTissue: BotrytisHostTissue
    ): BotrytisRiskResult {

        return BotrytisRiskResult(
            hostTissue = hostTissue,
            longestWetPeriodHours = null,
            wetPeriodMeanTemperatureCelsius = null,
            predictedInfectionPercent = null,
            riskLevel =
                BotrytisRiskResult.RiskLevel.NONE,
            evaluationAvailable = false
        )
    }

    private data class WetPeriodEvaluation(
        val hours: Int,
        val meanTemperatureCelsius: Double,
        val infectionPercent: Double
    )

    private data class Parameters(
        val maximumInfectionPercent: Double,
        val timeConstantHours: Double,
        val optimalTemperatureCelsius: Double,
        val temperatureRange: Double
    )

    companion object {

        /*
         * Nair & Allen (1993) parameters.
         */

        const val FLOWER_MAX_INFECTION_PERCENT =
            95.6

        const val FLOWER_TIME_CONSTANT_HOURS =
            1.3

        const val FLOWER_OPTIMAL_TEMP_C =
            23.7

        const val FLOWER_TEMP_RANGE_C =
            10.5

        const val BERRY_MAX_INFECTION_PERCENT =
            100.0

        const val BERRY_TIME_CONSTANT_HOURS =
            13.9

        const val BERRY_OPTIMAL_TEMP_C =
            20.8

        const val BERRY_TEMP_RANGE_C =
            8.9

        /*
         * Application-level interpretation bands.
         * These are NOT Nair & Allen constants.
         */
        const val LOW_TO_MODERATE_PERCENT =
            25.0

        const val MODERATE_TO_HIGH_PERCENT =
            50.0
    }
}