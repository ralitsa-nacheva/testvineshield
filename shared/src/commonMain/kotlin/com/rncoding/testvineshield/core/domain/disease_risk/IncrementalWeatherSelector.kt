package com.rncoding.testvineshield.core.domain.disease_risk

object IncrementalWeatherSelector {

    sealed interface Selection {

        data class Ready(
            val weather: List<DiseaseRiskWeatherPoint>
        ) : Selection

        data object NoNewWeather : Selection

        data object InvalidTimeline : Selection
    }

    fun select(
        weather: List<DiseaseRiskWeatherPoint>,
        lastProcessedTimestamp: Long?
    ): Selection {

        if (weather.isEmpty()) {
            return Selection.NoNewWeather
        }

        val sorted = weather.sortedBy { it.timestamp }

        if (!HourlyWeatherContinuityValidator
                .isStrictlyContinuous(sorted)
        ) {
            return Selection.InvalidTimeline
        }

        val newWeather =
            if (lastProcessedTimestamp == null) {
                sorted
            } else {
                sorted.filter {
                    it.timestamp > lastProcessedTimestamp
                }
            }

        if (newWeather.isEmpty()) {
            return Selection.NoNewWeather
        }

        if (lastProcessedTimestamp != null) {

            val expectedNext =
                lastProcessedTimestamp +
                        HourlyWeatherContinuityValidator
                            .ONE_HOUR_MILLIS

            if (newWeather.first().timestamp != expectedNext) {
                return Selection.InvalidTimeline
            }
        }

        return Selection.Ready(newWeather)
    }
}