package com.rncoding.testvineshield.core.domain.disease_risk.downy

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint

class DownyMildewIncubationCalculator {

    fun predict(
        infectionTimestamp: Long,
        weatherAfterInfection: List<DiseaseRiskWeatherPoint>
    ): DownyMildewIncubationPrediction {

        val relevantWeather =
            weatherAfterInfection
                .filter {
                    it.timestamp >= infectionTimestamp
                }
                .sortedBy {
                    it.timestamp
                }

        if (relevantWeather.isEmpty()) {
            return broadPrediction(
                infectionTimestamp =
                    infectionTimestamp
            )
        }

        val meanTemperature =
            relevantWeather
                .map {
                    it.temperatureCelsius
                }
                .average()

        val window =
            symptomWindowForTemperature(
                meanTemperatureCelsius =
                    meanTemperature
            )

        return DownyMildewIncubationPrediction(
            infectionTimestamp =
                infectionTimestamp,
            earliestExpectedSymptomsAt =
                infectionTimestamp +
                        daysToMillis(
                            window.earliestDay
                        ),
            latestExpectedSymptomsAt =
                infectionTimestamp +
                        daysToMillis(
                            window.latestDay
                        ),
            confidence =
                DownyMildewIncubationPrediction
                    .Confidence.MODERATE
        )
    }

    private fun broadPrediction(
        infectionTimestamp: Long
    ): DownyMildewIncubationPrediction {

        return DownyMildewIncubationPrediction(
            infectionTimestamp =
                infectionTimestamp,
            earliestExpectedSymptomsAt =
                infectionTimestamp +
                        daysToMillis(
                            WARM_EARLIEST_DAY
                        ),
            latestExpectedSymptomsAt =
                infectionTimestamp +
                        daysToMillis(
                            COOL_LATEST_DAY
                        ),
            confidence =
                DownyMildewIncubationPrediction
                    .Confidence.LOW
        )
    }

    private fun symptomWindowForTemperature(
        meanTemperatureCelsius: Double
    ): SymptomWindow {

        return if (
            meanTemperatureCelsius >=
            WARM_CONDITION_THRESHOLD_C
        ) {
            SymptomWindow(
                earliestDay =
                    WARM_EARLIEST_DAY,
                latestDay =
                    WARM_LATEST_DAY
            )
        } else {
            SymptomWindow(
                earliestDay =
                    COOL_EARLIEST_DAY,
                latestDay =
                    COOL_LATEST_DAY
            )
        }
    }

    private fun daysToMillis(
        days: Int
    ): Long {
        return days.toLong() *
                MILLIS_PER_DAY
    }

    private data class SymptomWindow(
        val earliestDay: Int,
        val latestDay: Int
    )

    companion object {

        /*
         * Operational split only.
         *
         * The NSW source supports approximately
         * 4–6 days in warm conditions and
         * 10–14 days in cool conditions.
         *
         * It does not define one exact temperature
         * at which "warm" becomes "cool".
         *
         * Therefore this threshold must NOT be
         * presented as a published biological
         * constant.
         */
        const val WARM_CONDITION_THRESHOLD_C =
            20.0

        const val WARM_EARLIEST_DAY = 4
        const val WARM_LATEST_DAY = 6

        const val COOL_EARLIEST_DAY = 10
        const val COOL_LATEST_DAY = 14

        private const val MILLIS_PER_DAY =
            24L * 60L * 60L * 1000L
    }
}