package com.rncoding.testvineshield.core.domain.disease_risk

object ContinuousWetPeriodExtractor {

    fun extract(
        weather: List<DiseaseRiskWeatherPoint>
    ): List<List<DiseaseRiskWeatherPoint>> {

        if (weather.isEmpty()) {
            return emptyList()
        }

        val sorted =
            weather.sortedBy {
                it.timestamp
            }

        val periods =
            mutableListOf<
                    MutableList<DiseaseRiskWeatherPoint>
                    >()

        var current =
            mutableListOf<DiseaseRiskWeatherPoint>()

        for (point in sorted) {

            if (point.leafWetness?.value == true) {

                val previous =
                    current.lastOrNull()

                val difference =
                    if (previous == null) {
                        null
                    } else {
                        point.timestamp - previous.timestamp
                    }

                val continuous =
                    previous == null ||
                            difference == ONE_HOUR_MILLIS
                if (!continuous) {
                    if (current.isNotEmpty()) {
                        periods += current
                    }

                    current =
                        mutableListOf()
                }

                current += point

            } else {
                if (current.isNotEmpty()) {
                    periods += current
                    current =
                        mutableListOf()
                }
            }
        }

        if (current.isNotEmpty()) {
            periods += current
        }

        return periods
    }

    private const val ONE_HOUR_MILLIS =
        60L * 60L * 1000L
}