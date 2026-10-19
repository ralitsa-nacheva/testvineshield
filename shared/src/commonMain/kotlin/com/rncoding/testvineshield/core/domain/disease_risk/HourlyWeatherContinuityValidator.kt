package com.rncoding.testvineshield.core.domain.disease_risk

object HourlyWeatherContinuityValidator {

    const val ONE_HOUR_MILLIS = 3_600_000L

    fun isStrictlyContinuous(
        weather: List<DiseaseRiskWeatherPoint>
    ): Boolean {

        if (weather.isEmpty()) {
            return false
        }

        return weather.zipWithNext().all { (previous, current) ->
            current.timestamp - previous.timestamp ==
                    ONE_HOUR_MILLIS
        }
    }

    fun isStrictlyIncreasing(
        weather: List<DiseaseRiskWeatherPoint>
    ): Boolean {

        return weather.zipWithNext().all { (previous, current) ->
            current.timestamp > previous.timestamp
        }
    }

    fun validateWindowCompleteness(
        weather: List<DiseaseRiskWeatherPoint>,
        window: DiseaseRiskWeatherWindow
    ): String? {

        val start = window.startTimestamp
        val end = window.endTimestamp

        if (start == end) {
            return "Weather window is empty."
        }

        val duration = end - start

        if (duration <= 0L || duration % ONE_HOUR_MILLIS != 0L) {
            return "Weather window must contain complete hourly intervals."
        }

        val expectedCount = duration / ONE_HOUR_MILLIS

        if (weather.size.toLong() != expectedCount) {
            return "Expected $expectedCount hourly observations, " +
                    "but received ${weather.size}."
        }

        if (weather.first().timestamp != start) {
            return "First weather observation does not match window start."
        }

        if (weather.last().timestamp != end - ONE_HOUR_MILLIS) {
            return "Last weather observation does not match window end."
        }

        if (!isStrictlyContinuous(weather)) {
            return "Weather observations are not strictly continuous."
        }

        return null
    }
}