package com.rncoding.testvineshield.core.domain.disease_risk.downy

import com.rncoding.testvineshield.core.domain.disease_risk.DiseaseRiskWeatherPoint

object LeafWetnessDegreeHourCalculator {

    fun calculate(
        weather: List<DiseaseRiskWeatherPoint>
    ): Double? {

        if (weather.isEmpty()) {
            return null
        }

        if (
            weather.any {
                it.leafWetness?.value == null
            }
        ) {
            return null
        }

        return weather
            .filter {
                it.leafWetness?.value == true
            }
            .sumOf { point ->
                point.temperatureCelsius
                    .coerceAtLeast(0.0)
            }
    }
}